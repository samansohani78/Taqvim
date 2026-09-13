/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarSystem

/** Name of [system] in the editor. */
@StringRes
internal fun calendarLabel(system: CalendarSystem): Int =
    when (system) {
        CalendarSystem.PERSIAN -> R.string.events_calendar_persian
        CalendarSystem.ISLAMIC -> R.string.events_calendar_islamic
        CalendarSystem.GREGORIAN -> R.string.events_calendar_gregorian
        CalendarSystem.NEPALI -> R.string.events_calendar_nepali
    }

/** Repeat choice for [frequency]; `null` is "does not repeat". */
@StringRes
internal fun frequencyLabel(frequency: Frequency?): Int =
    when (frequency) {
        null -> R.string.events_repeat_none
        Frequency.DAILY -> R.string.events_repeat_daily
        Frequency.WEEKLY -> R.string.events_repeat_weekly
        Frequency.MONTHLY -> R.string.events_repeat_monthly
        Frequency.YEARLY -> R.string.events_repeat_yearly
    }

/** Label of the interval field of [frequency]. */
@StringRes
internal fun intervalLabel(frequency: Frequency): Int =
    when (frequency) {
        Frequency.DAILY -> R.string.events_interval_days
        Frequency.WEEKLY -> R.string.events_interval_weeks
        Frequency.MONTHLY -> R.string.events_interval_months
        Frequency.YEARLY -> R.string.events_interval_years
    }

@StringRes
internal fun endLabel(end: RecurrenceEnd): Int =
    when (end) {
        RecurrenceEnd.NEVER -> R.string.events_repeat_end_never
        RecurrenceEnd.COUNT -> R.string.events_repeat_end_count
        RecurrenceEnd.UNTIL -> R.string.events_repeat_end_until
    }

@StringRes
internal fun policyLabel(policy: InvalidDatePolicy): Int =
    when (policy) {
        InvalidDatePolicy.SKIP -> R.string.events_invalid_days_skip
        InvalidDatePolicy.NEXT_DAY -> R.string.events_invalid_days_next
        InvalidDatePolicy.LAST_DAY_OF_MONTH -> R.string.events_invalid_days_last
    }

@StringRes
internal fun errorMessage(error: EditorError): Int =
    when (error) {
        EditorError.TITLE_REQUIRED -> R.string.events_error_title_required
        EditorError.TITLE_TOO_LONG -> R.string.events_error_title_too_long
        EditorError.NOTES_TOO_LONG -> R.string.events_error_notes_too_long
        EditorError.LINK_INVALID -> R.string.events_error_link
        EditorError.END_BEFORE_START -> R.string.events_error_end_before_start
        EditorError.INTERVAL_INVALID -> R.string.events_error_interval
        EditorError.COUNT_INVALID -> R.string.events_error_count
        EditorError.UNTIL_BEFORE_START -> R.string.events_error_until_before_start
        EditorError.TOO_MANY_REMINDERS -> R.string.events_error_too_many_reminders
        EditorError.REMINDER_INVALID -> R.string.events_error_reminder
    }

/** The text of [option]: "At the start" or "15 minutes before". */
@Composable
internal fun reminderText(option: ReminderOption): String =
    if (option.minutes == 0) {
        stringResource(R.string.events_reminder_at_start)
    } else {
        stringResource(R.string.events_reminder_before, option.amount)
    }

/** The first of [candidates] found in [errors], as its message; `null` when none is present. */
@Composable
internal fun firstError(
    errors: Set<EditorError>,
    vararg candidates: EditorError,
): String? = candidates.firstOrNull { it in errors }?.let { stringResource(errorMessage(it)) }

/** A section heading. */
@Composable
internal fun SectionTitle(
    @StringRes text: Int,
) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}
