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
import ir.taqvim.core.ics.OccurrenceSeries
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * A stored personal event with its [recurrence] rule (counted in its calendar), [reminders], and the [exceptionDays]
 * and [overrides] of its occurrences (T-1003).
 */
data class ExportRecord(
    val event: PersonalEventEntity,
    val recurrence: RecurrenceRule?,
    val reminders: List<ReminderEntity>,
    val exceptionDays: List<Long> = emptyList(),
    val overrides: List<EventOverrideEntity> = emptyList(),
) {
    /** Days whose occurrence does not take place: exception days and cancelled overrides, ascending. */
    val excludedDays: List<Long>
        get() = (exceptionDays + overrides.filter { it.cancelled }.map { it.originalJdn }).distinct().sorted()
}

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
            exceptionDates = record.excludedDays.map { dateTime(event, Jdn(it), event.startMinute) },
            alarms = alarms(record, event.title),
            recurrenceDates = explicit?.let { recurrenceDates(record, it, today) }.orEmpty(),
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

    /**
     * A VEVENT with the series' UID and a RECURRENCE-ID (RFC 5545 §3.8.4.4) for each override of [record] that is not
     * cancelled; cancelled overrides are written as EXDATE values of the series instead.
     */
    fun overrideEvents(record: ExportRecord): List<IcsEvent> {
        val event = record.event
        return record.overrides.filterNot { it.cancelled }.map { override ->
            val instance =
                event.copy(
                    startJdn = override.startJdn,
                    startMinute = override.startMinute,
                    endJdn = override.endJdn,
                    endMinute = override.endMinute,
                )
            IcsEvent(
                uid = uidOf(event),
                start = dateTime(event, Jdn(override.startJdn), override.startMinute),
                end = end(instance),
                summary = override.title.takeIf { it.isNotEmpty() },
                description = override.notes.takeIf { it.isNotEmpty() },
                alarms = alarms(record, override.title),
                recurrenceId = dateTime(event, Jdn(override.originalJdn), event.startMinute),
            )
        }
    }

    private fun alarms(
        record: ExportRecord,
        title: String,
    ): List<DisplayAlarm> =
        record.reminders
            .filter { it.enabled }
            .map { DisplayAlarm(AlarmTrigger.Relative(-it.minutesBefore.minutes), title) }

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

    /**
     * Occurrences after the start (DTSTART is the first) that are not excluded, bounded by [ExportLimits] when the rule
     * has no end. Overridden occurrences stay, so that their RECURRENCE-ID names one of the dates.
     */
    private fun recurrenceDates(
        record: ExportRecord,
        rule: RecurrenceRule,
        today: Jdn,
    ): List<IcsDateTime> {
        val event = record.event
        val calendar = calendars.calendarFor(event.calendarSystem) ?: return emptyList()
        val unbounded = rule.count == null && rule.until == null
        val horizon = maxOf(today.value, event.startJdn) + limits.horizonDays
        val series =
            OccurrenceSeries<Nothing>(
                calendar = calendar,
                start = calendar.fromJdn(Jdn(event.startJdn)),
                rule = rule,
                excluded = record.excludedDays.map(::Jdn).toSet(),
            )
        return series
            .starts()
            .drop(1)
            .takeWhile { !unbounded || it.value <= horizon }
            .filter { it !in series.excluded }
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
