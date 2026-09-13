/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import kotlin.time.Duration
import kotlin.time.Instant

/** Finds where a time predicate holds by scanning with a fixed step and bisecting each change of value. */
internal object IntervalSearch {
    /**
     * Intervals within [from] until [until] where [predicate] holds, scanned every [step] (changes shorter than a step
     * may be missed) with boundaries refined to [precision]; intervals are clipped to the window.
     */
    fun intervals(
        from: Instant,
        until: Instant,
        step: Duration,
        precision: Duration,
        predicate: (Instant) -> Boolean,
    ): List<TimeInterval> {
        require(from < until) { "from must be before until" }
        val intervals = mutableListOf<TimeInterval>()
        var openedAt: Instant? = if (predicate(from)) from else null
        var time = from
        while (time < until) {
            val next = minOf(time + step, until)
            val holds = predicate(next)
            val start = openedAt
            if (holds && start == null) {
                openedAt = boundary(time, next, precision, predicate)
            } else if (!holds && start != null) {
                intervals += TimeInterval(start, boundary(time, next, precision, predicate))
                openedAt = null
            }
            time = next
        }
        openedAt?.let { intervals += TimeInterval(it, until) }
        return intervals
    }

    /** First instant (to [precision]) after [before] where [predicate] has the value it has at [after]. */
    fun boundary(
        before: Instant,
        after: Instant,
        precision: Duration,
        predicate: (Instant) -> Boolean,
    ): Instant {
        val target = predicate(after)
        var low = before
        var high = after
        while (high - low > precision) {
            val middle = low + (high - low) / 2
            if (predicate(middle) == target) high = middle else low = middle
        }
        return high
    }
}
