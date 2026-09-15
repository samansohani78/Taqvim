/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.Jdn

/**
 * A day of the Hebrew calendar: [year] Anno Mundi, [month] counted from Tishri (see [HebrewMonth]) and 1-based [day].
 * Only the structural invariants are checked here; whether the date exists is decided by [HebrewCalendar].
 */
public data class HebrewDate(
    public val year: Int,
    public val month: Int,
    public val day: Int,
) {
    init {
        require(month >= 1) { "month must be ≥ 1 (was $month)" }
        require(day >= 1) { "day must be ≥ 1 (was $day)" }
    }
}

/**
 * Months of the Hebrew year in their order from Tishri. [ADAR] is the only Adar of a common year and Adar I of a leap
 * year; [ADAR_II] exists only in leap years.
 */
public enum class HebrewMonth {
    TISHRI,
    HESHVAN,
    KISLEV,
    TEVET,
    SHEVAT,
    ADAR,
    ADAR_II,
    NISAN,
    IYAR,
    SIVAN,
    TAMMUZ,
    AV,
    ELUL,
}

/**
 * The fixed (arithmetic) Hebrew calendar (T-108), from its published rules: a 19-year cycle with leap years 3, 6, 8,
 * 11, 14, 17 and 19 that add a 30-day Adar I; the year starts on 1 Tishri, fixed from the mean conjunction (molad) of
 * Tishri, 29 d 12 h 793 parts after the previous one, the first being BaHaRaD (Monday 5 h 204 parts, hours counted
 * from 6 p.m.), then postponed by the four dehiyyot; the year length decides whether Heshvan and Kislev have 29 or 30
 * days. 1 Tishri AM 1 is Monday 7 October 3761 BC (Julian) = [EPOCH_JDN]. Months are numbered from Tishri (1) to
 * Elul (12, or 13 in a leap year). See docs/PROVENANCE.md (T-108).
 *
 * Pure arithmetic with no year table: every year in [MIN_YEAR]..[MAX_YEAR] (the [Int] range less one year at each end,
 * which the year-length rules need) and every day in [FIRST_JDN]..[LAST_JDN] is exact; values outside are rejected
 * with [IllegalArgumentException]. Intermediate part counts stay below 4 × 10^17, well inside [Long].
 */
public object HebrewCalendar {
    /** JDN of 1 Tishri AM 1 (Monday 7 October 3761 BC, proleptic Julian). */
    public const val EPOCH_JDN: Long = 347_998L

    /** Earliest supported year. */
    public const val MIN_YEAR: Int = Int.MIN_VALUE + 1

    /** Latest supported year. */
    public const val MAX_YEAR: Int = Int.MAX_VALUE - 1

    /** Day number of 1 Tishri of [MIN_YEAR]. */
    public const val FIRST_JDN: Long = -784_361_230_200L

    /** Day number of the last day of [MAX_YEAR]. */
    public const val LAST_JDN: Long = 784_361_925_428L

    /** Parts (halakim) in a day: 24 hours of 1 080 parts. */
    public const val PARTS_PER_DAY: Long = 25_920L

    /** Parts in a mean lunation: 29 d 12 h 793 p. */
    public const val PARTS_PER_MONTH: Long = 765_433L

    /** Years after which the calendar repeats exactly (the molad returns to the same weekday and part). */
    public const val FULL_CYCLE_YEARS: Int = 689_472

    /** Days in [FULL_CYCLE_YEARS] years (35 975 351 weeks). */
    public const val FULL_CYCLE_DAYS: Long = 251_827_457L

    private const val PARTS_PER_HOUR = 1_080L
    private const val MOLAD_OF_YEAR_ONE = PARTS_PER_DAY + 5 * PARTS_PER_HOUR + 204
    private const val MOLAD_ZAKEN = 18 * PARTS_PER_HOUR
    private const val GATARAD = 9 * PARTS_PER_HOUR + 204
    private const val BETUTAKPAT = 15 * PARTS_PER_HOUR + 589
    private const val CYCLE_YEARS = 19L
    private const val MONTHS_PER_CYCLE = 235L
    private const val LEAP_YEARS_PER_CYCLE = 7L
    private const val DAYS_PER_WEEK = 7L
    private const val MONDAY = 1L
    private const val TUESDAY = 2L
    private const val COMMON_MONTHS = 12
    private const val LEAP_MONTHS = 13
    private const val LONG_MONTH = 30
    private const val SHORT_MONTH = 29
    private const val DEFICIENT_UNITS = 3
    private const val COMPLETE_UNITS = 5
    private const val UNITS = 10

    /** Weekdays (0 = Sunday) on which 1 Tishri may not fall (lo ADU rosh: Sunday, Wednesday, Friday). */
    private val ADU = setOf(0L, 3L, 5L)

    /** Whether [year] has Adar I (the 3rd, 6th, 8th, 11th, 14th, 17th and 19th years of the cycle); any [Int]. */
    public fun isLeapYear(year: Int): Boolean =
        Math.floorMod(LEAP_YEARS_PER_CYCLE * year + 1, CYCLE_YEARS) < LEAP_YEARS_PER_CYCLE

    /** 13 in a leap year, else 12; any [Int]. */
    public fun monthsInYear(year: Int): Int = if (isLeapYear(year)) LEAP_MONTHS else COMMON_MONTHS

