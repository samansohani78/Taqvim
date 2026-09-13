/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/** How a repetition ends. */
enum class RecurrenceEnd {
    NEVER,
    COUNT,
    UNTIL,
}

/**
 * The editable repetition of an event. Numbers stay as typed (in any digits) until saving. BYDAY entries with an
 * ordinal, BYMONTHDAY and the week start of a loaded rule cannot be edited here and are kept in [keptRule].
 */
data class RepeatForm(
    val frequency: Frequency,
    val intervalText: String,
    val end: RecurrenceEnd = RecurrenceEnd.NEVER,
    val countText: String,
    /** Last day of the repetition when [end] is [RecurrenceEnd.UNTIL]; in the form's calendar. */
    val until: CalendarDate,
    /** Days of a weekly repetition; empty means the start's weekday. */
    val weekdays: Set<Weekday> = emptySet(),
    val invalidDates: InvalidDatePolicy = InvalidDatePolicy.SKIP,
    val keptRule: RecurrenceRule? = null,
) {
    /** The typed interval, or `null` when it is not an integer. */
    val interval: Long?
        get() = Numerals.parseLong(intervalText.trim())

    /** The typed number of occurrences, or `null` when it is not an integer. */
    val count: Long?
        get() = Numerals.parseLong(countText.trim())

    /** This repetition as a rule; only for a form without validation errors. */
    fun toRule(calendar: CalendarArithmetic): RecurrenceRule {
        val base = keptRule ?: RecurrenceRule(frequency)
        return base.copy(
            frequency = frequency,
            interval = requireNotNull(interval) { "invalid interval '$intervalText'" }.toInt(),
            count = count?.toInt()?.takeIf { end == RecurrenceEnd.COUNT },
            until = calendar.toJdn(until).takeIf { end == RecurrenceEnd.UNTIL },
            byDay = base.byDay.filter { it.ordinal != null } + weekdays.sorted().map { WeekdayNum(it) },
            invalidDates = invalidDates,
        )
    }

    companion object {
        private const val DEFAULT_INTERVAL = 1L
        private const val DEFAULT_COUNT = 10L

        /** A new repetition every [frequency] period, ending after ten occurrences if the user picks a count. */
        fun create(
            frequency: Frequency,
            start: CalendarDate,
            numerals: NumeralSystem,
        ): RepeatForm =
            RepeatForm(
                frequency = frequency,
                intervalText = Numerals.format(DEFAULT_INTERVAL, numerals),
                countText = Numerals.format(DEFAULT_COUNT, numerals),
                until = start,
            )

        /** The form of a stored [rule] of an event starting on [start]. */
        fun of(
            rule: RecurrenceRule,
            start: CalendarDate,
            calendar: CalendarArithmetic,
            numerals: NumeralSystem,
        ): RepeatForm =
            RepeatForm(
                frequency = rule.frequency,
                intervalText = Numerals.format(rule.interval.toLong(), numerals),
                end =
                    when {
                        rule.count != null -> RecurrenceEnd.COUNT
                        rule.until != null -> RecurrenceEnd.UNTIL
                        else -> RecurrenceEnd.NEVER
                    },
                countText = Numerals.format(rule.count?.toLong() ?: DEFAULT_COUNT, numerals),
                until = rule.until?.let(calendar::fromJdn) ?: start,
                weekdays =
                    rule.byDay
                        .filter { it.ordinal == null }
                        .map { it.weekday }
                        .toSet(),
                invalidDates = rule.invalidDates,
                keptRule = rule,
            )
    }
}

/** Everything the editor lets the user change (T-1000). Times are kept while the event is all-day. */
data class EditorForm(
    val id: Long? = null,
    val title: String = "",
    val notes: String = "",
    val calendar: CalendarSystem,
    val start: CalendarDate,
    val end: CalendarDate,
    val allDay: Boolean = true,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE,
    val timeZoneId: String,
    val colorArgb: Int? = null,
    val repeat: RepeatForm? = null,
    /** Minutes before the start, ascending and distinct. */
    val reminderMinutes: List<Int> = emptyList(),
    val sourceLink: String = "",
) {
    /** The event this form describes; only for a form without validation errors. */
    fun toEvent(calendar: CalendarArithmetic): PersonalEvent =
        PersonalEvent(
            id = id,
            title = title.trim(),
            notes = notes.trim(),
            calendar = this.calendar,
            start = start,
            end = end,
            startMinute = startMinute.takeUnless { allDay },
            endMinute = endMinute.takeUnless { allDay },
            timeZoneId = timeZoneId,
            colorArgb = colorArgb,
            recurrence = repeat?.toRule(calendar),
            reminderMinutes = reminderMinutes.distinct().sorted(),
            sourceLink = sourceLink.trim().ifEmpty { null },
        )

    companion object {
        /** 09:00. */
        const val DEFAULT_START_MINUTE = 540

        /** 10:00. */
        const val DEFAULT_END_MINUTE = 600

        /** Last minute of a day, 23:59. */
        const val LAST_MINUTE = 1_439

        /** A new all-day event on [today] in the first calendar of [settings]. */
        fun new(
            settings: EditorSettings,
            today: Jdn,
        ): EditorForm {
            val system = settings.calendars.first()
            val date = settings.arithmeticOf(system).fromJdn(today)
            return EditorForm(calendar = system, start = date, end = date, timeZoneId = settings.timeZoneId)
        }

        /** The form of a stored [event]; [calendar] is the arithmetic of its calendar. */
        fun of(
            event: PersonalEvent,
            calendar: CalendarArithmetic,
            numerals: NumeralSystem,
        ): EditorForm =
            EditorForm(
                id = event.id,
                title = event.title,
                notes = event.notes,
                calendar = event.calendar,
                start = event.start,
                end = event.end,
                allDay = event.startMinute == null,
                startMinute = event.startMinute ?: DEFAULT_START_MINUTE,
                endMinute = event.endMinute ?: DEFAULT_END_MINUTE,
                timeZoneId = event.timeZoneId,
                colorArgb = event.colorArgb,
                repeat = event.recurrence?.let { RepeatForm.of(it, event.start, calendar, numerals) },
                reminderMinutes = event.reminderMinutes.distinct().sorted(),
                sourceLink = event.sourceLink.orEmpty(),
            )
    }
}
