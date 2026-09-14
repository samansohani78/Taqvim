/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.ui.component.DateSelection
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

/**
 * The Astronomy screen (T-1300): Earth, Moon and Sun views of the sky at a selected instant, which follows the clock
 * until the user moves it by days, years, the time slider or the date picker; plus the header and four dialogs.
 * Astronomy is computed on [computeDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AstronomyViewModel(
    settingsSource: AstronomySettingsSource,
    private val clock: Clock,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** A dialog to open once the settings are known (T-1103 links, the calendar's planetary hours). */
    entry: AstronomyEntry? = null,
) : ViewModel() {
    private val pendingEntry = MutableStateFlow(entry)
    private val selection = MutableStateFlow<Instant?>(null)
    private val mode = MutableStateFlow(AstronomyMode.EARTH)
    private val dialogKind = MutableStateFlow<AstronomyDialogKind?>(null)
    private val pickingDate = MutableStateFlow(false)
    private val latestSettings = MutableStateFlow<AstronomySettings?>(null)
    private val headerCache = AstronomyHeaderCache()
    private val dialogCache = LruCache<Triple<AstronomyDialogKind, Instant, AstronomySettings>, AstronomyDialog>(4)

    private data class Moment(
        val settings: AstronomySettings?,
        val instant: Instant,
        val isNow: Boolean,
    )

    private data class Presentation(
        val mode: AstronomyMode,
        val dialog: AstronomyDialogKind?,
        val pickingDate: Boolean,
    )

    val uiState: StateFlow<AstronomyUiState> =
        combine(
            combine(settingsSource.settings().onEach(::onSettings), selection, minuteTicks(clock)) {
                settings,
                selected,
                now,
                ->
                Moment(settings, selected ?: now, selected == null)
            },
            combine(mode, dialogKind, pickingDate, ::Presentation),
            ::Pair,
        ).mapLatest { (moment, presentation) -> state(moment, presentation) }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AstronomyUiState())

    fun onMode(value: AstronomyMode) {
        mode.value = value
    }

    /** Moves the selection by [days] local days, keeping the local time. */
    fun onStepDays(days: Int) {
        move { settings, instant ->
            val local = instant.toLocalDateTime(settings.timeZone)
            settings.at(instant.toJdn(settings.timeZone) + days.toLong(), local.time)
        }
    }

    /** Moves the selection by [years] years of the settings' calendar, keeping month, day (clamped) and local time. */
    fun onStepYears(years: Int) {
        move { settings, instant ->
            val calendar = settings.calendar
            val date = calendar.fromJdn(instant.toJdn(settings.timeZone))
            val moved = calendar.toJdn(calendar.addMonths(date, years * MONTHS_PER_YEAR))
            settings.at(moved, instant.toLocalDateTime(settings.timeZone).time)
        }
    }

    /** Sets the local time of the selected day to [minute] after midnight. */
    fun onMinuteOfDay(minute: Int) {
        move { settings, instant ->
            val clamped = minute.coerceIn(0, MINUTES_PER_DAY - 1)
            val time = LocalTime(clamped / MINUTES_PER_HOUR, clamped % MINUTES_PER_HOUR)
            settings.at(instant.toJdn(settings.timeZone), time)
        }
    }

    /** Follows the clock again. */
    fun onNow() {
        selection.value = null
    }

    fun onPickDate() {
        pickingDate.value = true
    }

    fun onDismissPicker() {
        pickingDate.value = false
    }

    /** Selects [date] of the settings' calendar at the current local time. */
    fun onDatePicked(date: DateSelection) {
        pickingDate.value = false
        move { settings, instant ->
            val calendar = settings.calendar
            val day = calendar.toJdn(calendar.date(date.year, date.month, date.day))
            settings.at(day, instant.toLocalDateTime(settings.timeZone).time)
        }
    }

    /** Opens [kind]; the selection stops following the clock so the dialog stays stable. */
    fun onOpenDialog(kind: AstronomyDialogKind) {
        selection.value = selection.value ?: clock.now()
        dialogKind.value = kind
    }

    fun onDismissDialog() {
        dialogKind.value = null
    }

    /** Keeps [settings] and, the first time they are known, opens the pending entry's dialog at noon of its day. */
    private fun onSettings(settings: AstronomySettings?) {
        latestSettings.value = settings
        if (settings == null) return
        pendingEntry.getAndUpdate { null }?.let { opened ->
            selection.value = settings.at(opened.day, ENTRY_TIME)
            dialogKind.value = opened.kind
        }
    }

    private fun move(transform: (AstronomySettings, Instant) -> Instant) {
        val settings = latestSettings.value ?: return
        selection.value = transform(settings, selection.value ?: clock.now())
    }

    private fun state(
        moment: Moment,
        presentation: Presentation,
    ): AstronomyUiState {
        val settings = moment.settings
        val dialog =
            presentation.dialog?.takeIf { settings != null }?.let { kind ->
                val key = Triple(kind, moment.instant, requireNotNull(settings))
                dialogCache.getOrPut(key) { AstronomyDialogs.build(kind, settings, moment.instant) }
            }
        return AstronomyUiState(
            mode = presentation.mode,
            content = AstronomyStateMapper.content(settings, moment.instant, moment.isNow, headerCache),
            dialog = dialog,
            pickingDate = presentation.pickingDate && settings != null,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MONTHS_PER_YEAR = 12
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 1_440

        /** Local time an entry's day opens at: noon, inside the day in every zone. */
        val ENTRY_TIME = LocalTime(12, 0)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

/** [clock]'s time now and then at every following minute boundary. */
internal fun minuteTicks(clock: Clock): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
        }
    }
