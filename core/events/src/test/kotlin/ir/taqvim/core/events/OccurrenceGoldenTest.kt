/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.PersianCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** Golden cases required by T-300, against ICU4J and java.time. */
@Suppress("DEPRECATION")
class OccurrenceGoldenTest {
    private fun jdnValues(
        definition: EventDefinition,
        year: Int,
        calendars: CalendarProvider = CalendarProvider.DEFAULT,
    ): List<Long> =
        OccurrenceCalculator(listOf(definition), calendars).occurrences(definition, year).map { it.jdn.value }

    /** Walks [calendar] back from the last day of its current month to the last day with [dayOfWeek]. */
    private fun lastWeekdayJdn(
        calendar: Calendar,
        dayOfWeek: Int,
    ): Long {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) calendar.add(Calendar.DATE, -1)
        return LocalDate.ofEpochDay(Math.floorDiv(calendar.timeInMillis, MILLIS_PER_DAY)).toJdnValue()
    }

    @TestFactory
    fun `last Friday of Ramadan 1440 to 1450`(): List<DynamicTest> {
        val lastFriday =
            event(
                "test.last-friday-of-ramadan",
                CalendarSystem.ISLAMIC,
                EventRule.LastWeekdayOfMonth(9, Weekday.FRIDAY),
            )
        val icu =
            IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT).apply {
                calculationType =
                    IslamicCalendar.CalculationType.ISLAMIC_CIVIL
            }
        return (1440..1450).map { year ->
            DynamicTest.dynamicTest("AH $year") {
                icu.clear()
                icu.set(Calendar.EXTENDED_YEAR, year)
                icu.set(Calendar.MONTH, 8)
                icu.set(Calendar.DAY_OF_MONTH, 1)
                jdnValues(lastFriday, year, TABULAR_ISLAMIC_CALENDARS) shouldBe
                    listOf(lastWeekdayJdn(icu, Calendar.FRIDAY))
            }
        }
    }

    @TestFactory
    fun `third Thursday of November 2020 to 2035`(): List<DynamicTest> {
        val rule =
            event("test.third-thursday", CalendarSystem.GREGORIAN, EventRule.NthWeekdayOfMonth(11, Weekday.THURSDAY, 3))
        return (2020..2035).map { year ->
            DynamicTest.dynamicTest("$year") {
                val expected = LocalDate.of(year, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.THURSDAY))
                jdnValues(rule, year) shouldBe listOf(expected.toJdnValue())
            }
        }
    }

    @TestFactory
    fun `day 256 of the year`(): List<DynamicTest> {
        val rule = event("test.day-256", CalendarSystem.GREGORIAN, EventRule.NthDayOfYear(256))
        return (2015..2030).map { year ->
            DynamicTest.dynamicTest("$year") {
                jdnValues(rule, year) shouldBe
                    listOf(LocalDate.ofYearDay(year, 256).toJdnValue())
            }
        }
    }

    @TestFactory
    fun `last Tuesday of the Persian year`(): List<DynamicTest> {
        val rule = event("test.last-tuesday", CalendarSystem.PERSIAN, EventRule.LastWeekdayOfMonth(12, Weekday.TUESDAY))
        val icu = PersianCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)
        return (1395..1410).map { year ->
            DynamicTest.dynamicTest("$year SH") {
                icu.clear()
                icu.set(year, 11, 1)
                jdnValues(rule, year) shouldBe listOf(lastWeekdayJdn(icu, Calendar.TUESDAY))
            }
        }
    }

    @Test
    fun `the official calendar's last Friday of Ramadan 1447 uses the Iranian lunar calendar`() {
        // Calendar-1404.pdf page 15: 29 Esfand 1404 (20 March 2026) is "the last Friday of Ramadan" (golden
        // persian/official/1404.csv, lunar date 1447-09-30).
        val lastFriday = event("test.quds-day", CalendarSystem.ISLAMIC, EventRule.LastWeekdayOfMonth(9, Weekday.FRIDAY))

        jdnValues(lastFriday, 1447) shouldBe listOf(LocalDate.of(2026, 3, 20).toJdnValue())
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
