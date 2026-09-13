/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn

/**
 * Arithmetic ("tabular") Islamic calendar (A-03): a 30-year cycle with 11 leap years of 355 days, months
 * alternating 30 and 29 days, and the twelfth month lengthened to 30 days in leap years. Year 1 AH begins on
 * the civil epoch, Friday 16 July 622 (Julian), JD 1 948 439.5 → [EPOCH_JDN].
 *
 * The two variants differ only in cycle year 15 or 16 being the leap year. All arithmetic is derived directly
 * from those definitions (see docs/PROVENANCE.md, A-03) and works for every year, including years ≤ 0.
 */
public class TabularIslamicCalendar private constructor(
    /** Which tabular variant this is. */
    public val variant: IslamicVariant,
    leapYearsInCycle: Set<Int>,
) : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    private val isLeapInCycle = BooleanArray(YEARS_PER_CYCLE + 1) { it in leapYearsInCycle }

    /** `leapYearsBefore[k]` = leap years among cycle years `1..k`, for `k` in `0..30`. */
    private val leapYearsBefore =
        IntArray(YEARS_PER_CYCLE + 1).also { counts ->
            for (k in 1..YEARS_PER_CYCLE) counts[k] = counts[k - 1] + if (isLeapInCycle[k]) 1 else 0
        }

    init {
        require(leapYearsInCycle.size == LEAP_YEARS_PER_CYCLE && leapYearsInCycle.all { it in 1..YEARS_PER_CYCLE })
    }

    override fun isLeapYear(year: Int): Boolean = isLeapInCycle[cycleYear(year)]

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Islamic month must be in 1..12 (was $month)" }
        return when {
            month == MONTHS && isLeapYear(year) -> LONG_MONTH
            month % 2 == 1 -> LONG_MONTH
            else -> SHORT_MONTH
        }
    }

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected an ISLAMIC date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) {
            "Tabular Islamic ($variant) date ${date.toIsoLikeString()} does not exist"
        }
        val monthOffset = SHORT_MONTH * (date.month - 1) + date.month / 2
        return Jdn(EPOCH_JDN + daysBeforeYear(date.year) + monthOffset + date.day - 1)
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        val elapsed = jdn.value - EPOCH_JDN
        val cycles = Math.floorDiv(elapsed, DAYS_PER_CYCLE)
        var remaining = elapsed - cycles * DAYS_PER_CYCLE
        var yearInCycle = 1
        while (remaining >= yearLength(yearInCycle)) {
            remaining -= yearLength(yearInCycle)
            yearInCycle++
        }
        val yearsBeforeCycle = Math.multiplyExact(cycles, YEARS_PER_CYCLE.toLong())
        val year = Math.toIntExact(Math.addExact(yearsBeforeCycle, yearInCycle.toLong()))
        var month = 1
        while (remaining >= monthLength(year, month)) {
            remaining -= monthLength(year, month)
            month++
        }
        return CalendarDate(system, year, month, remaining.toInt() + 1)
    }

    /** Days from the epoch to 1 Muharram of [year]. */
    private fun daysBeforeYear(year: Int): Long {
        val elapsedYears = year - 1L
        val cycles = Math.floorDiv(elapsedYears, YEARS_PER_CYCLE.toLong())
        val yearsIntoCycle = Math.floorMod(elapsedYears, YEARS_PER_CYCLE.toLong()).toInt()
        return COMMON_YEAR_DAYS * elapsedYears + LEAP_YEARS_PER_CYCLE * cycles + leapYearsBefore[yearsIntoCycle]
    }

    private fun yearLength(cycleYear: Int): Long = COMMON_YEAR_DAYS + if (isLeapInCycle[cycleYear]) 1 else 0

    private fun cycleYear(year: Int): Int = Math.floorMod(year - 1L, YEARS_PER_CYCLE.toLong()).toInt() + 1

    public companion object {
        /** JDN of 1 Muharram 1 AH (civil epoch, Friday 16 July 622 Julian). */
        public const val EPOCH_JDN: Long = 1_948_440L

        /** Years in a tabular cycle. */
        public const val YEARS_PER_CYCLE: Int = 30

        /** Days in a tabular cycle: 30 × 354 + 11. */
        public const val DAYS_PER_CYCLE: Long = 10_631L

        private const val MONTHS = 12
        private const val LONG_MONTH = 30
        private const val SHORT_MONTH = 29
        private const val COMMON_YEAR_DAYS = 354L
        private const val LEAP_YEARS_PER_CYCLE = 11

        /** Type II ("16"): leap years 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29 — the civil calendar used by ICU. */
        public val TYPE_II: TabularIslamicCalendar =
            TabularIslamicCalendar(IslamicVariant.TABULAR_16, setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29))

        /** Type I ("15"): leap years 2, 5, 7, 10, 13, 15, 18, 21, 24, 26, 29. */
        public val TYPE_I: TabularIslamicCalendar =
            TabularIslamicCalendar(IslamicVariant.TABULAR_15, setOf(2, 5, 7, 10, 13, 15, 18, 21, 24, 26, 29))
    }
}
