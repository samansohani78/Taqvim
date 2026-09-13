/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.AlarmTrigger
import ir.taqvim.core.ics.DisplayAlarm
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** A stored personal event with its [recurrence] rule (counted in its calendar) and [reminders]. */
data class ExportRecord(
    val event: PersonalEventEntity,
    val recurrence: RecurrenceRule?,
    val reminders: List<ReminderEntity>,
)

/**
 * Bounds of the explicit occurrences written for rules RFC 5545 cannot express (ADR-0013): open-ended rules are
 * expanded for [horizonDays] from today (or the event start, if later) and never beyond [maxRecurrenceDates] dates.
 */
data class ExportLimits(
    val horizonDays: Long = DEFAULT_HORIZON_DAYS,
    val maxRecurrenceDates: Int = DEFAULT_MAX_RECURRENCE_DATES,
) {
    private companion object {
        const val DEFAULT_HORIZON_DAYS = 3_653L
        const val DEFAULT_MAX_RECURRENCE_DATES = 1_000
    }
}

/** The iCalendar UID of [event]: its stored UID, or one derived from its row id and creation time. */
internal fun uidOf(event: PersonalEventEntity): String =
    event.icsUid ?: "taqvim-${event.id}-${event.createdAtEpochMillis}"

/** Maps personal events to iCalendar events (T-1003, ADR-0013). */
internal class IcsExportMapping(
    private val calendars: CalendarProvider,
    private val limits: ExportLimits = ExportLimits(),
) {
    fun toIcs(
        record: ExportRecord,
        today: Jdn,
    ): IcsEvent {
        val event = record.event
        val rule = record.recurrence
        val rfcRule = rule?.takeIf { isRfcCompatible(event.calendarSystem, it) }
        val explicit = rule?.takeIf { rfcRule == null }
        return IcsEvent(
            uid = uidOf(event),
            start = dateTime(event, Jdn(event.startJdn), event.startMinute),
            end = end(event),
            summary = event.title.takeIf { it.isNotEmpty() },
            description = event.notes.takeIf { it.isNotEmpty() },
            recurrence = rfcRule?.let { toRecurrence(it, event) },
            alarms =
                record.reminders
                    .filter { it.enabled }
                    .map { DisplayAlarm(AlarmTrigger.Relative(-it.minutesBefore.minutes), event.title) },
            recurrenceDates = explicit?.let { recurrenceDates(event, it, today) }.orEmpty(),
            extensions =
                explicit
                    ?.let {
                        mapOf(
                            TaqvimRecurrence.PROPERTY to
                                TaqvimRecurrence.format(CalendarRecurrence(event.calendarSystem, it)),
                        )
                    }.orEmpty(),
        )
    }

    /** Whether RFC 5545 expands [rule] as Taqvim does: Gregorian months, skipped invalid days, weeks from Monday. */
    private fun isRfcCompatible(
        calendar: CalendarSystem,
        rule: RecurrenceRule,
    ): Boolean =
        calendar == CalendarSystem.GREGORIAN &&
            rule.invalidDates == InvalidDatePolicy.SKIP &&
            rule.weekStart == Weekday.MONDAY

    private fun end(event: PersonalEventEntity): IcsDateTime {
        val startMinute = event.startMinute
        return if (startMinute == null) {
            IcsDateTime.Date((Jdn(event.endJdn) + 1).toLocalDate())
        } else {
            dateTime(event, Jdn(event.endJdn), event.endMinute ?: startMinute)
        }
    }

    private fun dateTime(
        event: PersonalEventEntity,
        day: Jdn,
        minute: Int?,
    ): IcsDateTime {
        val date = day.toLocalDate()
        if (minute == null) return IcsDateTime.Date(date)
        val local = LocalDateTime(date, LocalTime(minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR))
        return if (zoneOf(event) != null) IcsDateTime.Zoned(local, event.timeZoneId) else IcsDateTime.Floating(local)
    }

    private fun toRecurrence(
        rule: RecurrenceRule,
        event: PersonalEventEntity,
    ): Recurrence =
        Recurrence(
            frequency = rule.frequency,
            interval = rule.interval,
            count = rule.count,
            until = rule.until?.let { untilValue(event, it) },
            byDay = rule.byDay,
            byMonthDay = rule.byMonthDay,
        )

    /** UNTIL of DTSTART's value type (RFC 5545 §3.3.10): a DATE, or the last start as UTC or floating time. */
    private fun untilValue(
        event: PersonalEventEntity,
        until: Jdn,
    ): IcsDateTime {
        val start = dateTime(event, until, event.startMinute)
        val zone = zoneOf(event)
        return if (start is IcsDateTime.Zoned &&
            zone != null
        ) {
            IcsDateTime.Utc(start.dateTime.toInstant(zone))
        } else {
            start
        }
    }

    /** Occurrences after the start (DTSTART is the first), bounded by [ExportLimits] when the rule has no end. */
    private fun recurrenceDates(
        event: PersonalEventEntity,
        rule: RecurrenceRule,
        today: Jdn,
    ): List<IcsDateTime> {
        val calendar = calendars.calendarFor(event.calendarSystem) ?: return emptyList()
        val unbounded = rule.count == null && rule.until == null
        val horizon = maxOf(today.value, event.startJdn) + limits.horizonDays
        return RecurrenceEngine(calendar)
            .occurrences(calendar.fromJdn(Jdn(event.startJdn)), rule)
            .drop(1)
            .takeWhile { !unbounded || it.value <= horizon }
            .take(limits.maxRecurrenceDates)
            .map { dateTime(event, it, event.startMinute) }
            .toList()
    }

    private fun zoneOf(event: PersonalEventEntity): TimeZone? =
        runCatching { TimeZone.of(event.timeZoneId) }.getOrNull()

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
