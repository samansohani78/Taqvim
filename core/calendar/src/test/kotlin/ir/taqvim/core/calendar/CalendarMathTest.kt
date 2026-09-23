/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Table-driven tests for T-106 utilities on the Gregorian and Islamic calendars. Persian rows (29/30 Esfand) are in
 * [PersianCalendarMathTest]; Nepali rows (32-day months) and Hebrew 13-month years are in [NepaliCalendarMathTest].
 */
class CalendarMathTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val gregorian = GregorianCalendarSystem
    private val islamic = TabularIslamicCalendar.TYPE_II

    private fun g(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.GREGORIAN, year, month, day)

    private fun h(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    private fun rows(vararg rows: Pair<String, () -> Unit>) =
        rows.map { (name, check) -> DynamicTest.dynamicTest(name, check) }

    private fun gregorianMonthLength(
        year: Int,
        month: Int,
    ): Int = if (month == 2) (if (gregorian.isLeapYear(year)) 29 else 28) else GREGORIAN_MONTHS[month - 1]

    private fun nth(
        month: Int,
        weekday: Weekday,
        n: Int,
    ) = gregorian.nthWeekdayOfMonth(2026, month, weekday, n)

    private fun lastInSeptember(
        weekday: Weekday,
        offsetDays: Int = 0,
    ) = gregorian.lastWeekdayOfMonth(2026, 9, weekday, offsetDays)

    @TestFactory
    fun `month and year lengths`() =
        (1..12).flatMap { month ->
            listOf(2023, 2024, 1900, 2000).map { year ->
                val expected = gregorianMonthLength(year, month)
                DynamicTest.dynamicTest("Gregorian $year-$month has $expected days") {
                    gregorian.monthLength(year, month) shouldBe expected
                }
            }
        } +
            (1..30).map { year ->
                val expected = if (islamic.isLeapYear(year)) 30 else 29
                DynamicTest.dynamicTest("Dhul-Hijjah of AH $year has $expected days") {
                    islamic.monthLength(year, 12) shouldBe expected
                }
            } +
            rows(
                "Gregorian 2024 has 366 days" to { gregorian.yearLength(2024) shouldBe 366 },
                "Gregorian 2100 has 365 days" to { gregorian.yearLength(2100) shouldBe 365 },
                "AH 1447 is a leap year of 355 days" to { islamic.yearLength(1447) shouldBe 355 },
                "AH 1446 is a common year of 354 days" to { islamic.yearLength(1446) shouldBe 354 },
            )

    @TestFactory
    fun `day of year`() =
        rows(
            "2024-01-01" to { gregorian.dayOfYear(g(2024, 1, 1)) shouldBe 1 },
            "2024-12-31" to { gregorian.dayOfYear(g(2024, 12, 31)) shouldBe 366 },
            "2023-12-31" to { gregorian.dayOfYear(g(2023, 12, 31)) shouldBe 365 },
            "2023-03-01" to { gregorian.dayOfYear(g(2023, 3, 1)) shouldBe 60 },
            "2024-03-01" to { gregorian.dayOfYear(g(2024, 3, 1)) shouldBe 61 },
            "2026-09-13 is day 256" to { gregorian.dayOfYear(g(2026, 9, 13)) shouldBe 256 },
            "AH 1447-02-01" to { islamic.dayOfYear(h(1447, 2, 1)) shouldBe 31 },
            "AH 1447-12-29" to { islamic.dayOfYear(h(1447, 12, 29)) shouldBe 354 },
        )

    @TestFactory
    fun `adding months clamps the day`() =
        rows(
            "Jan 31 + 1 month (leap)" to { gregorian.addMonths(g(2024, 1, 31), 1) shouldBe g(2024, 2, 29) },
            "Jan 31 + 1 month" to { gregorian.addMonths(g(2023, 1, 31), 1) shouldBe g(2023, 2, 28) },
            "Mar 31 − 1 month" to { gregorian.addMonths(g(2023, 3, 31), -1) shouldBe g(2023, 2, 28) },
            "Dec 15 + 1 month crosses the year" to { gregorian.addMonths(g(2023, 12, 15), 1) shouldBe g(2024, 1, 15) },
            "Jan 15 − 13 months" to { gregorian.addMonths(g(2024, 1, 15), -13) shouldBe g(2022, 12, 15) },
            "+ 0 months" to { gregorian.addMonths(g(2024, 5, 5), 0) shouldBe g(2024, 5, 5) },
            "+ 120 months" to { gregorian.addMonths(g(2000, 2, 29), 120) shouldBe g(2010, 2, 28) },
            "AH Muharram 30 + 1 month" to { islamic.addMonths(h(1447, 1, 30), 1) shouldBe h(1447, 2, 29) },
            "AH Dhul-Qadah 30 + 1 month (common year)" to {
                islamic.addMonths(h(1446, 11, 30), 1) shouldBe h(1446, 12, 29)
            },
        )

    @TestFactory
    fun `months and periods between dates`() =
        rows(
            "same day" to { gregorian.monthsBetween(g(2024, 1, 31), g(2024, 1, 31)) shouldBe 0 },
            "Jan 31 → Feb 29" to { gregorian.monthsBetween(g(2024, 1, 31), g(2024, 2, 29)) shouldBe 1 },
            "Jan 31 → Feb 28 (leap)" to { gregorian.monthsBetween(g(2024, 1, 31), g(2024, 2, 28)) shouldBe 0 },
            "backwards" to { gregorian.monthsBetween(g(2024, 5, 1), g(2023, 5, 1)) shouldBe -12 },
            "period 2000-02-29 → 2026-09-13" to {
                gregorian.periodBetween(g(2000, 2, 29), g(2026, 9, 13)) shouldBe DatePeriod(26, 6, 15)
            },
            "negative period" to {
                gregorian.periodBetween(g(2024, 3, 10), g(2024, 1, 5)) shouldBe DatePeriod(0, -2, -5)
            },
            "days between" to { gregorian.daysBetween(g(2024, 1, 1), g(2025, 1, 1)) shouldBe 366L },
            "plus days" to { gregorian.plusDays(g(2024, 2, 28), 2) shouldBe g(2024, 3, 1) },
        )

    @TestFactory
    fun `nth and last weekday of a month`() =
        rows(
            "4th Thursday of Nov 2026" to { nth(11, Weekday.THURSDAY, 4) shouldBe g(2026, 11, 26) },
            "1st Monday of Sep 2026" to { nth(9, Weekday.MONDAY, 1) shouldBe g(2026, 9, 7) },
            "1st Tuesday when the month starts on Tuesday" to { nth(9, Weekday.TUESDAY, 1) shouldBe g(2026, 9, 1) },
            "5th Friday of Feb 2026 does not exist" to { nth(2, Weekday.FRIDAY, 5) shouldBe null },
            "5th Wednesday of Sep 2026" to { nth(9, Weekday.WEDNESDAY, 5) shouldBe g(2026, 9, 30) },
            "last Friday of Sep 2026" to { lastInSeptember(Weekday.FRIDAY) shouldBe g(2026, 9, 25) },
            "last Wednesday of Sep 2026 is the 30th" to { lastInSeptember(Weekday.WEDNESDAY) shouldBe g(2026, 9, 30) },
            "last Friday + 6 days leaves the month" to { lastInSeptember(Weekday.FRIDAY, 6) shouldBe g(2026, 10, 1) },
            "last Friday − 1 day" to { lastInSeptember(Weekday.FRIDAY, -1) shouldBe g(2026, 9, 24) },
            "n must be positive" to { shouldThrow<IllegalArgumentException> { nth(9, Weekday.FRIDAY, 0) } },
        )

    @TestFactory
    fun `ISO and first-day week numbering`() =
        listOf(
            g(2020, 12, 31) to WeekOfYear(2020, 53),
            g(2021, 1, 1) to WeekOfYear(2020, 53),
            g(2021, 1, 3) to WeekOfYear(2020, 53),
            g(2021, 1, 4) to WeekOfYear(2021, 1),
            g(2024, 12, 30) to WeekOfYear(2025, 1),
            g(2026, 1, 1) to WeekOfYear(2026, 1),
            g(2026, 9, 13) to WeekOfYear(2026, 37),
            g(2026, 12, 31) to WeekOfYear(2026, 53),
            g(2027, 1, 3) to WeekOfYear(2026, 53),
            g(2027, 1, 4) to WeekOfYear(2027, 1),
        ).map { (date, expected) ->
            DynamicTest.dynamicTest("ISO week of $date") { gregorian.weekOfYear(date, WeekRule.ISO) shouldBe expected }
        } +
            rows(
                "Saturday-start, week 1 holds Jan 1" to {
                    val rule = WeekRule.containingFirstDay(Weekday.SATURDAY)
                    gregorian.weekOfYear(g(2026, 1, 1), rule) shouldBe WeekOfYear(2026, 1)
                    gregorian.weekOfYear(g(2026, 1, 3), rule) shouldBe WeekOfYear(2026, 2)
                    // The week holding 2026-01-01 starts on Saturday 2025-12-27, so 2025-12-31 is 2026 week 1.
                    gregorian.weekOfYear(g(2025, 12, 31), rule) shouldBe WeekOfYear(2026, 1)
                    gregorian.weekOfYear(g(2025, 12, 26), rule) shouldBe WeekOfYear(2025, 52)
                },
                "minimal days must be 1..7" to {
                    shouldThrow<IllegalArgumentException> { WeekRule(Weekday.MONDAY, 0) }
                    shouldThrow<IllegalArgumentException> { WeekRule(Weekday.MONDAY, 8) }
                },
            )

    @TestFactory
    fun `position within a season`() =
        rows(
            "first day of season 0" to { gregorian.positionInSeason(g(2026, 1, 1)) shouldBe SeasonPosition(0, 1, 90) },
            "last day of season 0 (leap)" to {
                gregorian.positionInSeason(g(2024, 3, 31)) shouldBe SeasonPosition(0, 91, 91)
            },
            "middle of season 2" to { gregorian.positionInSeason(g(2026, 8, 15)) shouldBe SeasonPosition(2, 46, 92) },
            "last season" to { gregorian.positionInSeason(g(2026, 12, 31)) shouldBe SeasonPosition(3, 92, 92) },
        )

    @Test
    fun `adding months to a month start and back returns the month start`(): Unit =
        runBlocking {
            val years = Arb.int(-5_000..5_000)
            val shifts = Arb.int(-2_400..2_400)
            checkAll(propertyConfig, years, Arb.int(1..12), shifts) { year, month, k ->
                val start = g(year, month, 1)
                gregorian.addMonths(gregorian.addMonths(start, k), -k) shouldBe start
                val hijriStart = h(year, month, 1)
                islamic.addMonths(islamic.addMonths(hijriStart, k), -k) shouldBe hijriStart
            }
        }

    @Test
    fun `adding months and back returns the start at the property's own range edges`() {
        val yearMonths = listOf(-5_000, 5_000).flatMap { year -> listOf(1, 12).map { month -> year to month } }
        yearMonths
            .flatMap { pair -> listOf(-2_400, 2_400).map { k -> Triple(pair.first, pair.second, k) } }
            .forEach { (year, month, k) ->
                val start = g(year, month, 1)
                gregorian.addMonths(gregorian.addMonths(start, k), -k) shouldBe start
                val hijriStart = h(year, month, 1)
                islamic.addMonths(islamic.addMonths(hijriStart, k), -k) shouldBe hijriStart
            }
    }

    @Test
    fun `period components rebuild the target date`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(-400_000..400_000), Arb.int(0..20_000)) { offset, span ->
                val from = gregorian.plusDays(g(2000, 1, 1), offset.toLong())
                val to = gregorian.plusDays(from, span.toLong())
                val period = gregorian.periodBetween(from, to)
                val monthsOnly = gregorian.addMonths(from, period.years * 12 + period.months)
                gregorian.plusDays(monthsOnly, period.days.toLong()) shouldBe to
            }
        }

    @Test
    fun `period components rebuild the target date at the property's own range edges`() {
        listOf(-400_000, 400_000).forEach { offset ->
            listOf(0, 20_000).forEach { span ->
                val from = gregorian.plusDays(g(2000, 1, 1), offset.toLong())
                val to = gregorian.plusDays(from, span.toLong())
                val period = gregorian.periodBetween(from, to)
                val monthsOnly = gregorian.addMonths(from, period.years * 12 + period.months)
                gregorian.plusDays(monthsOnly, period.days.toLong()) shouldBe to
            }
        }
    }

    private companion object {
        val GREGORIAN_MONTHS = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    }
}
