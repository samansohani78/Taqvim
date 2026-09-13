/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday

/** Grid arithmetic of a month page (T-801): six whole weeks from the week start on or before the first of the month. */
object MonthLayout {
    const val DAYS_PER_WEEK: Int = 7
    const val WEEKS: Int = 6
    const val CELLS: Int = DAYS_PER_WEEK * WEEKS

    /** The first grid day of the month beginning on [monthStart]: the last [weekStart] on or before it. */
    fun gridStart(
        monthStart: Jdn,
        weekStart: Weekday,
    ): Jdn = monthStart - monthStart.weekday().daysAfter(weekStart)

    /** The [CELLS] days of the grid of the month beginning on [monthStart]. */
    fun gridDays(
        monthStart: Jdn,
        weekStart: Weekday,
    ): JdnRange {
        val first = gridStart(monthStart, weekStart)
        return first..(first + (CELLS - 1))
    }

    /**
     * Week of the year of [day] in [calendar]: week 1 contains the first day of the year, and every following week
     * begins on [weekStart].
     */
    fun weekOfYear(
        day: Jdn,
        calendar: CalendarArithmetic,
        weekStart: Weekday,
    ): Int {
        val yearStart = calendar.toJdn(calendar.date(calendar.fromJdn(day).year, 1, 1))
        val leadingDays = yearStart.weekday().daysAfter(weekStart)
        return ((day - yearStart).toInt() + leadingDays) / DAYS_PER_WEEK + 1
    }
}
