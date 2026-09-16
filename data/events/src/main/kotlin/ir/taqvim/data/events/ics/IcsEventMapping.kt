/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.ics.AlarmTrigger
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.toRecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** A part of an iCalendar event that could not be imported as it was (T-1003). */
enum class ImportIssue {
    /** RDATE values without a Taqvim rule: only the rule (if any) and the first occurrence are imported. */
    RECURRENCE_DATES_IGNORED,

    /** An unreadable `X-TAQVIM-RECURRENCE` value; the RRULE, if any, was used instead. */
    INVALID_TAQVIM_RECURRENCE,

    /** An alarm other than a display alarm at or before the event start. */
    UNSUPPORTED_ALARM,

    /** DTEND before DTSTART or of another value type: the event ends where it starts. */
    INVALID_END,
}

/** [issue] found in the event with iCalendar [uid]. */
data class ImportWarning(
    val uid: String,
    val issue: ImportIssue,
)

/**
 * A personal event read from iCalendar and not yet stored (its [PersonalEventEntity.id] is 0, as is the event id of
 * its [overrides]), with the days its EXDATE values remove and the RECURRENCE-ID components that change occurrences.
 */
data class ImportedEvent(
    val event: PersonalEventEntity,
    val recurrence: RecurrenceRule?,
    val reminderMinutes: List<Int>,
    val warnings: List<ImportWarning>,
    val exceptionDays: List<Long> = emptyList(),
    val overrides: List<EventOverrideEntity> = emptyList(),
)

/**
 * Maps iCalendar events to personal events. Floating times are placed in [zone]; a UTC value keeps UTC as its time
 * basis, so a UTC series recurs at the same instant of day as the source (RFC 5545 §3.3.5 form #2) instead of
 * following local wall-clock time across a daylight-saving change.
 */
