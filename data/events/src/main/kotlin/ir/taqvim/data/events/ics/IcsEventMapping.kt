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
import ir.taqvim.data.database.PersonalEventEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** A part of an iCalendar event that could not be imported as it was (T-1003). */
enum class ImportIssue {
    /** EXDATE values: personal events have no exception dates. */
    EXCEPTION_DATES_IGNORED,

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

/** A personal event read from iCalendar and not yet stored (its [PersonalEventEntity.id] is 0). */
data class ImportedEvent(
    val event: PersonalEventEntity,
    val recurrence: RecurrenceRule?,
    val reminderMinutes: List<Int>,
    val warnings: List<ImportWarning>,
)

/** Maps iCalendar events to personal events; floating and UTC times are placed in [zone]. */
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

    fun toImported(
        event: IcsEvent,
        nowEpochMillis: Long,
    ): ImportedEvent {
        val issues = mutableListOf<ImportIssue>()
        val start = moment(event.start)
        val end = end(event, start, issues)
        val recurrence = recurrence(event, start, issues)
        if (event.exceptionDates.isNotEmpty()) issues += ImportIssue.EXCEPTION_DATES_IGNORED
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
        )
    }

    private fun moment(value: IcsDateTime): Moment =
        when (value) {
            is IcsDateTime.Date -> Moment(value.date.toJdn(), null, zone, null)
            is IcsDateTime.Floating -> local(value.dateTime, zone)
            is IcsDateTime.Utc -> local(value.instant.toLocalDateTime(zone), zone)
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

    /** An RRULE as a Gregorian rule; a date-time UNTIL of a timed event becomes its day in the event's time zone. */
    private fun gregorian(
        recurrence: Recurrence,
        start: Moment,
    ): CalendarRecurrence {
        val rule = recurrence.toRecurrenceRule()
        val until = recurrence.until
        val localUntil =
            if (until != null && until !is IcsDateTime.Date && start.instant != null) {
                instantOf(until, start.timeZone).toLocalDateTime(start.timeZone).date.toJdn()
            } else {
                rule.until
            }
        return CalendarRecurrence(CalendarSystem.GREGORIAN, rule.copy(until = localUntil))
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
