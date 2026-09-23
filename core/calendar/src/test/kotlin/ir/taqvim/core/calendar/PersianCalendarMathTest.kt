/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

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
 * T-106 utilities on the Persian calendar. Leap years (29/30 Esfand) follow the Calendar Center's official table and
 * weekdays follow the official 1404/1405 calendars (golden/persian, docs/PROVENANCE.md).
 */
class PersianCalendarMathTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val persian = PersianCalendarSystem
    private val saturdayWeeks = WeekRule.containingFirstDay(Weekday.SATURDAY)

    private fun p(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.PERSIAN, year, month, day)

    private fun rows(vararg rows: Pair<String, () -> Unit>) =
        rows.map { (name, check) -> DynamicTest.dynamicTest(name, check) }

    @TestFactory
    fun `month lengths including 29 and 30 Esfand`() =
        listOf(
            1399 to listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30),
            1403 to listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30),
            1404 to listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29),
            1405 to listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29),
            1408 to listOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30),
        ).flatMap { (year, lengths) ->
            lengths.mapIndexed { index, expected ->
                DynamicTest.dynamicTest("$year month ${index + 1} has $expected days") {
                    persian.monthLength(year, index + 1) shouldBe expected
                }
            }
        } +
            listOf(1309 to 366, 1310 to 365, 1403 to 366, 1404 to 365, 1408 to 366, 1498 to 366).map { (year, days) ->
                DynamicTest.dynamicTest("$year has $days days") { persian.yearLength(year) shouldBe days }
            }

    @TestFactory
    fun `day of year, adding months and distances`() =
        rows(
            "1404-01-01 is day 1" to { persian.dayOfYear(p(1404, 1, 1)) shouldBe 1 },
            "1404-06-31 is day 186" to { persian.dayOfYear(p(1404, 6, 31)) shouldBe 186 },
            "1404-07-01 is day 187" to { persian.dayOfYear(p(1404, 7, 1)) shouldBe 187 },
            "30 Esfand 1403 is day 366" to { persian.dayOfYear(p(1403, 12, 30)) shouldBe 366 },
            "29 Esfand 1404 is day 365" to { persian.dayOfYear(p(1404, 12, 29)) shouldBe 365 },
            "31 Shahrivar + 1 month clamps to 30 Mehr" to
                { persian.addMonths(p(1404, 6, 31), 1) shouldBe p(1404, 7, 30) },
            "30 Esfand 1403 + 12 months clamps to 29 Esfand" to {
                persian.addMonths(p(1403, 12, 30), 12) shouldBe p(1404, 12, 29)
            },
            "29 Esfand 1404 − 12 months" to { persian.addMonths(p(1404, 12, 29), -12) shouldBe p(1403, 12, 29) },
            "30 Bahman 1404 + 1 month clamps to 29 Esfand" to {
                persian.addMonths(p(1404, 11, 30), 1) shouldBe p(1404, 12, 29)
            },
            "30 Bahman 1403 + 1 month keeps 30 Esfand" to {
                persian.addMonths(p(1403, 11, 30), 1) shouldBe p(1403, 12, 30)
            },
            "31 Farvardin 1404 − 1 month" to { persian.addMonths(p(1404, 1, 31), -1) shouldBe p(1403, 12, 30) },
            "31 Shahrivar → 30 Mehr is one month" to
                { persian.monthsBetween(p(1404, 6, 31), p(1404, 7, 30)) shouldBe 1 },
            "31 Shahrivar → 29 Mehr is not" to { persian.monthsBetween(p(1404, 6, 31), p(1404, 7, 29)) shouldBe 0 },
            "leap year 1403 spans 366 days" to { persian.daysBetween(p(1403, 1, 1), p(1404, 1, 1)) shouldBe 366L },
            "1404 spans 365 days" to { persian.daysBetween(p(1404, 1, 1), p(1405, 1, 1)) shouldBe 365L },
            "one whole year" to { persian.periodBetween(p(1404, 1, 1), p(1405, 1, 1)) shouldBe DatePeriod(1, 0, 0) },
        )

    @TestFactory
    fun `nth and last weekday on the official calendars`() =
        rows(
            "1st Friday of Farvardin 1405" to {
                persian.nthWeekdayOfMonth(1405, 1, Weekday.FRIDAY, 1) shouldBe p(1405, 1, 7)
            },
            "5th Friday of Farvardin 1405 does not exist" to {
                persian.nthWeekdayOfMonth(1405, 1, Weekday.FRIDAY, 5) shouldBe null
            },
            "1st Saturday of Farvardin 1405 is Nowruz" to {
                persian.nthWeekdayOfMonth(1405, 1, Weekday.SATURDAY, 1) shouldBe p(1405, 1, 1)
            },
            "5th Saturday of Farvardin 1405" to {
                persian.nthWeekdayOfMonth(1405, 1, Weekday.SATURDAY, 5) shouldBe p(1405, 1, 29)
            },
            "5th Friday of Mehr 1404 does not exist" to {
                persian.nthWeekdayOfMonth(1404, 7, Weekday.FRIDAY, 5) shouldBe null
            },
            "4th Friday of Esfand 1404" to {
                persian.nthWeekdayOfMonth(1404, 12, Weekday.FRIDAY, 4) shouldBe p(1404, 12, 22)
            },
            "5th Friday of Esfand 1404 is 29 Esfand" to {
                persian.nthWeekdayOfMonth(1404, 12, Weekday.FRIDAY, 5) shouldBe p(1404, 12, 29)
            },
            "2nd Monday of Shahrivar 1405" to {
                persian.nthWeekdayOfMonth(1405, 6, Weekday.MONDAY, 2) shouldBe p(1405, 6, 9)
            },
            "3rd Wednesday of Esfand 1405" to {
                persian.nthWeekdayOfMonth(1405, 12, Weekday.WEDNESDAY, 3) shouldBe p(1405, 12, 19)
            },
            "last Friday of Esfand 1404" to {
                persian.lastWeekdayOfMonth(1404, 12, Weekday.FRIDAY) shouldBe p(1404, 12, 29)
            },
            "last Friday of Esfand 1405" to {
                persian.lastWeekdayOfMonth(1405, 12, Weekday.FRIDAY) shouldBe p(1405, 12, 28)
            },
            "last Thursday of Shahrivar 1405" to {
                persian.lastWeekdayOfMonth(1405, 6, Weekday.THURSDAY) shouldBe p(1405, 6, 26)
            },
            "last Saturday of Farvardin 1404" to {
                persian.lastWeekdayOfMonth(1404, 1, Weekday.SATURDAY) shouldBe p(1404, 1, 30)
            },
        )

    @TestFactory
    fun `Saturday-start weeks and seasons`() =
        listOf(
            p(1405, 1, 1) to WeekOfYear(1405, 1),
            p(1405, 1, 7) to WeekOfYear(1405, 1),
            p(1405, 1, 8) to WeekOfYear(1405, 2),
            p(1404, 1, 1) to WeekOfYear(1404, 1),
            p(1403, 12, 30) to WeekOfYear(1404, 1),
            p(1404, 12, 28) to WeekOfYear(1404, 53),
            p(1404, 12, 29) to WeekOfYear(1404, 53),
            // 29 Esfand 1405 is a Saturday and 1 Farvardin 1406 a Sunday, so that Saturday opens week 1 of 1406.
            p(1405, 12, 29) to WeekOfYear(1406, 1),
        ).map { (date, expected) ->
            DynamicTest.dynamicTest("week of $date") { persian.weekOfYear(date, saturdayWeeks) shouldBe expected }
        } +
            rows(
                "last day of spring" to { persian.positionInSeason(p(1404, 3, 31)) shouldBe SeasonPosition(0, 93, 93) },
                "last day of summer" to { persian.positionInSeason(p(1404, 6, 31)) shouldBe SeasonPosition(1, 93, 93) },
                "first day of autumn" to { persian.positionInSeason(p(1404, 7, 1)) shouldBe SeasonPosition(2, 1, 90) },
                "winter of a common year" to {
                    persian.positionInSeason(p(1404, 12, 29)) shouldBe SeasonPosition(3, 89, 89)
                },
                "winter of a leap year" to {
                    persian.positionInSeason(p(1403, 12, 30)) shouldBe SeasonPosition(3, 90, 90)
                },
            )

    @Test
    fun `adding months to a Persian month start and back returns the month start`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(-5_000..5_000), Arb.int(1..12), Arb.int(-2_400..2_400)) {
                year,
                month,
                k,
                ->
                val start = p(year, month, 1)
                persian.addMonths(persian.addMonths(start, k), -k) shouldBe start
            }
        }

    @Test
    fun `adding months and back returns the start at the property's own range edges`() {
        val yearMonths = listOf(-5_000, 5_000).flatMap { year -> listOf(1, 12).map { month -> year to month } }
        yearMonths
            .flatMap { pair -> listOf(-2_400, 2_400).map { k -> Triple(pair.first, pair.second, k) } }
            .forEach { (year, month, k) ->
                val start = p(year, month, 1)
                persian.addMonths(persian.addMonths(start, k), -k) shouldBe start
            }
    }
}
