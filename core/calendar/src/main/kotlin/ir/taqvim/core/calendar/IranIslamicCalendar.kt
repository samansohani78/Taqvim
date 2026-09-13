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
 * ([IranOfficialMonthStarts]); elsewhere the tabular calendar ([TabularIslamicCalendar.TYPE_II]) shifted so that it
 * meets the table exactly at each edge. The shift keeps every month at 29 or 30 days across the edges
 * (docs/adr/0009-iran-islamic-calendar.md).
 */
public class IranIslamicCalendar(
    private val table: IslamicMonthTable = IranOfficialMonthStarts.TABLE,
) : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    private val tabular = TabularIslamicCalendar.TYPE_II
    private val shiftBefore: Long = table.firstStartJdn - tabularStart(table.firstYear, table.firstMonth)
    private val shiftAfter: Long = table.endJdn - tabularStart(table.next.first, table.next.second)

    /** Whether [jdn] is covered by the published table (as opposed to the shifted tabular estimate). */
    public fun isOfficial(jdn: Jdn): Boolean = table.covers(jdn.value)

    override fun isLeapYear(year: Int): Boolean = (1..MONTHS).sumOf { monthLength(year, it) } > COMMON_YEAR_DAYS

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Islamic month must be in 1..12 (was $month)" }
        val index = table.indexOf(year, month)
        val inTable = index in 0 until table.monthCount
        return if (inTable) table.lengthAt(index.toInt()) else tabular.monthLength(year, month)
    }

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected an ISLAMIC date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) { "Islamic date ${date.toIsoLikeString()} does not exist" }
        val index = table.indexOf(date.year, date.month)
        return when {
            index < 0 -> Jdn(tabular.toJdn(date).value + shiftBefore)
            index >= table.monthCount -> Jdn(tabular.toJdn(date).value + shiftAfter)
            else -> Jdn(table.startAt(index.toInt()) + date.day - 1)
        }
    }

    override fun fromJdn(jdn: Jdn): CalendarDate =
        when {
            jdn.value < table.firstStartJdn -> {
                tabular.fromJdn(Jdn(jdn.value - shiftBefore))
            }

            jdn.value >= table.endJdn -> {
                tabular.fromJdn(Jdn(jdn.value - shiftAfter))
            }

            else -> {
                val index = table.indexContaining(jdn.value)
                val (year, month) = table.yearMonthAt(index)
                CalendarDate(system, year, month, (jdn.value - table.startAt(index)).toInt() + 1)
            }
        }

    private fun tabularStart(
        year: Int,
        month: Int,
    ): Long = tabular.toJdn(CalendarDate(CalendarSystem.ISLAMIC, year, month, 1)).value

    private companion object {
        const val MONTHS = 12
        const val COMMON_YEAR_DAYS = 354
    }
}
