/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday

/** A Gregorian month and day of month, for rules that name a day without fixing the year. */
public data class GregorianMonthDay(
    public val month: Int,
    public val day: Int,
)

/** The US national daylight saving time dates of [year]: the change to daylight time and the change back. */
public data class UsDaylightSavingDates(
    public val year: Long,
    public val begins: GregorianMonthDay,
    public val ends: GregorianMonthDay,
)

/**
 * US national daylight saving time begin and end dates (T-110) for any Gregorian year from 1967, with no upper bound.
 * The federal rule eras, codified at 15 U.S.C. §260a:
 * - Uniform Time Act of 1966 (Pub. L. 89-387), from 1967: last Sunday of April to last Sunday of October.
 * - Emergency Daylight Saving Time Energy Conservation Act of 1973 (Pub. L. 93-182), amended by Pub. L. 93-434:
 *   daylight time from 6 January 1974, and after the winter of 1974–75 from 23 February 1975; both years still end
 *   on the last Sunday of October.
 * - Pub. L. 99-359 (1986), from 1987: first Sunday of April to last Sunday of October.
 * - Energy Policy Act of 2005 (Pub. L. 109-58, §110), from 2007: second Sunday of March to first Sunday of November.
 *
 * The statutes change the clocks at 2:00 local time; states may opt out, so these are national dates, not a zone's.
 * Every era boundary is checked against all 16,066 dates the U.S. Naval Observatory publishes for 1967–9999
 * (UsDaylightSavingRulesTest). Gregorian weekdays repeat every 400 years (146,097 days = 20,871 weeks), so any
 * year is reduced into one cycle and no arithmetic can overflow.
 */
public object UsDaylightSavingRules {
    /** First year of the Uniform Time Act rules. */
    public const val FIRST_YEAR: Long = 1967L

    private const val JANUARY = 1
    private const val FEBRUARY = 2
    private const val MARCH = 3
    private const val APRIL = 4
    private const val OCTOBER = 10
    private const val NOVEMBER = 11
    private const val DAYS_PER_WEEK = 7
    private const val CYCLE_YEARS = 400L
    private const val CYCLE_BASE = 2000
    private const val FIRST_EMERGENCY_YEAR = 1974L
    private const val SECOND_EMERGENCY_YEAR = 1975L
    private const val FIRST_APRIL_RULE_YEAR = 1987L
    private const val ENERGY_POLICY_ACT_YEAR = 2007L
    private val EMERGENCY_1974_START = GregorianMonthDay(JANUARY, 6)
    private val EMERGENCY_1975_START = GregorianMonthDay(FEBRUARY, 23)

    /** The national dates of [year], or null before [FIRST_YEAR]. */
    public fun forYear(year: Long): UsDaylightSavingDates? {
        if (year < FIRST_YEAR) return null
        val cycleYear = CYCLE_BASE + Math.floorMod(year - CYCLE_BASE, CYCLE_YEARS).toInt()
        val (begins, ends) =
            when {
                year == FIRST_EMERGENCY_YEAR -> EMERGENCY_1974_START to lastSunday(cycleYear, OCTOBER)
                year == SECOND_EMERGENCY_YEAR -> EMERGENCY_1975_START to lastSunday(cycleYear, OCTOBER)
                year < FIRST_APRIL_RULE_YEAR -> lastSunday(cycleYear, APRIL) to lastSunday(cycleYear, OCTOBER)
                year < ENERGY_POLICY_ACT_YEAR -> nthSunday(cycleYear, APRIL, 1) to lastSunday(cycleYear, OCTOBER)
                else -> nthSunday(cycleYear, MARCH, 2) to nthSunday(cycleYear, NOVEMBER, 1)
            }
        return UsDaylightSavingDates(year, begins, ends)
    }

    private fun nthSunday(
        year: Int,
        month: Int,
        n: Int,
    ): GregorianMonthDay {
        val firstSunday = 1 + Math.floorMod(Weekday.SUNDAY.ordinal - weekday(year, month, 1).ordinal, DAYS_PER_WEEK)
        return GregorianMonthDay(month, firstSunday + DAYS_PER_WEEK * (n - 1))
    }

    private fun lastSunday(
        year: Int,
        month: Int,
    ): GregorianMonthDay {
        val lastDay = GregorianCalendarSystem.monthLength(year, month)
        val back = Math.floorMod(weekday(year, month, lastDay).ordinal - Weekday.SUNDAY.ordinal, DAYS_PER_WEEK)
        return GregorianMonthDay(month, lastDay - back)
    }

    private fun weekday(
        year: Int,
        month: Int,
        day: Int,
    ): Weekday = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day)).weekday()
}
