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
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod as CivilPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * T-106 utilities on the Bikram Sambat calendar (32-day months) and the Hebrew calendar (13-month years). Every
 * Nepali expectation is derived from the official National Panchang 2082/2083 month starts (the golden also holds
 * the first day of 2084), never typed by hand.
 */
class NepaliCalendarMathTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val nepali = NepaliCalendarSystem
    private val hebrew = HebrewCalendarSystem

    /** Official month starts, in order: (BS year, month, first civil day). */
    private val starts =
        GoldenFile
            .load("golden/nepal/national-panchang-2082-2083.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { Triple(it[0].toInt(), it[1].toInt(), LocalDate.parse(it[3])) }

    /** The printed months with their official lengths (the day before the next printed start is the last day). */
    private val months = starts.zipWithNext { current, next -> Official(current, next.third) }

    private data class Official(
        val year: Int,
        val month: Int,
        val first: LocalDate,
        val last: LocalDate,
    ) {
        constructor(start: Triple<Int, Int, LocalDate>, nextFirst: LocalDate) :
            this(start.first, start.second, start.third, nextFirst.minus(CivilPeriod(days = 1)))

        val length: Int get() = (last.toEpochDays() - first.toEpochDays()).toInt() + 1

        fun date(day: Int) = CalendarDate(CalendarSystem.NEPALI, year, month, day)
    }

    private fun weekday(date: LocalDate): Weekday = date.toJdn().weekday()

    private fun rows(
        label: String,
        check: (Official) -> Unit,
    ) = months.map { DynamicTest.dynamicTest("$label ${it.year}-${it.month}") { check(it) } }

    @TestFactory
    fun `month and year lengths match the panchang`() =
        rows("length of") { nepali.monthLength(it.year, it.month) shouldBe it.length } +
            months.groupBy { it.year }.map { (year, printed) ->
                DynamicTest.dynamicTest("BS $year has ${printed.sumOf { it.length }} days in 12 months") {
                    nepali.monthsInYear(year) shouldBe printed.size
                    nepali.yearLength(year) shouldBe printed.sumOf { it.length }
                }
            } +
            DynamicTest.dynamicTest("the panchang years include a 32-day month") {
                months.maxOf { it.length } shouldBe nepali.monthLength(2082, 3)
                nepali.monthLength(2082, 3) shouldBe 32
            }

    @TestFactory
    fun `day of year counts from Baisakh 1`() =
        rows("first day of") { month ->
            val baisakh = months.first { it.year == month.year && it.month == 1 }.first
            val expected = (month.first.toEpochDays() - baisakh.toEpochDays()).toInt() + 1
            nepali.dayOfYear(month.date(1)) shouldBe expected
            nepali.dayOfYear(month.date(month.length)) shouldBe expected + month.length - 1
        }

    @TestFactory
    fun `first and last weekdays of each month`() =
        rows("weekdays of") { month ->
            nepali.nthWeekdayOfMonth(month.year, month.month, weekday(month.first), 1) shouldBe month.date(1)
            nepali.nthWeekdayOfMonth(month.year, month.month, weekday(month.first), 5) shouldBe
                month.date(29).takeIf { month.length >= 29 }
            nepali.lastWeekdayOfMonth(month.year, month.month, weekday(month.last)) shouldBe month.date(month.length)
            nepali.lastWeekdayOfMonth(month.year, month.month, weekday(month.last), 1) shouldBe
                nepali.plusDays(month.date(month.length), 1)
        }

    @TestFactory
    fun `days and months between official month starts`() =
        rows("from Baisakh 2082 to") { month ->
            val start = months.first()
            nepali.daysBetween(start.date(1), month.date(1)) shouldBe
                month.first.toEpochDays() - start.first.toEpochDays()
            nepali.monthsBetween(start.date(1), month.date(1)) shouldBe months.indexOf(month)
            nepali.plusDays(start.date(1), month.first.toEpochDays() - start.first.toEpochDays()) shouldBe month.date(1)
        }

    @TestFactory
    fun `adding a month clamps the last day to the next month's length`() =
        months.zipWithNext { month, next ->
            DynamicTest.dynamicTest("last day of ${month.year}-${month.month} + 1 month") {
                nepali.addMonths(month.date(month.length), 1) shouldBe next.date(minOf(month.length, next.length))
                nepali.addMonths(next.date(1), -1) shouldBe month.date(1)
            }
        }

    @TestFactory
    fun `week numbers with week 1 holding Baisakh 1`() =
        rows("week of the first day of") { month ->
            val rule = WeekRule.containingFirstDay(Weekday.SUNDAY)
            val baisakh = months.first { it.year == month.year && it.month == 1 }.first
            val weekStart = baisakh.minus(CivilPeriod(days = weekday(baisakh).daysAfter(Weekday.SUNDAY)))
            val expected = ((month.first.toEpochDays() - weekStart.toEpochDays()) / DAYS_PER_WEEK).toInt() + 1
            nepali.weekOfYear(month.date(1), rule) shouldBe WeekOfYear(month.year, expected)
        }

    @Test
    fun `the golden spans two full years with a civil next start`() {
        months.size shouldBe 24
        starts.last().third shouldBe months.last().last.plus(CivilPeriod(days = 1))
    }

    @Test
    fun `adding months to a month start and back returns the month start`(): Unit =
        runBlocking {
            val shifts = Arb.int(-400..400)
            checkAll(propertyConfig, Arb.int(1900..2200), Arb.int(1..12), shifts) { year, month, k ->
                val start = CalendarDate(CalendarSystem.NEPALI, year, month, 1)
                nepali.addMonths(nepali.addMonths(start, k), -k) shouldBe start
            }
            checkAll(propertyConfig, Arb.int(5000..6000), Arb.int(1..13), shifts) { year, raw, k ->
                val month = if (raw > hebrew.monthsInYear(year)) 1 else raw
                val start = CalendarDate(CalendarSystem.HEBREW, year, month, 1)
                hebrew.addMonths(hebrew.addMonths(start, k), -k) shouldBe start
            }
        }

    @Test
    fun `Hebrew months and periods count 13-month years`() {
        val leap = (5780..5790).first { hebrew.monthsInYear(it) == 13 }
        val start = CalendarDate(CalendarSystem.HEBREW, leap, 1, 1)
        hebrew.addMonths(start, 13) shouldBe CalendarDate(CalendarSystem.HEBREW, leap + 1, 1, 1)
        hebrew.monthsBetween(start, CalendarDate(CalendarSystem.HEBREW, leap + 1, 1, 1)) shouldBe 13
        hebrew.periodBetween(start, CalendarDate(CalendarSystem.HEBREW, leap + 1, 2, 3)) shouldBe DatePeriod(1, 1, 2)
        hebrew.periodBetween(CalendarDate(CalendarSystem.HEBREW, leap + 1, 2, 3), start) shouldBe
            DatePeriod(-1, -1, -2)
    }

    @Test
    fun `Hebrew period components rebuild the target date`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(5000..6000), Arb.int(0..3_000)) { year, span ->
                val from = CalendarDate(CalendarSystem.HEBREW, year, 1, 1)
                val to = hebrew.plusDays(from, span.toLong())
                val period = hebrew.periodBetween(from, to)
                val wholeYears = (0 until period.years).sumOf { hebrew.monthsInYear(year + it) }
                val monthsOnly = hebrew.addMonths(from, wholeYears + period.months)
                hebrew.plusDays(monthsOnly, period.days.toLong()) shouldBe to
            }
        }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}
