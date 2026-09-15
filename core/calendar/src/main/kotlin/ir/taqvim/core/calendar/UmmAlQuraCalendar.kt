/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Saudi Umm al-Qura calendar (A-04) for every year (ADR-0028).
 *
 * AH [PUBLISHED_FIRST_YEAR]‥[PUBLISHED_LAST_YEAR] follow the published calendar (ICU4J 78.3 data, Unicode License v3;
 * notice in `licenses/ICU-LICENSE.txt`). Every other year is computed from the calendar's astronomical criterion at the
 * Kaʿba ([UmmAlQuraCriterion]) — proleptically before the published years — and, far from the present, from mean lunar
 * months continued from the astronomical ones ([UmmAlQuraMonths]). Every year of [Int] is supported.
 */
public object UmmAlQuraCalendar : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    /** First year of the published calendar. */
    public const val PUBLISHED_FIRST_YEAR: Int = UmmAlQuraMonths.PUBLISHED_FIRST_YEAR

    /** Last year of the published calendar. */
    public const val PUBLISHED_LAST_YEAR: Int = UmmAlQuraMonths.PUBLISHED_LAST_YEAR

    private const val MONTHS = 12
    private const val COMMON_YEAR_DAYS = 354

    /** `true` when [year] follows the published calendar rather than the computed criterion. */
    public fun isPublished(year: Int): Boolean = year in PUBLISHED_FIRST_YEAR..PUBLISHED_LAST_YEAR

    /** Umm al-Qura years have 354 or 355 days (rarely 353 or 356); a year is "leap" when it is longer than 354. */
    override fun isLeapYear(year: Int): Boolean {
        val start = UmmAlQuraMonths.index(year.toLong(), 1)
        return UmmAlQuraMonths.start(start + MONTHS) - UmmAlQuraMonths.start(start) > COMMON_YEAR_DAYS
    }

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Islamic month must be in 1..12 (was $month)" }
        val index = UmmAlQuraMonths.index(year.toLong(), month)
        return (UmmAlQuraMonths.start(index + 1) - UmmAlQuraMonths.start(index)).toInt()
    }

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected an ISLAMIC date (was ${date.system})" }
        require(isValid(date.year, date.month, date.day)) {
            "Umm al-Qura date ${date.toIsoLikeString()} does not exist"
        }
        return Jdn(UmmAlQuraMonths.start(UmmAlQuraMonths.index(date.year.toLong(), date.month)) + date.day - 1)
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        val index = UmmAlQuraMonths.indexContaining(jdn.value)
        val day = jdn.value - UmmAlQuraMonths.start(index) + 1
        return CalendarDate(system, UmmAlQuraMonths.yearOf(index).toInt(), UmmAlQuraMonths.monthOf(index), day.toInt())
    }
}
