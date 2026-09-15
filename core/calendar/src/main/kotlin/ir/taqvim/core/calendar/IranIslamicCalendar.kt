/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Official Iranian lunar Hijri calendar (A-05): published month starts where the Calendar Center has announced them
 * ([table], by default [IranOfficialMonthStarts]); every other month from the Iran-calibrated crescent calendar
 * ([IranCrescentCalendar]), for any year (docs/adr/0009-iran-islamic-calendar.md,
 * docs/adr/0027-iran-islamic-months-computed.md).
 *
 * Joining rule: next to the table each month start is the crescent start kept within 29 and 30 days of its neighbour
 * towards the table, month by month outwards, until a month start equals the crescent start; from there on the crescent
 * months are used unchanged. Every month therefore has 29 or 30 days and no day is skipped or repeated at either edge.
 */
public class IranIslamicCalendar(
    private val table: IslamicMonthTable = IranOfficialMonthStarts.TABLE,
) : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    private val firstIndex = IranCrescentMonths.monthIndex(table.firstYear.toLong(), table.firstMonth)
    private val endIndex = firstIndex + table.monthCount
    private val joinedBefore: LongArray by lazy { join(firstIndex - 1, table.firstStartJdn, -1) }
    private val joinedAfter: LongArray by lazy { join(endIndex + 1, table.endJdn, 1) }
    private val arithmetic = LunarMonthArithmetic(::startJdn)

    /** Whether [jdn] is covered by the published table (as opposed to the crescent estimate). */
    public fun isOfficial(jdn: Jdn): Boolean = table.covers(jdn.value)

    override fun isLeapYear(year: Int): Boolean = arithmetic.yearLength(year) > COMMON_YEAR_DAYS

    override fun monthsInYear(year: Int): Int = IranCrescentMonths.MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int = arithmetic.monthLength(year, month)

    override fun toJdn(date: CalendarDate): Jdn = arithmetic.toJdn(date)

    override fun fromJdn(jdn: Jdn): CalendarDate = arithmetic.fromJdn(jdn)

    private fun startJdn(index: Long): Long {
        val intoTable = index - firstIndex
        val afterTable = index - endIndex
        return when {
            intoTable in 0..table.monthCount -> table.startAt(intoTable.toInt())
            intoTable < 0 && -intoTable <= joinedBefore.size -> joinedBefore[(-intoTable - 1).toInt()]
            afterTable in 1..joinedAfter.size -> joinedAfter[(afterTable - 1).toInt()]
            else -> IranCrescentMonths.startJdn(index)
        }
    }

    /**
     * Month starts from [from] outwards in [direction] (−1 before the table, +1 after it), each kept within 29 and 30
     * days of its neighbour, beginning next to the table month starting at [edgeStart]; ends before the first month
     * whose kept start equals the crescent start.
     */
    private fun join(
        from: Long,
        edgeStart: Long,
        direction: Int,
    ): LongArray {
        val joined = ArrayList<Long>()
        var index = from
        var start = kept(edgeStart, index, direction)
        while (start != IranCrescentMonths.startJdn(index)) {
            check(joined.size < MAX_JOINED_MONTHS) { "The crescent months never meet the table near month $from" }
            joined += start
            index += direction
            start = kept(start, index, direction)
        }
        return joined.toLongArray()
    }

    private fun kept(
        neighbour: Long,
        index: Long,
        direction: Int,
    ): Long {
        val crescent = IranCrescentMonths.startJdn(index)
        return if (direction > 0) {
            crescent.coerceIn(neighbour + SHORT_MONTH, neighbour + LONG_MONTH)
        } else {
            crescent.coerceIn(neighbour - LONG_MONTH, neighbour - SHORT_MONTH)
        }
    }

    private companion object {
        const val COMMON_YEAR_DAYS = 354L
        const val SHORT_MONTH = 29L
        const val LONG_MONTH = 30L
        const val MAX_JOINED_MONTHS = 1_200
    }
}
