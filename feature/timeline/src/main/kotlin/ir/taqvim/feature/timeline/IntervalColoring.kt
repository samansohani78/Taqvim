/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

/** The half-open interval from [start] until [end] (minutes), identified by [key]. */
data class TimedInterval(
    val key: String,
    val start: Int,
    val end: Int,
) {
    init {
        require(end > start) { "interval '$key' must end after it starts ($start..$end)" }
    }
}

/** [interval] drawn in [column] of [columns] equal columns. */
data class IntervalSlot(
    val interval: TimedInterval,
    val column: Int,
    val columns: Int,
)

/**
 * Interval-graph coloring for the timeline (T-900). Intervals are taken greedily by start (longer first on equal
 * starts, then by key) and each takes the lowest column that is free at its start. Intervals that overlap directly or
 * through a chain form a cluster, and every interval of a cluster shares the cluster's column count. Greedy coloring in
 * start order is optimal for interval graphs, so a cluster has exactly as many columns as its deepest overlap.
 */
object IntervalColoring {
    private val ORDER: Comparator<TimedInterval> =
        compareBy<TimedInterval> { it.start }.thenByDescending { it.end }.thenBy { it.key }

    /** Columns for [intervals], in the greedy order (by start). */
    fun assign(intervals: List<TimedInterval>): List<IntervalSlot> {
        val slots = ArrayList<IntervalSlot>(intervals.size)
        val cluster = ArrayList<Pair<TimedInterval, Int>>()
        val columnEnds = ArrayList<Int>()
        var clusterEnd = Int.MIN_VALUE
        intervals.sortedWith(ORDER).forEach { interval ->
            if (interval.start >= clusterEnd) {
                cluster.mapTo(slots) { (member, column) -> IntervalSlot(member, column, columnEnds.size) }
                cluster.clear()
                columnEnds.clear()
            }
            val free = columnEnds.indexOfFirst { it <= interval.start }
            val column = if (free >= 0) free else columnEnds.size
            if (free >= 0) columnEnds[free] = interval.end else columnEnds += interval.end
            cluster += interval to column
            clusterEnd = maxOf(clusterEnd, interval.end)
        }
        cluster.mapTo(slots) { (member, column) -> IntervalSlot(member, column, columnEnds.size) }
        return slots
    }
}
