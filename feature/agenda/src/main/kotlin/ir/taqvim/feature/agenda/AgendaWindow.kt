/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

/**
 * The loaded months of the list, [first]‥[last] as offsets from the month containing today (T-901). The list is
 * "infinite": it grows by [STEP] months when either end is reached, keeps at most [MAX_SPAN] months by dropping the far
 * end, and stops at ±[LIMIT] months (a century either way).
 */
data class AgendaWindow(
    val first: Int,
    val last: Int,
) {
    init {
        require(first <= last) { "first ($first) must not be after last ($last)" }
        require(last - first < MAX_SPAN) { "at most $MAX_SPAN months (was $first‥$last)" }
        require(first >= -LIMIT && last <= LIMIT) { "months beyond ±$LIMIT (was $first‥$last)" }
    }

    val canLoadEarlier: Boolean get() = first > -LIMIT

    val canLoadLater: Boolean get() = last < LIMIT

    operator fun contains(offset: Int): Boolean = offset in first..last

    /** [STEP] more months before [first]; the latest months are dropped beyond [MAX_SPAN]. */
    fun earlier(): AgendaWindow {
        val newFirst = maxOf(first - STEP, -LIMIT)
        return AgendaWindow(newFirst, minOf(last, newFirst + MAX_SPAN - 1))
    }

    /** [STEP] more months after [last]; the earliest months are dropped beyond [MAX_SPAN]. */
    fun later(): AgendaWindow {
        val newLast = minOf(last + STEP, LIMIT)
        return AgendaWindow(maxOf(first, newLast - MAX_SPAN + 1), newLast)
    }

    companion object {
        /** Months added at an end when it is reached. */
        const val STEP: Int = 3

        /**
         * Most months loaded at once. Every month has at least two rows (header and days or an empty note), so a full
         * window is always taller than a screen and both ends are never reached together.
         */
        const val MAX_SPAN: Int = 24

        /** Farthest month from today in either direction. */
        const val LIMIT: Int = 1_200

        /** The month before today's month through two months after it. */
        val INITIAL: AgendaWindow = AgendaWindow(-1, 2)
    }
}
