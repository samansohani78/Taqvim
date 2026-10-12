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

/**
 * Civil days for events stored as instants, independent of any one source (ADR-0031).
 *
 * This was `DeviceEventMapping` while the device calendar provider was one of those sources. That provider is gone
 * (ADR-0051) and so are subscribed feeds (ADR-0054), but the rule outlives both readers that needed it: a timed
 * personal occurrence is still an instant that has to be assigned to a civil day.
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
}