internal class IcsEventMapping(
    private val zone: TimeZone,
) {
    /** A moment of an event: its [day], [minute] of the day (`null` all day) in [timeZone] and [instant] if timed. */
    private data class Moment(
        val day: Jdn,
        val minute: Int?,
        val timeZone: TimeZone,
        val instant: Instant?,
    )

    /** [event] with the [overrides] (components of its UID with a RECURRENCE-ID) of its occurrences. */
    fun toImported(
        event: IcsEvent,
        nowEpochMillis: Long,
        overrides: List<IcsEvent> = emptyList(),
    ): ImportedEvent {
        val issues = mutableListOf<ImportIssue>()
        val start = moment(event.start)
        val end = end(event, start, issues)
        val recurrence = recurrence(event, start, issues)
        val reminders = reminders(event, issues)
        return ImportedEvent(
            event =
                PersonalEventEntity(
                    title = event.summary.orEmpty(),
                    notes = event.description.orEmpty(),
                    calendarSystem = recurrence?.calendar ?: CalendarSystem.GREGORIAN,
                    startJdn = start.day.value,
                    startMinute = start.minute,
                    endJdn = end.day.value,
                    endMinute = end.minute,
                    timeZoneId = start.timeZone.id,
                    createdAtEpochMillis = nowEpochMillis,
                    updatedAtEpochMillis = nowEpochMillis,
                    icsUid = event.uid,
                ),
            recurrence = recurrence?.rule,
            reminderMinutes = reminders,
            warnings = issues.distinct().map { ImportWarning(event.uid, it) },
            exceptionDays =
                event.exceptionDates
                    .map { dayIn(it, start).value }
                    .distinct()
                    .sorted(),
            overrides = overrides.mapNotNull { override(it, event, start) }.distinctBy { it.originalJdn },
        )
    }

    /**
     * The override of the occurrence [component]'s RECURRENCE-ID names, with its times in the series' time zone; its
     * SUMMARY and DESCRIPTION default to the [series]' ones.
     */
    private fun override(
        component: IcsEvent,
        series: IcsEvent,
        seriesStart: Moment,
    ): EventOverrideEntity? {
        val recurrenceId = component.recurrenceId ?: return null
        val ownStart = moment(component.start)
        val start = ownStart.inZone(seriesStart.timeZone)
        val end = end(component, ownStart, mutableListOf()).inZone(seriesStart.timeZone)
        return EventOverrideEntity(
            eventId = 0,
            originalJdn = dayIn(recurrenceId, seriesStart).value,
            title = component.summary ?: series.summary.orEmpty(),
            notes = component.description ?: series.description.orEmpty(),
            startJdn = start.day.value,
            startMinute = start.minute,
            endJdn = end.day.value,
            endMinute = end.minute,
            cancelled = component.cancelled,
        )
    }

    /** The day of an EXDATE or RECURRENCE-ID [value] in the time zone of the series starting at [seriesStart]. */
    private fun dayIn(
        value: IcsDateTime,
        seriesStart: Moment,
    ): Jdn =
        if (value is IcsDateTime.Date) {
            value.date.toJdn()
        } else {
            instantOf(value, seriesStart.timeZone).toLocalDateTime(seriesStart.timeZone).date.toJdn()
        }

    private fun Moment.inZone(target: TimeZone): Moment =
        instant?.let { local(it.toLocalDateTime(target), target) } ?: this

    private fun moment(value: IcsDateTime): Moment =
        when (value) {
            is IcsDateTime.Date -> Moment(value.date.toJdn(), null, zone, null)
            is IcsDateTime.Floating -> local(value.dateTime, zone)
            is IcsDateTime.Utc -> local(value.instant.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
            is IcsDateTime.Zoned -> local(value.dateTime, TimeZone.of(value.timeZoneId))
        }

    private fun local(
        dateTime: LocalDateTime,
        timeZone: TimeZone,
    ): Moment = Moment(dateTime.date.toJdn(), minuteOf(dateTime), timeZone, dateTime.toInstant(timeZone))

    private fun end(
        event: IcsEvent,
        start: Moment,
        issues: MutableList<ImportIssue>,
    ): Moment {
        val end = event.end ?: return start
        val startInstant = start.instant
        val endMoment =
            when {
                startInstant == null && end is IcsDateTime.Date -> {
                    val endJdn = end.date.toJdn()
                    if (endJdn >= start.day) start.copy(day = maxOf(start.day, Jdn(endJdn.value - 1))) else null
                }

                startInstant != null && end !is IcsDateTime.Date -> {
                    val endInstant = instantOf(end, start.timeZone)
                    val local = endInstant.toLocalDateTime(start.timeZone)
                    Moment(local.date.toJdn(), minuteOf(local), start.timeZone, endInstant)
                        .takeIf { endInstant >= startInstant }
                }

                else -> {
                    null
                }
            }
        return endMoment ?: start.also { issues += ImportIssue.INVALID_END }
    }

    private fun recurrence(
        event: IcsEvent,
        start: Moment,
        issues: MutableList<ImportIssue>,
    ): CalendarRecurrence? {
        val taqvim =
            event.extensions[TaqvimRecurrence.PROPERTY]?.let { text ->
                TaqvimRecurrence.parse(text).also { if (it == null) issues += ImportIssue.INVALID_TAQVIM_RECURRENCE }
            }
        if (taqvim == null && event.recurrenceDates.isNotEmpty()) issues += ImportIssue.RECURRENCE_DATES_IGNORED
        return taqvim ?: event.recurrence?.let { gregorian(it, start) }
    }

    /** An RRULE as a Gregorian rule, with a date-time UNTIL reduced to the last day an occurrence may start on. */
    private fun gregorian(
        recurrence: Recurrence,
        start: Moment,
    ): CalendarRecurrence {
        val rule = recurrence.toRecurrenceRule()
        val until = lastStartDay(recurrence.until, start) ?: rule.until
        return CalendarRecurrence(CalendarSystem.GREGORIAN, rule.copy(until = until))
    }

    /**
     * The last day an occurrence of a timed series may start on for the instant cutoff [until]: its day in the
     * series' time zone, or the day before when the series starts later in the day than the cutoff, since UNTIL is
     * inclusive of an occurrence starting exactly at it (RFC 5545 §3.3.10). `null` for an all-day or DATE cutoff,
     * which bounds by day already.
     */
    private fun lastStartDay(
        until: IcsDateTime?,
        start: Moment,
    ): Jdn? {
        if (until == null || until is IcsDateTime.Date) return null
        val startMinute = start.minute ?: return null
        val cutoff = instantOf(until, start.timeZone).toLocalDateTime(start.timeZone)
        val day = cutoff.date.toJdn()
        return if (minuteOf(cutoff) >= startMinute) day else Jdn(day.value - 1)
    }

    private fun reminders(
        event: IcsEvent,
        issues: MutableList<ImportIssue>,
    ): List<Int> =
        event.alarms
            .mapNotNull { alarm ->
                val trigger = alarm.trigger as? AlarmTrigger.Relative
                val offset = trigger?.takeIf { !it.relatedToEnd && !it.offset.isPositive() }?.offset
                offset
                    ?.let { (-it.inWholeMinutes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
                    .also { if (it == null) issues += ImportIssue.UNSUPPORTED_ALARM }
            }.distinct()

    private fun instantOf(
        value: IcsDateTime,
        timeZone: TimeZone,
    ): Instant =
        when (value) {
            is IcsDateTime.Date -> value.date.atStartOfDayIn(timeZone)
            is IcsDateTime.Floating -> value.dateTime.toInstant(timeZone)
            is IcsDateTime.Utc -> value.instant
            is IcsDateTime.Zoned -> value.dateTime.toInstant(TimeZone.of(value.timeZoneId))
        }

    private fun minuteOf(dateTime: LocalDateTime): Int = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
