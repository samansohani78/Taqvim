/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant

/** Shown days, columns and prayer lines of the timeline (T-900); pure and independent of Android. */
object TimelineContentBuilder {
    private const val DAYS_PER_WEEK = 7
    private const val NOON_HOUR = 12
    private const val SECONDS_PER_MINUTE = 60

    /** The days [mode] shows around [anchor]: the day itself, or its week starting on [weekStart]. */
    fun range(
        mode: TimelineMode,
        anchor: Jdn,
        weekStart: Weekday,
    ): JdnRange =
        when (mode) {
            TimelineMode.DAY -> {
                anchor..anchor
            }

            TimelineMode.WEEK -> {
                val first = anchor - Math.floorMod(anchor.weekday().ordinal - weekStart.ordinal, DAYS_PER_WEEK)
                first..first + (DAYS_PER_WEEK - 1)
            }
        }

    /** The first of [calendars], else the Gregorian calendar. */
    fun primaryCalendar(
        calendars: List<CalendarSystem>,
        variant: IslamicVariant,
    ): CalendarArithmetic =
        calendars.firstOrNull()?.let { IslamicCalendarSelection.arithmeticFor(it, variant) } ?: GregorianCalendarSystem

    /** Columns for [range] with the events of [days] (missing days are empty) and the prayer lines of [lines]. */
    fun columns(
        range: JdnRange,
        days: List<TimelineDay>,
        lines: (Jdn) -> ImmutableList<PrayerLine>,
    ): ImmutableList<TimelineColumn> {
        val byDay = days.associateBy { it.jdn }
        return range
            .map { jdn ->
                val day = byDay[jdn]
                val events = day?.events.orEmpty()
                TimelineColumn(
                    jdn = jdn,
                    isHoliday = day?.isHoliday == true,
                    isWeekend = day?.isWeekend == true,
                    allDay = events.filter { it.isAllDay }.toImmutableList(),
                    timed = placed(events.filterNot { it.isAllDay }),
                    prayerLines = lines(jdn),
                )
            }.toImmutableList()
    }

    /** Timed [events] with their columns, in the greedy order. */
    fun placed(events: List<TimelineEvent>): ImmutableList<PlacedEvent> {
        val intervals =
            events.mapIndexed { index, event -> TimedInterval(index.toString(), event.startMinute, event.endMinute) }
        return IntervalColoring
            .assign(intervals)
            .map { slot -> PlacedEvent(events[slot.interval.key.toInt()], slot.column, slot.columns) }
            .toImmutableList()
    }

    /** The prayer lines of [day] at [place]; none without a place or when the Sun does not rise or set. */
    fun prayerLines(
        day: Jdn,
        place: TimelinePlace?,
    ): ImmutableList<PrayerLine> {
        if (place == null) return persistentListOf()
        val zone = place.timeZone
        val noon = day.toLocalDate().atTime(NOON_HOUR, 0).toInstant(zone)
        val offsetMinutes = zone.offsetAt(noon).totalSeconds / SECONDS_PER_MINUTE
        return when (
            val result =
                PrayerTimesCalculator.calculate(
                    day,
                    place.coordinates,
                    offsetMinutes,
                    place.prayer,
                )
        ) {
            is PrayerTimesResult.Unavailable -> persistentListOf()
            is PrayerTimesResult.Available -> lines(result.times)
        }
    }

    private fun lines(times: PrayerTimes): ImmutableList<PrayerLine> =
        listOf<Pair<PrayerLineKind, MinuteOfDay?>>(
            PrayerLineKind.FAJR to times.fajr,
            PrayerLineKind.SUNRISE to times.sunrise,
            PrayerLineKind.DHUHR to times.dhuhr,
            PrayerLineKind.ASR to times.asr,
            PrayerLineKind.SUNSET to times.sunset,
            PrayerLineKind.MAGHRIB to times.maghrib,
            PrayerLineKind.ISHA to times.isha,
        ).mapNotNull { (kind, time) -> time?.let { PrayerLine(kind, it.value) } }.toImmutableList()
}
