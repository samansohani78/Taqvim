/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.monthNamesOf
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.component.CalendarPickerData
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.ShiftRotation
import ir.taqvim.core.workdays.ShiftType
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The shift rotations a user keeps (F-08).
 *
 * The draft lives here and is written only on save, so leaving the editor half-way changes nothing. A pattern is
 * built from shift types the user names first, which is why the editor is a sequence of taps rather than free text:
 * "Day, Day, Night, Night, Off, Off" is a list of choices, not a sentence to be parsed.
 */
class ShiftRotationViewModel(
    private val store: ShiftRotationStore,
    private val settings: ToolsSettingsSource,
    private val clock: Clock,
) : ViewModel() {
    private val draft = MutableStateFlow<ShiftRotationDraft?>(null)
    private val pickingAnchor = MutableStateFlow(false)
    private var current: ToolsSettings? = null

    init {
        viewModelScope.launch { settings.settings().collect { current = it } }
    }

    val uiState: StateFlow<ShiftRotationUiState> =
        combine(
            store.rotations(),
            settings.settings(),
            draft,
            pickingAnchor,
        ) { rotations, toolsSettings, editing, picking ->
            ShiftRotationUiState(
                loading = false,
                rotations = rotations.toImmutableList(),
                editing = editing,
                picker = if (picking) picker(toolsSettings, editing?.anchorJdn) else null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShiftRotationUiState())

    fun onNew() {
        val today = today()
        draft.value = ShiftRotationDraft(anchorJdn = today, anchorText = dayText(today))
    }

    fun onEdit(rotation: ShiftRotation) {
        val types = rotation.types
        draft.value =
            ShiftRotationDraft(
                id = rotation.id,
                name = rotation.name,
                anchorJdn = rotation.anchor.value,
                anchorText = dayText(rotation.anchor.value),
                types = types.toImmutableList(),
                pattern =
                    rotation.pattern
                        .map {
                            types.indexOfFirst { type ->
                                type.label == it.label
                            }
                        }.toImmutableList(),
                isActive = rotation.isActive,
            )
    }

    fun onCancel() {
        draft.value = null
        pickingAnchor.value = false
    }

    fun onName(name: String) = edit { it.copy(name = name) }

    fun onNewTypeLabel(label: String) = edit { it.copy(newTypeLabel = label) }

    fun onAddType() =
        edit { current ->
            val label = current.newTypeLabel.trim()
            if (label.isEmpty() || current.types.any { it.label == label }) {
                current
            } else {
                val color = SHIFT_COLORS[current.types.size % SHIFT_COLORS.size]
                current.copy(
                    types = (current.types + ShiftType(label, color)).toImmutableList(),
                    newTypeLabel = "",
                )
            }
        }

    /** Removes a type and every place the pattern used it; the pattern cannot name a type that is gone. */
    fun onRemoveType(index: Int) =
        edit { current ->
            if (index !in current.types.indices) {
                current
            } else {
                current.copy(
                    types = (current.types - current.types[index]).toImmutableList(),
                    pattern =
                        current.pattern
                            .filter { it != index }
                            .map { if (it > index) it - 1 else it }
                            .toImmutableList(),
                )
            }
        }

    fun onTypeColor(
        index: Int,
        color: Int,
    ) = edit { current ->
        val type = current.types.getOrNull(index) ?: return@edit current
        current.copy(
            types =
                current.types
                    .toMutableList()
                    .also { it[index] = type.copy(color = color) }
                    .toImmutableList(),
        )
    }

    fun onAppendToPattern(typeIndex: Int) =
        edit { current ->
            if (typeIndex in current.types.indices) {
                current.copy(pattern = (current.pattern + typeIndex).toImmutableList())
            } else {
                current
            }
        }

    fun onRemoveLastFromPattern() = edit { it.copy(pattern = it.pattern.dropLast(1).toImmutableList()) }

    fun onPickAnchor() {
        pickingAnchor.value = draft.value != null
    }

    fun onAnchorPicked(selection: DateSelection) {
        val calendar = current?.calendars?.firstOrNull() ?: return
        val day = calendar.toJdn(calendar.date(selection.year, selection.month, selection.day)).value
        edit { it.copy(anchorJdn = day, anchorText = dayText(day)) }
        pickingAnchor.value = false
    }

    fun onDismissPicker() {
        pickingAnchor.value = false
    }

    fun onActive(active: Boolean) = edit { it.copy(isActive = active) }

    fun onSave() {
        val editing = draft.value?.takeIf { it.canSave } ?: return
        viewModelScope.launch {
            store.save(
                ShiftRotation(
                    id = editing.id ?: 0L,
                    name = editing.name.trim(),
                    anchor = Jdn(editing.anchorJdn),
                    pattern = editing.patternTypes,
                    isActive = editing.isActive,
                ),
            )
            draft.value = null
        }
    }

    fun onDelete(rotation: ShiftRotation) {
        viewModelScope.launch {
            store.delete(rotation.id)
            if (draft.value?.id == rotation.id) draft.value = null
        }
    }

    private fun edit(change: (ShiftRotationDraft) -> ShiftRotationDraft) {
        draft.value = draft.value?.let(change)
    }

    private fun today(): Long = clock.now().toJdn(current?.homeZone ?: kotlinx.datetime.TimeZone.UTC).value

    private fun dayText(day: Long): String {
        val toolsSettings = current ?: return day.toString()
        val calendar = toolsSettings.calendars.first()
        return DateFormatter.format(
            calendar.fromJdn(Jdn(day)),
            Jdn(day).weekday(),
            toolsSettings.language,
            DateStyle.NUMERIC,
        )
    }

    private fun picker(
        toolsSettings: ToolsSettings,
        anchorJdn: Long?,
    ): CalendarPickerData {
        val calendar = toolsSettings.calendars.first()
        val date = calendar.fromJdn(Jdn(anchorJdn ?: today()))
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
        const val PICKER_YEARS = 5
    }
}
