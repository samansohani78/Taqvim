/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Solar Hijri (Persian) calendar (A-02): months of 31 × 6, 30 × 5 and 29/30 days.
 *
 * Year starts follow the astronomical rule of [PersianYearStartRule]. For [TABLE_FIRST_YEAR]‥[TABLE_LAST_YEAR] they come
 * from [PersianLeapTable], generated from that rule and checked against the Calendar Center's official leap years;
 * outside the table the 2820-year arithmetic cycle ([BirashkArithmetic]) continues from the table edges, so every year
 * still has 365 or 366 days. See docs/adr/0008-persian-year-start-table.md.
 */
public object PersianCalendarSystem : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.PERSIAN

    /** First year taken from the astronomical table. */
    public const val TABLE_FIRST_YEAR: Int = PersianLeapTable.FIRST_YEAR

    /** Last year taken from the astronomical table. */
    public const val TABLE_LAST_YEAR: Int = PersianLeapTable.LAST_YEAR

    private const val MONTHS = 12
    private const val LONG_MONTHS = 6
    private const val LONG_MONTH_DAYS = 31
    private const val SHORT_MONTH_DAYS = 30
    private const val COMMON_ESFAND_DAYS = 29
    private const val COMMON_YEAR_DAYS = 365
    private const val DAYS_IN_LONG_MONTHS = LONG_MONTHS * LONG_MONTH_DAYS

    /** Days from 1 Farvardin [TABLE_FIRST_YEAR] to 1 Farvardin of each table year, plus the year after the table. */
    private val TABLE_YEAR_OFFSETS: IntArray =
        (TABLE_FIRST_YEAR..TABLE_LAST_YEAR)
            .runningFold(0) { offset, year ->
                offset + COMMON_YEAR_DAYS + if (PersianLeapTable.isLeap(year)) 1 else 0
            }.toIntArray()

    private const val TABLE_START_JDN = PersianLeapTable.FIRST_YEAR_START_JDN
    private val TABLE_END_JDN: Long = TABLE_START_JDN + TABLE_YEAR_OFFSETS.last()

    override fun isLeapYear(year: Int): Boolean =
        if (year in TABLE_FIRST_YEAR..TABLE_LAST_YEAR) {
            val index = year - TABLE_FIRST_YEAR
            TABLE_YEAR_OFFSETS[index + 1] - TABLE_YEAR_OFFSETS[index] > COMMON_YEAR_DAYS
        } else {
            BirashkArithmetic.isLeapYear(year)
        }

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Persian month must be in 1..12 (was $month)" }
        return when {
            month <= LONG_MONTHS -> LONG_MONTH_DAYS
            month < MONTHS || isLeapYear(year) -> SHORT_MONTH_DAYS
            else -> COMMON_ESFAND_DAYS
        }
    }

    /** JDN of 1 Farvardin [year]. */
    public fun firstDayOfYear(year: Int): Jdn = Jdn(yearStartJdn(year))

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected a PERSIAN date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) { "Persian date ${date.toIsoLikeString()} does not exist" }
        return Jdn(yearStartJdn(date.year) + daysBeforeMonth(date.month) + date.day - 1)
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        val year = yearContaining(jdn.value)
        val dayOfYear = (jdn.value - yearStartJdn(year)).toInt()
        val month =
            if (dayOfYear < DAYS_IN_LONG_MONTHS) {
                dayOfYear / LONG_MONTH_DAYS + 1
            } else {
                (dayOfYear - DAYS_IN_LONG_MONTHS) / SHORT_MONTH_DAYS + LONG_MONTHS + 1
            }
        return CalendarDate(system, year, month, dayOfYear - daysBeforeMonth(month) + 1)
    }

    private fun daysBeforeMonth(month: Int): Int =
        if (month <= LONG_MONTHS) {
            (month - 1) * LONG_MONTH_DAYS
        } else {
            DAYS_IN_LONG_MONTHS + (month - LONG_MONTHS - 1) * SHORT_MONTH_DAYS
        }

    private fun yearStartJdn(year: Int): Long =
        when {
            year < TABLE_FIRST_YEAR -> {
                TABLE_START_JDN - COMMON_YEAR_DAYS * (TABLE_FIRST_YEAR.toLong() - year) -
                    BirashkArithmetic.leapYearsBetween(year, TABLE_FIRST_YEAR)
            }

            year > TABLE_LAST_YEAR -> {
                TABLE_END_JDN + COMMON_YEAR_DAYS * (year.toLong() - TABLE_LAST_YEAR - 1) +
                    BirashkArithmetic.leapYearsBetween(TABLE_LAST_YEAR + 1, year)
            }

            else -> {
                TABLE_START_JDN + TABLE_YEAR_OFFSETS[year - TABLE_FIRST_YEAR]
            }
        }

    private fun yearContaining(jdn: Long): Int {
        if (jdn < TABLE_START_JDN || jdn >= TABLE_END_JDN) return fallbackYearContaining(jdn)
        val index = TABLE_YEAR_OFFSETS.binarySearch((jdn - TABLE_START_JDN).toInt())
        return TABLE_FIRST_YEAR + if (index >= 0) index else -index - 2
    }

    /** Estimates the year from the mean cycle year, then corrects by whole years (at most a step or two). */
    private fun fallbackYearContaining(jdn: Long): Int {
        val anchor = if (jdn < TABLE_START_JDN) TABLE_FIRST_YEAR else TABLE_LAST_YEAR + 1
        val elapsedYears = Math.floor((jdn - yearStartJdn(anchor)) / BirashkArithmetic.MEAN_YEAR_DAYS).toLong()
        var year = Math.toIntExact(anchor + elapsedYears)
        while (yearStartJdn(year) > jdn) year--
        while (yearStartJdn(year + 1) <= jdn) year++
        return year
    }
}
