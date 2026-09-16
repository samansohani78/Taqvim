/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.runtime.Immutable
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf

/** State of the event editor (T-1000). */
data class EventEditorUiState(
    val content: EditorContent = EditorContent.Loading,
)

/** What the editor shows. */
sealed interface EditorContent {
    /** Settings or the event are loading. */
    data object Loading : EditorContent

    /** The event does not exist, or its calendar cannot be edited yet. */
    data object NotFound : EditorContent

    /** A repeating event opened from a day: change only that occurrence or the whole series (ADR-0034)? */
    data object ChoosingScope : EditorContent

    /** The form being edited. */
    data class Editing(
        val form: EditorForm,
        val display: EditorDisplay,
        /** Problems to show; empty until the first attempt to save. */
        val errors: ImmutableSet<EditorError>,
        val isNew: Boolean,
        /** Whether the form differs from the stored (or new) event. */
        val hasChanges: Boolean,
        /** The typed date phrase, and whether the last one contained no date. */
        val dateText: String,
        val dateTextUnrecognized: Boolean,
        /** A save or delete is in progress. */
        val busy: Boolean,
        /** The last save or delete failed. */
        val storeFailed: Boolean,
        /** Only one occurrence of a repeating event is edited: no repetition or reminders, delete cancels it. */
        val occurrenceOnly: Boolean = false,
    ) : EditorContent

    /** The editor is done and should close. */
    data class Finished(
        val outcome: EditorOutcome,
    ) : EditorContent
}

/** What a request to leave the editor (its Cancel button or system Back) does (B10). */
enum class CloseDecision {
    /** Nothing would be lost: close at once. */
    CLOSE,

    /** Unsaved changes: ask before discarding them. */
    CONFIRM,

    /** A save or delete is running: stay until it finishes. */
    WAIT,
}

/** The single rule for leaving the editor, shared by the Cancel button and system Back. */
val EditorContent.Editing.closeDecision: CloseDecision
    get() =
        when {
            busy -> CloseDecision.WAIT
            hasChanges -> CloseDecision.CONFIRM
            else -> CloseDecision.CLOSE
        }

/** User actions of the event editor. */
@Immutable
data class EventEditorActions(
    val onIntent: (EditorIntent) -> Unit = {},
    val onDateTextChange: (String) -> Unit = {},
    val onApplyDateText: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    /** Answers [EditorContent.ChoosingScope]: `true` edits only the occurrence, `false` the whole series. */
    val onChooseScope: (thisOccurrence: Boolean) -> Unit = {},
)

/** Which date or time picker is open. */
internal enum class PickerTarget {
    START_DATE,
    END_DATE,
    UNTIL_DATE,
    START_TIME,
    END_TIME,
}

/** How the editor was closed. */
enum class EditorOutcome {
    SAVED,
    DELETED,
    DISCARDED,
}

/** Localized texts of the form. */
data class EditorDisplay(
    /** Calendars to choose from; always includes the form's calendar. */
    val calendars: ImmutableList<CalendarSystem>,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    /** Last day of the repetition, or `null` while the event does not repeat. */
    val untilDate: String?,
    /** Weekdays from the language's first day of the week. */
    val weekdays: ImmutableList<WeekdayOption>,
    val reminders: ImmutableList<ReminderOption>,
    /** Preset reminders that have not been added yet. */
    val reminderChoices: ImmutableList<ReminderOption>,
    /** Event colors (ARGB). */
    val colors: ImmutableList<Int>,
    val picker: CalendarPickerData,
    /** The next occurrences of a repeating event (review F02), empty otherwise. */
    val preview: ImmutableList<PreviewOccurrence> = persistentListOf(),
)

/** An upcoming occurrence: its localized [date] and, for a timed event, [time], in the viewer's zone. */
data class PreviewOccurrence(
    val date: String,
    val time: String?,
)

/** A weekday of a weekly repetition. */
data class WeekdayOption(
    val weekday: Weekday,
    val label: String,
    val selected: Boolean,
)

/** A reminder offset; [amount] is the localized duration ("15 minutes"), empty at the start. */
data class ReminderOption(
    val minutes: Int,
    val amount: String,
)

/** What the date and time pickers need for the form's calendar. */
@Immutable
data class CalendarPickerData(
    val calendar: CalendarArithmetic,
    val monthNames: ImmutableList<String>,
    val years: IntRange,
    val numerals: NumeralSystem,
) {
    fun daysInMonth(
        year: Int,
        month: Int,
    ): Int = calendar.monthLength(year, month)

    /** [value] in the language's digits. */
    fun formatNumber(value: Int): String = Numerals.format(value.toLong(), numerals)

    /** [value] as two digits in the language's digits (clock fields). */
    fun formatTwoDigits(value: Int): String = Numerals.localizeDigits(value.toString().padStart(2, '0'), numerals)
}
