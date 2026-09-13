/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * Consecutive lunar Hijri months with published lengths: month [firstMonth] of [firstYear] starts at [firstStartJdn],
 * and each following month has the next of [lengths] days (29 or 30).
 */
public class IslamicMonthTable(
    public val firstYear: Int,
    public val firstMonth: Int,
    public val firstStartJdn: Long,
    lengths: List<Int>,
) {
    private val monthLengths: List<Int> = lengths.toList()
    private val starts: LongArray =
        monthLengths.runningFold(firstStartJdn) { start, length -> start + length }.toLongArray()

    init {
        require(firstMonth in 1..MONTHS) { "first month must be in 1..12 (was $firstMonth)" }
        require(monthLengths.isNotEmpty()) { "an Islamic month table needs at least one month" }
        require(monthLengths.all { it == SHORT_MONTH || it == LONG_MONTH }) { "lunar months have 29 or 30 days" }
    }

    /** Number of months in the table. */
    public val monthCount: Int
        get() = monthLengths.size

    /** JDN of the first day after the table (the start of the month following the last one). */
    public val endJdn: Long
        get() = starts.last()

    /** Year and month right after the table. */
    public val next: Pair<Int, Int>
        get() = yearMonthAt(monthCount)

    /** Whether [jdn] falls inside a tabulated month. */
    public fun covers(jdn: Long): Boolean = jdn >= firstStartJdn && jdn < endJdn

    /** Position of [year]-[month] relative to the first month; may lie outside `0 until monthCount`. */
    internal fun indexOf(
        year: Int,
        month: Int,
    ): Long = (year.toLong() - firstYear) * MONTHS + (month - firstMonth)

    internal fun startAt(index: Int): Long = starts[index]

    internal fun lengthAt(index: Int): Int = monthLengths[index]

    /** Year and month at [index] months after the first month. */
    internal fun yearMonthAt(index: Int): Pair<Int, Int> {
        val absolute = firstMonth - 1 + index
        return (firstYear + Math.floorDiv(absolute, MONTHS)) to (Math.floorMod(absolute, MONTHS) + 1)
    }

    /** Index of the month containing [jdn], which must be [covers]ed. */
    internal fun indexContaining(jdn: Long): Int {
        val found = starts.binarySearch(jdn)
        return if (found >= 0) found else -found - 2
    }

    private companion object {
        const val MONTHS = 12
        const val SHORT_MONTH = 29
        const val LONG_MONTH = 30
    }
}
