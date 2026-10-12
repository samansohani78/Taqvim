/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.JdnRange
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** A cache query window `[fromEpochMillis, toEpochMillis)` in UTC milliseconds. */
data class InstantWindow(
    val fromEpochMillis: Long,
    val toEpochMillis: Long,
)

/**
 * Civil days and query windows for events stored as instants, independent of any one source (ADR-0031).
 *
 * This was `DeviceEventMapping` while the device calendar provider was one of those sources. The provider is gone
 * (ADR-0053) and subscribed iCalendar feeds are not, so the rule outlives the reader that first needed it: feed rows
 * and timed personal occurrences are still instants that have to be assigned to civil days.
 */
object EventWindows {
    /**
     * Civil days covered by an occurrence whose end is exclusive. All-day rows are UTC-midnight bounded, so their
     * days are the UTC dates in every device zone; timed ones use their dates in [zone]. A zero-length occurrence
     * covers the day it starts on.
     */
    fun days(
        beginEpochMillis: Long,
        endEpochMillis: Long,
        allDay: Boolean,
        zone: TimeZone,
    ): JdnRange {
        val dayZone = if (allDay) TimeZone.UTC else zone
        val lastMillis = maxOf(beginEpochMillis, endEpochMillis - 1)
        val first = Instant.fromEpochMilliseconds(beginEpochMillis).toJdn(dayZone)
        return first..Instant.fromEpochMilliseconds(lastMillis).toJdn(dayZone)
    }

    /**
     * The cache window for [days] in [zone], widened by one day on each side so every all-day row of those days is
     * included whatever the zone offset; results are narrowed again by the days each row is assigned.
     */
    fun window(
        days: JdnRange,
        zone: TimeZone,
    ): InstantWindow {
        require(!days.isEmpty()) { "days must not be empty" }
        val from = (days.start - 1).toLocalDate().atStartOfDayIn(zone)
        val to = (days.endInclusive + 2).toLocalDate().atStartOfDayIn(zone)
        return InstantWindow(from.toEpochMilliseconds(), to.toEpochMilliseconds())
    }
}
