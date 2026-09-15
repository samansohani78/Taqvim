/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * The calculated-observational lunar Hijri calendar with the Iran calibration (A-06, ADR-0027), without official data:
 * each month starts the day after the first evening on which the crescent passes Yallop's test at class D or better at
 * any of five cities across Iran, computed for AH −3000…3000 and continued with the mean month beyond (see
 * [IranCrescentMonths]). Every month has 29 or 30 days and every [Int] year is supported.
 *
 * [IranIslamicCalendar] uses the same months wherever the Calendar Center has published none.
 */
public object IranCrescentCalendar : CalendarArithmetic {
    override val system: CalendarSystem = CalendarSystem.ISLAMIC

    private val arithmetic = LunarMonthArithmetic(IranCrescentMonths::startJdn)

    override fun isLeapYear(year: Int): Boolean = arithmetic.yearLength(year) > COMMON_YEAR_DAYS

    override fun monthsInYear(year: Int): Int = IranCrescentMonths.MONTHS

    override fun monthLength(
        year: Int,
        month: Int,
    ): Int = arithmetic.monthLength(year, month)

    override fun toJdn(date: CalendarDate): Jdn = arithmetic.toJdn(date)

    override fun fromJdn(jdn: Jdn): CalendarDate = arithmetic.fromJdn(jdn)

    private const val COMMON_YEAR_DAYS = 354L
}
