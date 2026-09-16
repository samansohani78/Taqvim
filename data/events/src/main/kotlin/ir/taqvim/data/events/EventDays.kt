/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.devicecalendar.DeviceEventMapping
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/**
 * The one rule deciding which civil days an event is shown on, for every source (B03, B12).
 *
 * - **All-day events keep their dates**, whatever the display zone: personal and dataset events by the dates the user
 *   or the dataset gives them, device and feed rows by their UTC dates (those sources store UTC-midnight bounds).
 * - **Timed events are dated by their instants**: the wall-clock time is read in the event's own zone, and the
 *   occurrence covers the days that interval falls on in the display zone. A meeting at 00:30 in Tokyo therefore
 *   belongs to the previous day for someone reading the calendar in UTC.
 *
 * Because a conversion can move an occurrence across a date boundary, sources must be read one day wider than the
 * shown range ([WIDENING]) and narrowed again by the assigned days.
 */
internal object EventDays {
    /** Days to read beyond each end of the shown range, so conversions into it are not missed. */
    const val WIDENING: Long = 1

    /** [days] widened by [WIDENING] on each side. */
    fun widened(days: JdnRange): JdnRange = (days.start - WIDENING)..(days.endInclusive + WIDENING)

    /**
     * The days [occurrence] covers in [zone]: its own dates when it is all-day, otherwise the days of its instants.
     * A zero-length occurrence keeps the day it starts on.
     */
    fun of(
        occurrence: PersonalOccurrence,
        zone: TimeZone,
    ): JdnRange {
        val start = occurrence.startMinute
        val end = occurrence.endMinute
        if (start == null || end == null) return occurrence.days
        val eventZone = zoneOf(occurrence.timeZoneId, zone)
        val begin = occurrence.days.start.at(start, eventZone)
        val finish = maxOf(occurrence.days.endInclusive.at(end, eventZone), begin)
        return DeviceEventMapping.days(
            beginEpochMillis = begin.toEpochMilliseconds(),
            endEpochMillis = finish.toEpochMilliseconds(),
            allDay = false,
            zone = zone,
        )
    }

    /** [id] as a time zone, falling back to [fallback] for a zone this device does not know. */
    fun zoneOf(
        id: String,
        fallback: TimeZone,
    ): TimeZone = runCatching { TimeZone.of(id) }.getOrDefault(fallback)

    /** The instant at [minute] of this civil day in [zone]; minute 1440 is the start of the next day. */
    private fun Jdn.at(
        minute: Int,
        zone: TimeZone,
    ): Instant =
        if (minute >= MINUTES_PER_DAY) {
            (this + 1).toLocalDate().atStartOfDayIn(zone)
        } else {
            toLocalDate().atTime(minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR).toInstant(zone)
        }

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
}
