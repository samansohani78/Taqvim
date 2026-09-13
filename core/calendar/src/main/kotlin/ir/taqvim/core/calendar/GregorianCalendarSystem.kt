/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Proleptic Gregorian calendar with astronomical year numbering (year 0 = 1 BC), as in ISO 8601 (A-01).
 *
 * Conversion uses the integer algorithm of Fliegel & Van Flandern (1968). That algorithm is only valid for
 * positive day counts, so dates are first shifted by whole 400-year Gregorian cycles (146 097 days) into
 * years 0‥399, converted, and shifted back — exact for every [Int] year. See docs/PROVENANCE.md (A-01).
 */
public object GregorianCalendarSystem : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.GREGORIAN

    /** Days in a 400-year Gregorian cycle. */
    public const val DAYS_PER_CYCLE: Long = 146_097L

    /** Years in a Gregorian cycle. */
    public const val YEARS_PER_CYCLE: Int = 400

    /** Day number of 0000-01-01 (1 January 1 BC, proleptic Gregorian). */
    public const val JDN_OF_YEAR_ZERO: Long = 1_721_060L

    private const val JULIAN_LEAP_CYCLE = 4
    private const val CENTURY = 100
    private const val MONTHS = 12
    private const val FEBRUARY = 2
    private val MONTH_LENGTHS = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    override fun isLeapYear(year: Int): Boolean =
        year % JULIAN_LEAP_CYCLE == 0 && (year % CENTURY != 0 || year % YEARS_PER_CYCLE == 0)

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Gregorian month must be in 1..12 (was $month)" }
        return if (month == FEBRUARY && isLeapYear(year)) MONTH_LENGTHS[month - 1] + 1 else MONTH_LENGTHS[month - 1]
    }

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected a GREGORIAN date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) { "Gregorian date ${date.toIsoLikeString()} does not exist" }
        val cycles = Math.floorDiv(date.year.toLong(), YEARS_PER_CYCLE.toLong())
        val yearInCycle = (date.year - cycles * YEARS_PER_CYCLE).toInt()
        val jdnInCycle = fliegelVanFlandernToJdn(yearInCycle, date.month, date.day)
        return Jdn(Math.addExact(jdnInCycle, Math.multiplyExact(cycles, DAYS_PER_CYCLE)))
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        val cycles = Math.floorDiv(jdn.value - JDN_OF_YEAR_ZERO, DAYS_PER_CYCLE)
        val jdnInCycle = jdn.value - cycles * DAYS_PER_CYCLE
        val (yearInCycle, month, day) = fliegelVanFlandernFromJdn(jdnInCycle)
        val year =
            Math.toIntExact(
                Math.addExact(yearInCycle.toLong(), Math.multiplyExact(cycles, YEARS_PER_CYCLE.toLong())),
            )
        return CalendarDate(system, year, month, day)
    }

    // The numeric constants below are the published coefficients of the cited algorithm; naming them would
    // obscure the correspondence with the paper, so MagicNumber is suppressed for these two functions only.

    /** Fliegel & Van Flandern (1968) date → JDN; valid for years ≥ −4800. Division truncates toward zero. */
    @Suppress("MagicNumber")
    private fun fliegelVanFlandernToJdn(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val a = (month - 14) / 12
        return day - 32_075L +
            1_461L * (year + 4_800 + a) / 4 +
            367L * (month - 2 - a * 12) / 12 -
            3L * ((year + 4_900 + a) / 100) / 4
    }

    /** Fliegel & Van Flandern (1968) JDN → date; valid for JDN ≥ 0. Division truncates toward zero. */
    @Suppress("MagicNumber")
    private fun fliegelVanFlandernFromJdn(jdn: Long): Triple<Int, Int, Int> {
        var l = jdn + 68_569L
        val n = 4L * l / 146_097L
        l -= (146_097L * n + 3L) / 4L
        val i = 4_000L * (l + 1L) / 1_461_001L
        l = l - 1_461L * i / 4L + 31L
        val j = 80L * l / 2_447L
        val day = l - 2_447L * j / 80L
        l = j / 11L
        val month = j + 2L - 12L * l
        val year = 100L * (n - 49L) + i + l
        return Triple(year.toInt(), month.toInt(), day.toInt())
    }
}
