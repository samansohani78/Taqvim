/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/**
 * Lunar Hijri arithmetic over month starts: [startJdn] gives the JDN of the first day of each month index (see
 * [IranCrescentMonths.monthIndex]); month lengths are the differences between consecutive starts. Every [Int] year is
 * supported; days outside them are rejected.
 */
internal class LunarMonthArithmetic(
    private val startJdn: (Long) -> Long,
) {
    private val firstIndex = IranCrescentMonths.monthIndex(Int.MIN_VALUE.toLong(), 1)
    private val endIndex = IranCrescentMonths.monthIndex(Int.MAX_VALUE + 1L, 1)

    fun monthLength(
        year: Int,
        month: Int,
    ): Int {
        require(month in 1..IranCrescentMonths.MONTHS) { "Islamic month must be in 1..12 (was $month)" }
        val index = IranCrescentMonths.monthIndex(year.toLong(), month)
        return (startJdn(index + 1) - startJdn(index)).toInt()
    }

    fun yearLength(year: Int): Long {
        val first = IranCrescentMonths.monthIndex(year.toLong(), 1)
        return startJdn(first + IranCrescentMonths.MONTHS) - startJdn(first)
    }

    fun toJdn(date: CalendarDate): Jdn {
        require(date.system == CalendarSystem.ISLAMIC) { "Expected an ISLAMIC date (was ${date.system})" }
        val validMonth = date.month in 1..IranCrescentMonths.MONTHS
        require(validMonth && date.day in 1..monthLength(date.year, date.month)) {
            "Islamic date ${date.toIsoLikeString()} does not exist"
        }
        return Jdn(startJdn(IranCrescentMonths.monthIndex(date.year.toLong(), date.month)) + date.day - 1)
    }

    fun fromJdn(jdn: Jdn): CalendarDate {
        val day = jdn.value
        require(day >= startJdn(firstIndex) && day < startJdn(endIndex)) {
            "JDN $day is outside the Hijri years ${Int.MIN_VALUE}..${Int.MAX_VALUE}"
        }
        var index = IranCrescentMonths.estimateIndex(day).coerceIn(firstIndex, endIndex - 1)
        while (startJdn(index) > day) index--
        while (startJdn(index + 1) <= day) index++
        val year = IranCrescentMonths.yearOf(index).toInt()
        val dayOfMonth = (day - startJdn(index)).toInt() + 1
        return CalendarDate(CalendarSystem.ISLAMIC, year, IranCrescentMonths.monthOf(index), dayOfMonth)
    }
}
