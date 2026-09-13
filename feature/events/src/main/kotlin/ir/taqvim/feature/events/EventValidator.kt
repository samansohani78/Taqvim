/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic

/** A reason the editor cannot save the event. */
enum class EditorError {
    TITLE_REQUIRED,
    TITLE_TOO_LONG,
    NOTES_TOO_LONG,
    LINK_INVALID,
    END_BEFORE_START,
    INTERVAL_INVALID,
    COUNT_INVALID,
    UNTIL_BEFORE_START,
    TOO_MANY_REMINDERS,
    REMINDER_INVALID,
}

/** Checks an [EditorForm] before it is saved (T-1000). The limits are product choices and are defined only here. */
object EventValidator {
    const val MAX_TITLE_LENGTH = 200
    const val MAX_NOTES_LENGTH = 4_000
    const val MAX_INTERVAL = 999L
    const val MAX_COUNT = 9_999L
    const val MAX_REMINDERS = 5

    /** Four weeks. */
    const val MAX_REMINDER_MINUTES = 40_320

    /** An absolute http(s) address with a dotted host and no spaces. */
    private val LINK = Regex("""^https?://[^\s/?#]+\.[^\s/?#]+([/?#]\S*)?$""", RegexOption.IGNORE_CASE)

    /** Every problem of [form]; [calendar] is the arithmetic of the form's calendar. */
    fun validate(
        form: EditorForm,
        calendar: CalendarArithmetic,
    ): Set<EditorError> =
        buildSet {
            addAll(textErrors(form))
            if (endsBeforeStart(form, calendar)) add(EditorError.END_BEFORE_START)
            form.repeat?.let { addAll(repeatErrors(it, form, calendar)) }
            addAll(reminderErrors(form.reminderMinutes))
        }

    private fun textErrors(form: EditorForm): List<EditorError> =
        listOfNotNull(
            EditorError.TITLE_REQUIRED.takeIf { form.title.isBlank() },
            EditorError.TITLE_TOO_LONG.takeIf { form.title.trim().length > MAX_TITLE_LENGTH },
            EditorError.NOTES_TOO_LONG.takeIf { form.notes.trim().length > MAX_NOTES_LENGTH },
            EditorError.LINK_INVALID.takeIf { form.sourceLink.isNotBlank() && !LINK.matches(form.sourceLink.trim()) },
        )

    private fun endsBeforeStart(
        form: EditorForm,
        calendar: CalendarArithmetic,
    ): Boolean {
        val days = calendar.toJdn(form.end) - calendar.toJdn(form.start)
        return days < 0 || (days == 0L && !form.allDay && form.endMinute < form.startMinute)
    }

    private fun repeatErrors(
        repeat: RepeatForm,
        form: EditorForm,
        calendar: CalendarArithmetic,
    ): List<EditorError> =
        listOfNotNull(
            EditorError.INTERVAL_INVALID.takeIf { repeat.interval?.let { it in 1..MAX_INTERVAL } != true },
            EditorError.COUNT_INVALID.takeIf {
                repeat.end == RecurrenceEnd.COUNT && repeat.count?.let { it in 1..MAX_COUNT } != true
            },
            EditorError.UNTIL_BEFORE_START.takeIf {
                repeat.end == RecurrenceEnd.UNTIL && calendar.toJdn(repeat.until) < calendar.toJdn(form.start)
            },
        )

    private fun reminderErrors(minutes: List<Int>): List<EditorError> =
        listOfNotNull(
            EditorError.TOO_MANY_REMINDERS.takeIf { minutes.distinct().size > MAX_REMINDERS },
            EditorError.REMINDER_INVALID.takeIf { minutes.any { it !in 0..MAX_REMINDER_MINUTES } },
        )
}
