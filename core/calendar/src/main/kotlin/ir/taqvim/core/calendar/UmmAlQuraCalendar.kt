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
 * Computed at run time for every year except AH [BUNDLED_FIRST_YEAR]‥[BUNDLED_LAST_YEAR]: from AH 1423 by the
 * calendar's astronomical criterion at the Kaʿba ([UmmAlQuraCriterion]), in AH 1420–1422 by that period's moonset
 * rule, before the bundled years by the criterion applied proleptically, and far from the present by mean lunar months
 * continued from the astronomical ones ([UmmAlQuraMonths]). Every year of [Int] is supported.
 *
 * The bundled years follow the printed calendar (ICU4J 78.3 data, Unicode License v3; notice in
 * `licenses/ICU-LICENSE.txt`) because no reproducible rule gives them; they are finite, fixed historical dates.
 */
public object UmmAlQuraCalendar : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    /** First year taken from the bundled printed calendar. */
    public const val BUNDLED_FIRST_YEAR: Int = UmmAlQuraMonths.BUNDLED_FIRST_YEAR

    /** Last year taken from the bundled printed calendar; every later year is computed. */
    public const val BUNDLED_LAST_YEAR: Int = UmmAlQuraMonths.BUNDLED_LAST_YEAR

    private const val MONTHS = 12
    private const val COMMON_YEAR_DAYS = 354

    /** `true` when [year] comes from the bundled printed calendar rather than a computed rule. */
    public fun isBundled(year: Int): Boolean = year in BUNDLED_FIRST_YEAR..BUNDLED_LAST_YEAR

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
