/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Saudi Umm al-Qura calendar (A-04).
 *
 * Month lengths for AH [FIRST_YEAR]‥[LAST_YEAR] are ICU4J 78.3 Umm al-Qura data (Unicode License v3; notice in
 * `licenses/ICU-LICENSE.txt`), stored as one 12-bit mask per year and verified month-by-month against ICU4J in
 * `UmmAlQuraCalendarTest`. Outside that range the civil tabular calendar ([TabularIslamicCalendar.TYPE_II]) is
 * used, exactly as ICU4J does; both table boundaries coincide with the civil calendar, so there is no gap or
 * overlap. See docs/adr/0006-umm-al-qura-table.md for why the table is embedded instead of calling ICU4J.
 */
public object UmmAlQuraCalendar : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    /** First year covered by the Umm al-Qura table. */
    public const val FIRST_YEAR: Int = 1300

    /** Last year covered by the Umm al-Qura table. */
    public const val LAST_YEAR: Int = 1600

    /** JDN of 1 Muharram [FIRST_YEAR]. */
    public const val TABLE_START_JDN: Long = 2_408_762L

    /** JDN of 1 Muharram [LAST_YEAR] + 1, the first day after the table. */
    public const val TABLE_END_JDN: Long = 2_515_427L

    private const val MONTHS = 12
    private const val LONG_MONTH = 30
    private const val SHORT_MONTH = 29
    private const val COMMON_YEAR_DAYS = 354

    private val fallback = TabularIslamicCalendar.TYPE_II

    private val MONTH_MASKS: List<Int> = UMM_AL_QURA_MONTH_MASKS

    /** JDN of 1 Muharram for every table year, followed by [TABLE_END_JDN]. */
    private val YEAR_STARTS: List<Long> =
        MONTH_MASKS.runningFold(TABLE_START_JDN) { start, mask -> start + yearLength(mask) }

    /** `true` when [year] lies in the embedded table (otherwise the civil tabular calendar applies). */
    public fun isTabulated(year: Int): Boolean = year in FIRST_YEAR..LAST_YEAR

    /** Umm al-Qura years have 353‥356 days; a year is reported as "leap" when it is longer than 354 days. */
    override fun isLeapYear(year: Int): Boolean =
        if (isTabulated(year)) {
            yearLength(MONTH_MASKS[year - FIRST_YEAR]) > COMMON_YEAR_DAYS
        } else {
            fallback.isLeapYear(year)
        }

    override fun monthsInYear(year: Int): Int = MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..MONTHS) { "Islamic month must be in 1..12 (was $month)" }
        return if (isTabulated(year)) {
            tableMonthLength(MONTH_MASKS[year - FIRST_YEAR], month)
        } else {
            fallback.monthLength(year, month)
        }
    }

    override fun toJdn(date: CalendarDate): Jdn {
        require(date.system == system) { "Expected an ISLAMIC date (was ${date.system})" }
        if (!isTabulated(date.year)) return fallback.toJdn(date)
        require(isValid(date.year, date.month, date.day)) {
            "Umm al-Qura date ${date.toIsoLikeString()} does not exist"
        }
        val mask = MONTH_MASKS[date.year - FIRST_YEAR]
        val daysBeforeMonth = (1 until date.month).sumOf { tableMonthLength(mask, it) }
        return Jdn(YEAR_STARTS[date.year - FIRST_YEAR] + daysBeforeMonth + date.day - 1)
    }

    override fun fromJdn(jdn: Jdn): CalendarDate {
        if (jdn.value !in TABLE_START_JDN until TABLE_END_JDN) return fallback.fromJdn(jdn)
        val index = YEAR_STARTS.binarySearch(jdn.value).let { if (it >= 0) it else -it - 2 }
        val mask = MONTH_MASKS[index]
        var remaining = (jdn.value - YEAR_STARTS[index]).toInt()
        var month = 1
        while (remaining >= tableMonthLength(mask, month)) {
            remaining -= tableMonthLength(mask, month)
            month++
        }
        return CalendarDate(system, FIRST_YEAR + index, month, remaining + 1)
    }

    private fun tableMonthLength(
        mask: Int,
        month: Int,
    ): Int = if (mask shr (MONTHS - month) and 1 == 1) LONG_MONTH else SHORT_MONTH

    private fun yearLength(mask: Int): Int = COMMON_YEAR_DAYS - (MONTHS / 2) + Integer.bitCount(mask)
}
