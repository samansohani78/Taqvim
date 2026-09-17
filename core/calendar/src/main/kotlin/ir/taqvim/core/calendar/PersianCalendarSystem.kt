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
 * 1 Farvardin follows [PersianYearStartRule] for every [Int] year, computed at run time: from the true March equinox
 * for [ASTRONOMICAL_FIRST_YEAR]‥[ASTRONOMICAL_LAST_YEAR], and from the mean March equinox continued from those edges
 * beyond them. Official data never changes a year: the Calendar Center's leap years (1206–1498), its 1404/1405 calendars
 * and Nowruz instants are golden test oracles only (ADR-0026 addendum, 2026-09-17), and they agree with the rule.
 * See docs/adr/0026-persian-year-starts-computed.md.
 */
public object PersianCalendarSystem : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.PERSIAN

    /** First year started from the true March equinox; earlier years use the mean equinox. */
    public const val ASTRONOMICAL_FIRST_YEAR: Int = PersianYearStarts.FIRST_ASTRONOMICAL_YEAR

    /** Last year started from the true March equinox; later years use the mean equinox. */
    public const val ASTRONOMICAL_LAST_YEAR: Int = PersianYearStarts.LAST_ASTRONOMICAL_YEAR

    private const val MONTHS = 12
    private const val LONG_MONTHS = 6
    private const val LONG_MONTH_DAYS = 31
    private const val SHORT_MONTH_DAYS = 30
    private const val COMMON_ESFAND_DAYS = 29
    private const val COMMON_YEAR_DAYS = 365
    private const val DAYS_IN_LONG_MONTHS = LONG_MONTHS * LONG_MONTH_DAYS

    override fun isLeapYear(year: Int): Boolean =
        PersianYearStarts.startJdn(year + 1L) - PersianYearStarts.startJdn(year.toLong()) > COMMON_YEAR_DAYS

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
    public fun firstDayOfYear(year: Int): Jdn = Jdn(PersianYearStarts.startJdn(year.toLong()))

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected a PERSIAN date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) { "Persian date ${date.toIsoLikeString()} does not exist" }
        return Jdn(PersianYearStarts.startJdn(date.year.toLong()) + daysBeforeMonth(date.month) + date.day - 1)
    }

    /** The Persian date of [jdn]; throws [IllegalArgumentException] for days outside the years of [Int]. */
    override fun fromJdn(jdn: Jdn): CalendarDate {
        val year = PersianYearStarts.yearContaining(jdn.value)
        val dayOfYear = (jdn.value - PersianYearStarts.startJdn(year.toLong())).toInt()
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
}
