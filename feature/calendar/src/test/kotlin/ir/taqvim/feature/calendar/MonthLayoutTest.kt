/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.HebrewCalendarSystem
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.WeekOfYear
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
        MonthLayout.weekOfYear(gregorian(2026, 3, 21), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1405, 1)
        MonthLayout.weekOfYear(gregorian(2026, 3, 27), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1405, 1)
        MonthLayout.weekOfYear(gregorian(2026, 3, 28), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1405, 2)
        MonthLayout.weekOfYear(gregorian(2026, 3, 22), PersianCalendarSystem, Weekday.SUNDAY) shouldBe
            WeekOfYear(1405, 2)
        // 29 Esfand 1404, the last day of a 365-day year that began on a Friday: week 1 of 1405 starts the next day,
        // so this day is still the 53rd week of 1404.
        MonthLayout.weekOfYear(gregorian(2026, 3, 20), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1404, 53)
    }

    @Test
    fun `the last days of a year can belong to week one of the next`() {
        // A-08's boundary rule, and the case the grid used to get wrong. 1 Farvardin 1406 is 2027-03-21, a Sunday,
        // so with a Saturday week start week 1 of 1406 opens on Saturday 2027-03-20 — which is 29 Esfand 1405.
        // The grid counted that day from Nowruz 1405 and called it week 53; the rule calls it week 1 of 1406, and
        // PROVENANCE A-08 says so in as many words.
        val saturday = Weekday.SATURDAY
        MonthLayout.weekOfYear(gregorian(2027, 3, 20), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1406, 1)
        // 1405 began on a Saturday, so its weeks line up with its days: 2027-03-19 is day 364, the last of week 52.
        MonthLayout.weekOfYear(gregorian(2027, 3, 19), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1405, 52)
        MonthLayout.weekOfYear(gregorian(2027, 3, 21), PersianCalendarSystem, saturday) shouldBe WeekOfYear(1406, 1)

        // The same thing in Gregorian with a Monday start: 2025-01-01 is a Wednesday, so week 1 of 2025 opens on
        // Monday 2024-12-30. (ISO agrees here, but by its own four-day rule, not this one.)
        MonthLayout.weekOfYear(gregorian(2024, 12, 30), GregorianCalendarSystem, Weekday.MONDAY) shouldBe
            WeekOfYear(2025, 1)
        MonthLayout.weekOfYear(gregorian(2024, 12, 29), GregorianCalendarSystem, Weekday.MONDAY) shouldBe
            WeekOfYear(2024, 52)
    }

    @Test
    fun `every supported year of every calendar has a week number, including its edges`() {
        // The rule looks at the year before and the year after the date's own, so the first and last supported years
        // ask the calendar for a year outside the supported range. Each of these threw nothing and must not start to.
        val calendars =
            listOf(
                GregorianCalendarSystem,
                PersianCalendarSystem,
                NepaliCalendarSystem,
                HebrewCalendarSystem,
            )
        for (calendar in calendars) {
            val years = CalendarLimits.years(calendar)
            for (year in listOf(years.first, years.first + 1, years.last - 1, years.last)) {
                val lastMonth = calendar.monthsInYear(year)
                val firstDay = calendar.toJdn(calendar.date(year, 1, 1))
                val lastDay = calendar.toJdn(calendar.date(year, lastMonth, calendar.monthLength(year, lastMonth)))

                val first = MonthLayout.weekOfYear(firstDay, calendar, Weekday.SATURDAY)
                val last = MonthLayout.weekOfYear(lastDay, calendar, Weekday.SATURDAY)

                first shouldBe WeekOfYear(year, 1)
                (last.week in 1..MAX_WEEKS) shouldBe true
                (last.weekBasedYear in year..(year + 1)) shouldBe true
            }
        }
    }

    private companion object {
        /** A lunar or 13-month year can hold more than 53 weeks of 7 days; nothing here should exceed this. */
        const val MAX_WEEKS = 56
    }
}