    /** The number of [month] in [year]; throws [IllegalArgumentException] for Adar II in a common year. */
    public fun monthNumber(
        year: Int,
        month: HebrewMonth,
    ): Int {
        val leap = isLeapYear(year)
        require(month != HebrewMonth.ADAR_II || leap) { "Hebrew year $year has no Adar II" }
        return if (leap || month < HebrewMonth.ADAR_II) month.ordinal + 1 else month.ordinal
    }

    /** Day number of 1 Tishri of [year], for [MIN_YEAR]..[MAX_YEAR] + 1. */
    public fun newYear(year: Int): Jdn {
        require(year >= MIN_YEAR) { "Hebrew year must be ≥ $MIN_YEAR (was $year)" }
        return Jdn(EPOCH_JDN - 1 + daysToNewYear(year))
    }

    /** Days in [year]: 353, 354 or 355, or 383, 384 or 385 in a leap year. */
    public fun yearLength(year: Int): Int {
        checkYear(year)
        return (newYear(year + 1) - newYear(year)).toInt()
    }

    /** Days in [month] of [year]; throws [IllegalArgumentException] for a month the year does not have. */
    public fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        checkYear(year)
        require(month in 1..monthsInYear(year)) { "Hebrew month must be in 1..${monthsInYear(year)} (was $month)" }
        return when (month) {
            HebrewMonth.HESHVAN.ordinal + 1 -> {
                if (yearLength(year) % UNITS == COMPLETE_UNITS) LONG_MONTH else SHORT_MONTH
            }

            HebrewMonth.KISLEV.ordinal + 1 -> {
                if (yearLength(year) % UNITS == DEFICIENT_UNITS) SHORT_MONTH else LONG_MONTH
            }

            HebrewMonth.ADAR.ordinal + 1 -> {
                if (isLeapYear(year)) LONG_MONTH else SHORT_MONTH
            }

            else -> {
                alternatingMonthLength(year, month)
            }
        }
    }

    /** Whether [year]-[month]-[day] exists; throws [IllegalArgumentException] for a year outside the supported range. */
    public fun isValid(
        year: Int,
        month: Int,
        day: Int,
    ): Boolean {
        checkYear(year)
        return month in 1..monthsInYear(year) && day in 1..monthLength(year, month)
    }

    /** Day number of [date]; throws [IllegalArgumentException] when the date does not exist. */
    public fun toJdn(date: HebrewDate): Jdn {
        require(isValid(date.year, date.month, date.day)) { "Hebrew date $date does not exist" }
        val daysBeforeMonth = (1 until date.month).sumOf { monthLength(date.year, it) }
        return newYear(date.year) + (daysBeforeMonth + date.day - 1)
    }

    /** The Hebrew date of [jdn]; throws [IllegalArgumentException] outside [FIRST_JDN]..[LAST_JDN]. */
    public fun fromJdn(jdn: Jdn): HebrewDate {
        require(jdn.value in FIRST_JDN..LAST_JDN) { "Day $jdn is outside the supported Hebrew years" }
        var year = estimatedYear(jdn)
        while (newYear(year + 1) <= jdn) year++
        while (newYear(year) > jdn) year--
        var remaining = jdn - newYear(year)
        var month = 1
        while (remaining >= monthLength(year, month)) {
            remaining -= monthLength(year, month)
            month++
        }
        return HebrewDate(year, month, remaining.toInt() + 1)
    }

    /** Tishri 30, Tevet 29 and Shevat 30; from Adar II on, months alternate with Nisan at 30 days. */
    private fun alternatingMonthLength(
        year: Int,
        month: Int,
    ): Int {
        val reference = if (month <= HebrewMonth.SHEVAT.ordinal + 1) 1 else monthNumber(year, HebrewMonth.NISAN)
        return if (Math.floorMod(month - reference, 2) == 0) LONG_MONTH else SHORT_MONTH
    }

    /** A year near the true year of [jdn], from the mean year of 235 lunations per 19 years, kept in range. */
    private fun estimatedYear(jdn: Jdn): Int {
        val elapsedParts = (jdn.value - EPOCH_JDN) * PARTS_PER_DAY * CYCLE_YEARS
        val estimate = Math.floorDiv(elapsedParts, MONTHS_PER_CYCLE * PARTS_PER_MONTH) + 1
        return estimate.coerceIn(MIN_YEAR.toLong(), MAX_YEAR.toLong()).toInt()
    }

    /** Days from the Sunday before the epoch to 1 Tishri of [year]: the molad of Tishri with the four dehiyyot. */
    private fun daysToNewYear(year: Int): Long {
        val monthsBefore = Math.floorDiv(MONTHS_PER_CYCLE * year - (MONTHS_PER_CYCLE - 1), CYCLE_YEARS)
        val molad = MOLAD_OF_YEAR_ONE + monthsBefore * PARTS_PER_MONTH
        val day = Math.floorDiv(molad, PARTS_PER_DAY)
        val parts = Math.floorMod(molad, PARTS_PER_DAY)
        val weekday = Math.floorMod(day, DAYS_PER_WEEK)
        val postponed =
            when {
                parts >= MOLAD_ZAKEN -> day + 1
                weekday == TUESDAY && parts >= GATARAD && !isLeapYear(year) -> day + 2
                weekday == MONDAY && parts >= BETUTAKPAT && isLeapYear(year - 1) -> day + 1
                else -> day
            }
        return if (Math.floorMod(postponed, DAYS_PER_WEEK) in ADU) postponed + 1 else postponed
    }

    private fun checkYear(year: Int) {
        require(year in MIN_YEAR..MAX_YEAR) { "Hebrew year must be in $MIN_YEAR..$MAX_YEAR (was $year)" }
    }
}
