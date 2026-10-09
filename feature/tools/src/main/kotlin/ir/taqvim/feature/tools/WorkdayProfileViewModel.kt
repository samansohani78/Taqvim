/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.monthNamesOf
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.component.CalendarPickerData
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.core.workdays.WorkdayProfile
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The workday profiles the user keeps (F-07).
 *
 * `workday_profiles` and its DAO have existed since T-504 with nothing writing to them, so every user ran on the
 * fallback weekend. The draft being edited lives here and is only written on save, so leaving the screen half-way
 * changes nothing.
 */
class WorkdayProfileViewModel(
    private val store: WorkdayProfileStore,
    private val settings: ToolsSettingsSource,
    private val clock: Clock,
) : ViewModel() {
    private val draft = MutableStateFlow<WorkdayProfileDraft?>(null)
    private val picking = MutableStateFlow<LeaveBound?>(null)
    private val pendingFirst = MutableStateFlow<Long?>(null)

    /**
     * The latest settings, for turning a picked year/month/day into a day number with the user's own calendar.
     *
     * Collected here rather than set as a side effect of [uiState], which does not run until something subscribes:
     * a date picked before the first subscriber would otherwise be silently dropped.
     */
    private var current: ToolsSettings? = null

    init {
        viewModelScope.launch { settings.settings().collect { current = it } }
    }

    val uiState: StateFlow<WorkdayProfileUiState> =
        combine(
            store.profiles(),
            settings.settings(),
            draft,
            picking,
            pendingFirst,
        ) { profiles, toolsSettings, editing, pickingBound, first ->
            WorkdayProfileUiState(
                loading = false,
                profiles = profiles.toImmutableList(),
                names = profiles.map { it.name }.toImmutableList(),
                editing = editing,
                picker = pickingBound?.let { picker(toolsSettings, first) },
                pickingLeave = pickingBound,
                pendingLeaveFirst = first,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WorkdayProfileUiState())

    fun onNew() {
        draft.value = WorkdayProfileDraft()
    }

    fun onEdit(stored: NamedWorkdayProfile) {
        draft.value =
            WorkdayProfileDraft(
                id = stored.id,
                name = stored.name,
                weekend = stored.profile.weekend,
                holidaySources = stored.profile.holidaySources,
                halfDays = stored.profile.halfDays,
                leave =
                    stored.profile.personalLeave
                        .map(::leaveRow)
                        .toImmutableList(),
                isDefault = stored.isDefault,
            )
    }

    fun onCancel() {
        draft.value = null
        picking.value = null
        pendingFirst.value = null
    }

    fun onName(name: String) = edit { it.copy(name = name) }

    fun onWeekend(
        day: Weekday,
        on: Boolean,
    ) = edit { it.copy(weekend = if (on) it.weekend + day else it.weekend - day) }

    fun onHolidaySource(
        source: EventSource,
        on: Boolean,
    ) = edit { it.copy(holidaySources = if (on) it.holidaySources + source else it.holidaySources - source) }

    fun onHalfDays(policy: HalfDayPolicy) = edit { it.copy(halfDays = policy) }

    /** Opens the picker for the first day of a new leave range. */
    fun onAddLeave() {
        pendingFirst.value = null
        picking.value = LeaveBound.FIRST
    }

    fun onRemoveLeave(row: LeaveRow) = edit { it.copy(leave = (it.leave - row).toImmutableList()) }

    fun onDismissPicker() {
        picking.value = null
        pendingFirst.value = null
    }

    /** The picker's answer: the first day of a range, then its last; a last before the first swaps the two. */
    fun onLeaveDatePicked(selection: DateSelection) {
        val calendar = current?.calendars?.firstOrNull() ?: return
        val day = calendar.toJdn(calendar.date(selection.year, selection.month, selection.day)).value
        when (picking.value) {
            LeaveBound.FIRST -> {
                pendingFirst.value = day
                picking.value = LeaveBound.LAST
            }

            LeaveBound.LAST -> {
                val first = pendingFirst.value ?: day
                edit { it.copy(leave = (it.leave + leaveRow(minOf(first, day), maxOf(first, day))).toImmutableList()) }
                onDismissPicker()
            }

            null -> {
                return
            }
        }
    }

    /**
     * Whether a save is in flight.
     *
     * [onSave] clears the draft only after the store returns, so until this guard existed a second tap inside that
     * window saw the same draft with no id and saved it again — two taps, two rows. It is released however the save
     * ends: completed, failed or cancelled with the screen.
     */
    private var saving = false

    fun onSave() {
        if (saving) return
        val editing = draft.value?.takeIf { it.canSave } ?: return
        saving = true
        viewModelScope
            .launch {
                store.save(editing.id, editing.name.trim(), editing.toProfile())
                draft.value = null
            }.invokeOnCompletion { saving = false }
    }

    fun onDelete(stored: NamedWorkdayProfile) {
        viewModelScope.launch {
            store.delete(stored.id)
            if (draft.value?.id == stored.id) draft.value = null
        }
    }

    fun onMakeDefault(stored: NamedWorkdayProfile) {
        viewModelScope.launch { store.makeDefault(stored.id) }
    }

    private fun edit(change: (WorkdayProfileDraft) -> WorkdayProfileDraft) {
        draft.value = draft.value?.let(change)
    }

    private fun leaveRow(range: LeaveRange): LeaveRow = leaveRow(range.first.value, range.last.value)

    private fun leaveRow(
        first: Long,
        last: Long,
    ): LeaveRow {
        val toolsSettings = current
        val text =
            if (toolsSettings == null) {
                "$first – $last"
            } else {
                val calendar = toolsSettings.calendars.first()
                val language = toolsSettings.language
                val from =
                    DateFormatter.format(
                        calendar.fromJdn(Jdn(first)),
                        Jdn(first).weekday(),
                        language,
                        DateStyle.NUMERIC,
                    )
                val to =
                    DateFormatter.format(calendar.fromJdn(Jdn(last)), Jdn(last).weekday(), language, DateStyle.NUMERIC)
                if (first == last) from else "$from – $to"
            }
        return LeaveRow(first, last, text)
    }

    private fun picker(
        toolsSettings: ToolsSettings,
        pendingDay: Long?,
    ): CalendarPickerData {
        val calendar = toolsSettings.calendars.first()
        val today = pendingDay?.let(::Jdn) ?: clock.now().toJdn(toolsSettings.homeZone)
        val date = calendar.fromJdn(today)
        val language = toolsSettings.language
        return CalendarPickerData(
            initial = DateSelection(date.year, date.month, date.day),
            years = (date.year - PICKER_YEARS)..(date.year + PICKER_YEARS),
            monthNames = language.monthNamesOf(calendar.system, date.year).orEmpty(),
            daysInMonth = calendar::monthLength,
            digits = { Numerals.format(it.toLong(), language.numerals) },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Years a leave range can be chosen in, either way from today: leave is planned, not historical research. */
        const val PICKER_YEARS = 5
    }
}

/** The draft as the engine's [WorkdayProfile]. */
internal fun WorkdayProfileDraft.toProfile(): WorkdayProfile =
    WorkdayProfile(
        weekend = weekend,
        holidaySources = holidaySources,
        halfDays = halfDays,
        personalLeave = leave.map { LeaveRange(Jdn(it.firstJdn), Jdn(it.lastJdn)) },
    )
