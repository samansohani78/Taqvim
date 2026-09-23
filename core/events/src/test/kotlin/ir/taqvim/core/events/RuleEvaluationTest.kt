/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.PersianCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.time.Instant
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** At least ten cases per rule, each checked against java.time or ICU4J (T-300). */
@Suppress("DEPRECATION")
class RuleEvaluationTest {
    private val years = 2015..2026

    private fun days(
        definition: EventDefinition,
        year: Int,
        calculator: OccurrenceCalculator = OccurrenceCalculator(listOf(definition)),
    ): List<LocalDate> = calculator.occurrences(definition, year).map { it.jdn.toJavaDate() }

    private fun cases(
        name: String,
        years: Iterable<Int>,
        check: (Int) -> Unit,
    ): List<DynamicTest> = years.map { year -> DynamicTest.dynamicTest("$name $year") { check(year) } }

    @TestFactory
    fun `Fixed occurs only on existing days`(): List<DynamicTest> {
        val leapDay = event("test.leap-day", CalendarSystem.GREGORIAN, EventRule.Fixed(2, 29))
        val lastEsfand = event("test.esfand-30", CalendarSystem.PERSIAN, EventRule.Fixed(12, 30))
        val icu = PersianCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)
        return cases("29 February", years) { year ->
            days(leapDay, year) shouldBe
                if (Year.isLeap(year.toLong())) listOf(LocalDate.of(year, 2, 29)) else emptyList()
        } +
            cases("30 Esfand", 1398..1409) { year ->
                icu.clear()
                icu.set(year, 11, 1)
                val esfandHas30 = icu.getActualMaximum(Calendar.DAY_OF_MONTH) == 30
                days(lastEsfand, year).size shouldBe if (esfandHas30) 1 else 0
            }
    }

    @TestFactory
    fun `NthWeekdayOfMonth skips months without that weekday`(): List<DynamicTest> {
        val fifthFriday =
            event("test.fifth-friday", CalendarSystem.GREGORIAN, EventRule.NthWeekdayOfMonth(3, Weekday.FRIDAY, 5))
        return cases("5th Friday of March", years) { year ->
            val candidate = LocalDate.of(year, 3, 1).with(TemporalAdjusters.dayOfWeekInMonth(5, DayOfWeek.FRIDAY))
            days(fifthFriday, year) shouldBe if (candidate.monthValue == 3) listOf(candidate) else emptyList()
        }
    }

    @TestFactory
    fun `LastWeekdayOfMonth applies its offset`(): List<DynamicTest> {
        val lastMonday =
            event("test.last-monday", CalendarSystem.GREGORIAN, EventRule.LastWeekdayOfMonth(5, Weekday.MONDAY))
        val weekBefore =
            event("test.week-before", CalendarSystem.GREGORIAN, EventRule.LastWeekdayOfMonth(5, Weekday.MONDAY, -6))
        return cases("last Monday of May", years) { year ->
            val expected = LocalDate.of(year, 5, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
            days(lastMonday, year) shouldBe listOf(expected)
            days(weekBefore, year) shouldBe listOf(expected.minusDays(6))
        }
    }

    @TestFactory
    fun `LastDayOfMonth follows month length`(): List<DynamicTest> {
        val endOfFebruary = event("test.end-of-february", CalendarSystem.GREGORIAN, EventRule.LastDayOfMonth(2))
        return cases("last day of February", years) { year ->
            days(endOfFebruary, year) shouldBe listOf(YearMonth.of(year, 2).atEndOfMonth())
        }
    }

    @TestFactory
    fun `Single occurs in its year only`(): List<DynamicTest> {
        val once = event("test.once", CalendarSystem.GREGORIAN, EventRule.Single(2020, 6, 1))
        return cases("1 June 2020 only", years) { year ->
            days(once, year) shouldBe if (year == 2020) listOf(LocalDate.of(2020, 6, 1)) else emptyList()
        }
    }

    @TestFactory
    fun `NthDayOfYear counts from the first day`(): List<DynamicTest> {
        val day60 = event("test.day-60", CalendarSystem.GREGORIAN, EventRule.NthDayOfYear(60))
        val day366 = event("test.day-366", CalendarSystem.GREGORIAN, EventRule.NthDayOfYear(366))
        return cases("day 60 and day 366", years) { year ->
            days(day60, year) shouldBe listOf(LocalDate.ofYearDay(year, 60))
            days(day366, year) shouldBe
                if (Year.isLeap(year.toLong())) listOf(LocalDate.of(year, 12, 31)) else emptyList()
        }
    }

    @TestFactory
    fun `RelativeToEvent works across calendars`(): List<DynamicTest> {
        val nowruz = event("test.nowruz", CalendarSystem.PERSIAN, EventRule.Fixed(1, 1))
        val eve = event("test.nowruz-eve", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(nowruz.id, -1))
        val calculator = OccurrenceCalculator(listOf(nowruz, eve))
        val icu = PersianCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)
        return cases("day before 1 Farvardin", years) { year ->
            icu.clear()
            icu.set(year - 621, 0, 1)
            val firstFarvardin = LocalDate.ofEpochDay(Math.floorDiv(icu.timeInMillis, MILLIS_PER_DAY))
            days(eve, year, calculator) shouldBe listOf(firstFarvardin.minusDays(1))
        }
    }

    @TestFactory
    fun `Astronomical uses the local day in the rule's time zone`(): List<DynamicTest> {
        val source =
            AstronomicalEventSource { _, from, until ->
                (from.toString().take(4).toInt() - 1..until.toString().take(4).toInt())
                    .map { Instant.parse("$it-03-20T23:50:00Z") }
            }
        val tehran =
            event(
                "test.equinox-tehran",
                CalendarSystem.GREGORIAN,
                EventRule.Astronomical(AstroKind.MARCH_EQUINOX, 1, "Asia/Tehran"),
            )
        val utc =
            event(
                "test.equinox-utc",
                CalendarSystem.GREGORIAN,
                EventRule.Astronomical(AstroKind.MARCH_EQUINOX, 0, "UTC"),
            )
        val calculator = OccurrenceCalculator(listOf(tehran, utc), astronomy = source)
        return cases("March equinox", years) { year ->
            days(tehran, year, calculator) shouldBe listOf(LocalDate.of(year, 3, 22))
            days(utc, year, calculator) shouldBe listOf(LocalDate.of(year, 3, 20))
        }
    }

    @TestFactory
    fun `Astronomical with month keeps the earliest FULL_MOON of that month (ADR-0044)`(): List<DynamicTest> {
        // A source shaped like every real "blue moon" May in the USNO golden (e.g. 2026: 1 May 17:23Z, 31 May
        // 08:45Z): two full moons, one at each end of May, and one more in April and June so the month filter is
        // exercised, not just an already-single-element list.
        val source =
            AstronomicalEventSource { kind, from, until ->
                require(kind == AstroKind.FULL_MOON)
                val year = from.toString().take(4).toInt()
                listOf(
                    Instant.parse("$year-04-15T00:00:00Z"),
                    Instant.parse("$year-05-01T17:23:00Z"),
                    Instant.parse("$year-05-31T08:45:00Z"),
                    Instant.parse("$year-06-14T00:00:00Z"),
                ).filter { it >= from && it < until }
            }
        val vesak =
            event(
                "test.vesak",
                CalendarSystem.GREGORIAN,
                EventRule.Astronomical(AstroKind.FULL_MOON, 0, "UTC", month = 5),
            )
        val calculator = OccurrenceCalculator(listOf(vesak), astronomy = source)
        return cases("day of the full moon in May", years) { year ->
            days(vesak, year, calculator) shouldBe listOf(LocalDate.of(year, 5, 1))
        }
    }

    @Test
    fun `Astronomical converts one instant to the expected civil day across time zones, including the date line`() {
        // 2024's real USNO full moon (core/astronomy golden/usno/moon-phases-1700-2100.csv): 2024-05-23T13:53:00Z.
        val instant = "2024-05-23T13:53:00Z"
        val source = AstronomicalEventSource { _, _, _ -> listOf(Instant.parse(instant)) }
        // Pacific/Kiritimati (UTC+14) and Etc/GMT+11 (UTC-11) sit either side of the international date line.
        listOf("UTC", "Pacific/Kiritimati", "Etc/GMT+11", "Asia/Tehran").forEach { zone ->
            val definition = event("test.zone-$zone", CalendarSystem.GREGORIAN, rule(zone))
            val calculator = OccurrenceCalculator(listOf(definition), astronomy = source)
            val expected =
                java.time.Instant
                    .parse(instant)
                    .atZone(java.time.ZoneId.of(zone))
                    .toLocalDate()
            days(definition, 2024, calculator) shouldBe listOf(expected)
        }
    }

    @Test
    fun `Astronomical treats an instant exactly at midnight as the day it begins`() {
        val source = AstronomicalEventSource { _, _, _ -> listOf(Instant.parse("2024-05-01T00:00:00Z")) }
        val rule = EventRule.Astronomical(AstroKind.FULL_MOON, 0, "UTC", month = 5)
        val definition = event("test.midnight", CalendarSystem.GREGORIAN, rule)
        val calculator = OccurrenceCalculator(listOf(definition), astronomy = source)
        days(definition, 2024, calculator) shouldBe listOf(LocalDate.of(2024, 5, 1))
    }

    private fun rule(timeZone: String) = EventRule.Astronomical(AstroKind.FULL_MOON, 0, timeZone, month = 5)

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
