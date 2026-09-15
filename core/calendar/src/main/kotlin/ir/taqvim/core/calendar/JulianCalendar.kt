/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn

/**
 * A day of the proleptic Julian calendar with astronomical year numbering (year 0 = 1 BC, −1 = 2 BC). Only the
 * structural invariants are checked here; whether the date exists is decided by [JulianCalendar].
 */
public data class JulianDate(
    public val year: Long,
    public val month: Int,
    public val day: Int,
) {
    init {
        require(month in 1..JulianCalendar.MONTHS) { "Julian month must be in 1..12 (was $month)" }
        require(day >= 1) { "day must be ≥ 1 (was $day)" }
    }

    override fun toString(): String = "JulianDate($year-$month-$day)"
}

/**
 * The proleptic Julian calendar (T-109): a leap year every fourth year, no exceptions. Conversion uses the integer
 * month-shift algorithm (E. G. Richards, "Calendars", Explanatory Supplement to the Astronomical Almanac, 3rd ed.,
 * §15.11), which needs non-negative day counts, so years are first shifted by whole 4-year cycles (1 461 days) into
 * 0‥3 and shifted back. Exact for every year in [MIN_YEAR]..[MAX_YEAR]; larger magnitudes are rejected with
 * [IllegalArgumentException] so no day count can leave the [Long] range.
 */
public object JulianCalendar {
    /** Months in a Julian year. */
    public const val MONTHS: Int = 12

    /** Earliest supported year. */
    public const val MIN_YEAR: Long = -1_000_000_000_000_000L

    /** Latest supported year. */
    public const val MAX_YEAR: Long = 1_000_000_000_000_000L

    /** Days in a 4-year Julian cycle. */
    public const val DAYS_PER_CYCLE: Long = 1_461L

    /** Years in a leap cycle. */
    public const val YEARS_PER_CYCLE: Long = 4L

    /** Day number of 1 January of year 0 (1 BC) in the Julian calendar. */
    public const val JDN_OF_YEAR_ZERO: Long = 1_721_058L

    private const val LEAP_YEAR_DAYS = 366L

    /** Day number of 1 January of [MIN_YEAR]. */
    public const val FIRST_JDN: Long = JDN_OF_YEAR_ZERO + MIN_YEAR / YEARS_PER_CYCLE * DAYS_PER_CYCLE

    /** Day number of 31 December of [MAX_YEAR] (a leap year). */
    public const val LAST_JDN: Long =
        JDN_OF_YEAR_ZERO + MAX_YEAR / YEARS_PER_CYCLE * DAYS_PER_CYCLE + LEAP_YEAR_DAYS - 1

    private const val FEBRUARY = 2
    private val MONTH_LENGTHS = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /** Whether [year] has 29 February; any [Long]. */
    public fun isLeapYear(year: Long): Boolean = Math.floorMod(year, YEARS_PER_CYCLE) == 0L

    /** Days in [month] of [year]. */
    public fun monthLength(
        year: Long,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Julian month must be in 1..12 (was $month)" }
        return MONTH_LENGTHS[month - 1] + if (month == FEBRUARY && isLeapYear(year)) 1 else 0
    }

    /** Day number of [date]; throws for a day that does not exist or a year outside [MIN_YEAR]..[MAX_YEAR]. */
    public fun toJdn(date: JulianDate): Jdn {
        requireYear(date.year)
        require(date.day <= monthLength(date.year, date.month)) { "Julian date $date does not exist" }
        val cycles = Math.floorDiv(date.year, YEARS_PER_CYCLE)
        val yearInCycle = (date.year - cycles * YEARS_PER_CYCLE).toInt()
        return Jdn(richardsToJdn(yearInCycle, date.month, date.day) + cycles * DAYS_PER_CYCLE)
    }

    /** Julian date of [jdn]; throws outside [FIRST_JDN]..[LAST_JDN]. */
    public fun fromJdn(jdn: Jdn): JulianDate {
        require(jdn.value in FIRST_JDN..LAST_JDN) { "Julian day numbers must be in $FIRST_JDN..$LAST_JDN (was $jdn)" }
        val cycles = Math.floorDiv(jdn.value - JDN_OF_YEAR_ZERO, DAYS_PER_CYCLE)
        val (yearInCycle, month, day) = richardsFromJdn(jdn.value - cycles * DAYS_PER_CYCLE)
        return JulianDate(yearInCycle + cycles * YEARS_PER_CYCLE, month, day)
    }

    private fun requireYear(year: Long) {
        require(year in MIN_YEAR..MAX_YEAR) { "Julian year must be in $MIN_YEAR..$MAX_YEAR (was $year)" }
    }

    // The numeric constants are the published coefficients of Richards' algorithm; naming them would obscure the
    // correspondence with the book, so MagicNumber is suppressed for these two functions only.

    /** Richards' Julian date → JDN for a year in 0‥3 (all intermediate values non-negative). */
    @Suppress("MagicNumber")
    private fun richardsToJdn(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val a = (14 - month) / 12
        val y = year + 4_800L - a
        val m = month + 12 * a - 3
        return day + (153L * m + 2) / 5 + 365 * y + y / 4 - 32_083
    }

    /** Richards' JDN → Julian (year, month, day) for a day of years 0‥3. */
    @Suppress("MagicNumber")
    private fun richardsFromJdn(jdn: Long): Triple<Long, Int, Int> {
        val c = jdn + 32_082
        val d = (4 * c + 3) / 1_461
        val e = c - 1_461 * d / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        return Triple(d - 4_800 + m / 10, month, day)
    }
}
