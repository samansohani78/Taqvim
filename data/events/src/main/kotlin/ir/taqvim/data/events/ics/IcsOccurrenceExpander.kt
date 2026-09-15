/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.toRecurrenceRule
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.devicecalendar.InstantWindow
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Expands subscribed iCalendar events into `ics_events_cache` rows overlapping a window (T-1003). DTSTART, the RRULE
 * occurrences (UNTIL compared as an instant) and RDATE values form the set, EXDATE values are removed (RFC 5545
 * §3.8.5). All-day rows are bounded by UTC midnights, like device instances; floating times are placed in [zone].
 */
internal class IcsOccurrenceExpander(
    private val zone: TimeZone,
    private val maxOccurrences: Int = MAX_OCCURRENCES,
) {
    private data class Start(
        val day: LocalDate,
        val epochMillis: Long,
    )

    /**
     * Rows of all [events] of a feed: every component without RECURRENCE-ID with the components of its UID that
     * override its instances (RFC 5545 §3.8.4.4); overrides without such a component stand alone.
     */
    fun expandAll(
        subscriptionId: Long,
        events: List<IcsEvent>,
        window: InstantWindow,
    ): List<IcsEventCacheEntity> =
        events.groupBy { it.uid }.values.flatMap { components ->
            val (overrides, series) = components.partition { it.recurrenceId != null }
            if (series.isEmpty()) {
                overrides.filterNot { it.cancelled }.flatMap { expand(subscriptionId, it.single(), window) }
            } else {
                series.flatMap { expand(subscriptionId, it, window, overrides) }
            }
        }

    /**
     * Rows of [event] overlapping [window]; the instances named by the RECURRENCE-ID of [overrides] are replaced by the
     * overrides, or removed when an override is cancelled.
     */
    fun expand(
        subscriptionId: Long,
        event: IcsEvent,
        window: InstantWindow,
        overrides: List<IcsEvent> = emptyList(),
    ): List<IcsEventCacheEntity> {
        val duration = durationMillis(event)
        val removed = event.exceptionDates + overrides.mapNotNull { it.recurrenceId }
        val excludedMillis = removed.map(::epochMillis).toSet()
        val excludedDays = removed.filterIsInstance<IcsDateTime.Date>().map { it.date }.toSet()
        val lastDay = dayOfUtc(window.toEpochMillis).plus(MARGIN_DAYS, DateTimeUnit.DAY)
        val replacements = overrides.filterNot { it.cancelled }.flatMap { expand(subscriptionId, it.single(), window) }
        return (ruleStarts(event, lastDay) + event.recurrenceDates.map { Start(dayOf(it), epochMillis(it)) })
            .asSequence()
            .filterNot { it.epochMillis in excludedMillis || it.day in excludedDays }
            .distinctBy { it.epochMillis }
            .filter { overlaps(it.epochMillis, duration, window) }
            .map { row(subscriptionId, event, it.epochMillis, duration) }
            .plus(replacements)
            .sortedBy { it.startEpochMillis }
            .take(maxOccurrences)
            .toList()
    }

    /** This component as a one-off event: an override is expanded on its own. */
    private fun IcsEvent.single(): IcsEvent =
        copy(recurrence = null, exceptionDates = emptyList(), recurrenceDates = emptyList(), recurrenceId = null)

    private fun row(
        subscriptionId: Long,
        event: IcsEvent,
        startMillis: Long,
        duration: Long,
    ): IcsEventCacheEntity =
        IcsEventCacheEntity(
            subscriptionId = subscriptionId,
            uid = event.uid,
            startEpochMillis = startMillis,
            endEpochMillis = startMillis + duration,
            allDay = event.start is IcsDateTime.Date,
            summary = event.summary.orEmpty(),
            description = event.description.orEmpty(),
        )

    /** DTSTART and, for an RRULE, its occurrences starting on or before [lastDay]. */
    private fun ruleStarts(
        event: IcsEvent,
        lastDay: LocalDate,
    ): List<Start> {
        val recurrence = event.recurrence ?: return listOf(Start(dayOf(event.start), epochMillis(event.start)))
        val until = recurrence.until?.let { untilMillis(it, event.start) }
        val first = GregorianCalendarSystem.fromJdn(dayOf(event.start).toJdn())
        return RecurrenceEngine(GregorianCalendarSystem)
            .occurrences(first, recurrence.copy(until = null).toRecurrenceRule())
            .map { it.toLocalDate() }
            .takeWhile { it <= lastDay }
            .map { Start(it, epochMillis(event.start.on(it))) }
            .takeWhile { until == null || it.epochMillis <= until }
            .take(maxOccurrences)
            .toList()
    }

    /** The last allowed start for UNTIL: a DATE bounds by day, other values by instant. */
    private fun untilMillis(
        until: IcsDateTime,
        start: IcsDateTime,
    ): Long = if (until is IcsDateTime.Date) epochMillis(start.on(until.date)) else epochMillis(until)

    private fun durationMillis(event: IcsEvent): Long {
        val start = event.start
        val end = event.end
        return when {
            start is IcsDateTime.Date && end is IcsDateTime.Date -> {
                maxOf(1, start.date.daysUntil(end.date)) * DAY_MILLIS
            }

            start is IcsDateTime.Date -> {
                DAY_MILLIS
            }

            end == null || end is IcsDateTime.Date -> {
                0L
            }

            else -> {
                maxOf(0L, epochMillis(end) - epochMillis(start))
            }
        }
    }

    private fun overlaps(
        startMillis: Long,
        durationMillis: Long,
        window: InstantWindow,
    ): Boolean =
        startMillis < window.toEpochMillis &&
            (startMillis + durationMillis > window.fromEpochMillis || startMillis >= window.fromEpochMillis)

    /** This value moved to [day], keeping its time of day and type. */
    private fun IcsDateTime.on(day: LocalDate): IcsDateTime =
        when (this) {
            is IcsDateTime.Date -> {
                IcsDateTime.Date(day)
            }

            is IcsDateTime.Floating -> {
                IcsDateTime.Floating(LocalDateTime(day, dateTime.time))
            }

            is IcsDateTime.Zoned -> {
                IcsDateTime.Zoned(LocalDateTime(day, dateTime.time), timeZoneId)
            }

            is IcsDateTime.Utc -> {
                IcsDateTime.Utc(
                    LocalDateTime(day, instant.toLocalDateTime(TimeZone.UTC).time).toInstant(TimeZone.UTC),
                )
            }
        }

    private fun dayOf(value: IcsDateTime): LocalDate =
        when (value) {
            is IcsDateTime.Date -> value.date
            is IcsDateTime.Floating -> value.dateTime.date
            is IcsDateTime.Zoned -> value.dateTime.date
            is IcsDateTime.Utc -> dayOfUtc(value.instant.toEpochMilliseconds())
        }

    private fun dayOfUtc(epochMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date

    private fun epochMillis(value: IcsDateTime): Long =
        when (value) {
            is IcsDateTime.Date -> value.date.atStartOfDayIn(TimeZone.UTC)
            is IcsDateTime.Floating -> value.dateTime.toInstant(zone)
            is IcsDateTime.Utc -> value.instant
            is IcsDateTime.Zoned -> value.dateTime.toInstant(TimeZone.of(value.timeZoneId))
        }.toEpochMilliseconds()

    companion object {
        /** Most rows kept per event. */
        const val MAX_OCCURRENCES: Int = 1_000
        private const val DAY_MILLIS = 86_400_000L
        private const val MARGIN_DAYS = 2
    }
}
