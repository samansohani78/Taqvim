/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * Arithmetic Solar Hijri leap years of the 2820-year cycle attributed to A. Birashk, used by [PersianCalendarSystem]
 * only outside its astronomical table (A-02 fallback, ADR-0008).
 *
 * Structure as published by C. Tøndering, "The Persian Calendar" (docs/PROVENANCE.md, A-02): a 2820-year period is
 * made of cycles of 29, 33, 33 and 33 years repeated, the final cycle lengthened to 37 years; numbering the years of a
 * cycle from 0, the years divisible by 4 except year 0 are leap years; the current period started in AP 475.
 */
internal object BirashkArithmetic {
    /** Years in a full period. */
    const val PERIOD_YEARS: Int = 2820

    /** Leap years in a full period. */
    const val LEAP_YEARS_PER_PERIOD: Int = 683

    /** First year of the period that contains the present. */
    const val EPOCH_YEAR: Int = 475

    /** Mean year length of the period in days. */
    const val MEAN_YEAR_DAYS: Double = 365.0 + 683.0 / 2820.0

    private const val GRAND_CYCLE_YEARS = 128
    private const val LEAP_YEARS_PER_GRAND_CYCLE = 31
    private const val LAST_GRAND_CYCLE_START = 2688
    private const val LEAP_INTERVAL = 4
    private val GRAND_CYCLE = listOf(29, 33, 33, 33)
    private val LAST_GRAND_CYCLE = listOf(29, 33, 33, 37)

    fun isLeapYear(year: Int): Boolean = leapYearsBefore(year.toLong() + 1) - leapYearsBefore(year.toLong()) == 1L

    /** Number of leap years in [from] until [until] (exclusive); negative when [until] is before [from]. */
    fun leapYearsBetween(
        from: Int,
        until: Int,
    ): Long = leapYearsBefore(until.toLong()) - leapYearsBefore(from.toLong())

    /** Leap years before [year], counted from [EPOCH_YEAR] (negative for earlier years). */
    private fun leapYearsBefore(year: Long): Long {
        val sinceEpoch = year - EPOCH_YEAR
        val periods = Math.floorDiv(sinceEpoch, PERIOD_YEARS.toLong())
        val position = Math.floorMod(sinceEpoch, PERIOD_YEARS.toLong()).toInt()
        return periods * LEAP_YEARS_PER_PERIOD + leapYearsBeforePosition(position)
    }

    /** Leap years among positions 0 until [position] of a period. */
    private fun leapYearsBeforePosition(position: Int): Int =
        if (position < LAST_GRAND_CYCLE_START) {
            position / GRAND_CYCLE_YEARS * LEAP_YEARS_PER_GRAND_CYCLE +
                leapYearsInGrandCycle(position % GRAND_CYCLE_YEARS, GRAND_CYCLE)
        } else {
            LAST_GRAND_CYCLE_START / GRAND_CYCLE_YEARS * LEAP_YEARS_PER_GRAND_CYCLE +
                leapYearsInGrandCycle(position - LAST_GRAND_CYCLE_START, LAST_GRAND_CYCLE)
        }

    /** Leap years among the first [years] years of a grand cycle made of [cycles]. */
    private fun leapYearsInGrandCycle(
        years: Int,
        cycles: List<Int>,
    ): Int {
        var remaining = years
        var count = 0
        for (length in cycles) {
            val taken = minOf(remaining, length)
            count += leapYearsInCycle(taken)
            remaining -= taken
        }
        return count
    }

    /** Leap years among positions 0 until [years] of a cycle: every position divisible by 4 except 0. */
    private fun leapYearsInCycle(years: Int): Int = if (years <= 0) 0 else (years - 1) / LEAP_INTERVAL
}
