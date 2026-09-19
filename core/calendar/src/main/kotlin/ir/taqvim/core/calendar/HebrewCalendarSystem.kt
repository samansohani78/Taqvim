/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * [HebrewCalendar] as a [CalendarArithmetic] (F07): months are numbered from Tishri, 12 in a common year and 13 in a
 * leap year, so [monthsPerYear] is `null` and month arithmetic steps year by year.
 */
public object HebrewCalendarSystem : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.HEBREW

    override val monthsPerYear: Int? = null

    override fun isLeapYear(year: Int): Boolean = HebrewCalendar.isLeapYear(year)

    override fun monthsInYear(year: Int): Int = HebrewCalendar.monthsInYear(year)

    /**
     * The named month of [fromYear] in [year] (review R01): Adar I and Adar II of a leap year both fall in Adar of a
     * common year, Adar of a common year falls in Adar II of a leap year (where Purim and the anniversaries of Adar
     * are kept), and every other month keeps its name, so Elul — the 13th month of a leap year — stays Elul.
     */
    override fun sameMonthIn(
        fromYear: Int,
        month: Int,
        year: Int,
    ): Int {
        val named = HebrewCalendar.monthOf(fromYear, month)
        val leap = isLeapYear(year)
        val target =
            when {
                named == HebrewMonth.ADAR_II && !leap -> HebrewMonth.ADAR
                named == HebrewMonth.ADAR && leap && !isLeapYear(fromYear) -> HebrewMonth.ADAR_II
                else -> named
            }
        return HebrewCalendar.monthNumber(year, target)
    }

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int = HebrewCalendar.monthLength(year, month)

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected a HEBREW date (was ${date.system})" }
        return HebrewCalendar.toJdn(HebrewDate(date.year, date.month, date.day))
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        val date = HebrewCalendar.fromJdn(jdn)
        return CalendarDate(system, date.year, date.month, date.day)
    }
}
