/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.Test

/** T-801: month grids and week numbers. */
class MonthLayoutTest {
    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)

    @Test
    fun `grids begin on the week start and hold the whole month in six weeks`() {
        val calendars = CalendarCalendars(PERSIAN_FIRST)
        Weekday.entries.forEach { weekStart ->
            (-24..24).forEach { offset ->
                val monthStart = calendars.monthStartAt(today, offset)
                val date = PersianCalendarSystem.fromJdn(monthStart)
                val monthEnd = monthStart + (PersianCalendarSystem.monthLength(date.year, date.month) - 1)
                val grid = MonthLayout.gridDays(monthStart, weekStart)

                grid.start.weekday() shouldBe weekStart
                (monthStart - grid.start in 0L..6L) shouldBe true
                grid.dayCount shouldBe MonthLayout.CELLS.toLong()
                (monthEnd <= grid.endInclusive) shouldBe true
            }
        }
    }

    @Test
    fun `week one holds the first day of the year`() {
        val saturday = Weekday.SATURDAY
        // 1 Farvardin 1405 (2026-03-21) is a Saturday.
        MonthLayout.weekOfYear(gregorian(2026, 3, 21), PersianCalendarSystem, saturday) shouldBe 1
        MonthLayout.weekOfYear(gregorian(2026, 3, 27), PersianCalendarSystem, saturday) shouldBe 1
        MonthLayout.weekOfYear(gregorian(2026, 3, 28), PersianCalendarSystem, saturday) shouldBe 2
        MonthLayout.weekOfYear(gregorian(2026, 3, 22), PersianCalendarSystem, Weekday.SUNDAY) shouldBe 2
        // 29 Esfand 1404, the last day of a 365-day year that began on a Friday.
        MonthLayout.weekOfYear(gregorian(2026, 3, 20), PersianCalendarSystem, saturday) shouldBe 53
    }
}
