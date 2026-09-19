/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.HebrewCalendar
import ir.taqvim.core.calendar.HebrewCalendarSystem
import ir.taqvim.core.calendar.HebrewMonth
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/**
 * Yearly recurrence in the Hebrew calendar (review R01): a leap year has 13 months and a common year 12, so a month
 * number names a different month in the two kinds of year. A yearly series follows the month's name — Adar I and
 * Adar II both fall in Adar of a common year, Adar of a common year falls in Adar II of a leap year (where Purim is
 * kept), and every month after Adar keeps its name.
 */
class HebrewRecurrenceTest {
    private val engine = RecurrenceEngine(HebrewCalendarSystem)

    private fun hebrew(
        year: Int,
        month: HebrewMonth,
        day: Int,
    ) = CalendarDate(CalendarSystem.HEBREW, year, HebrewCalendar.monthNumber(year, month), day)

    private fun yearly(
        start: CalendarDate,
        count: Int = 4,
    ): List<CalendarDate> =
        engine
            .occurrences(start, RecurrenceRule(Frequency.YEARLY, count = count))
            .map(HebrewCalendarSystem::fromJdn)
            .toList()

    @Test
    fun `the years around the tests are leap and common as assumed`() {
        listOf(5784, 5785, 5786, 5787).map(HebrewCalendar::isLeapYear) shouldBe listOf(true, false, false, true)
    }

    @Test
    fun `Elul, the 13th month of a leap year, recurs in Elul of the next common year`() {
        yearly(hebrew(5784, HebrewMonth.ELUL, 1)) shouldBe
            listOf(
                hebrew(5784, HebrewMonth.ELUL, 1),
                hebrew(5785, HebrewMonth.ELUL, 1),
                hebrew(5786, HebrewMonth.ELUL, 1),
                hebrew(5787, HebrewMonth.ELUL, 1),
            )
    }

    @Test
    fun `Adar I of a leap year recurs in Adar of a common year and Adar I of the next leap year`() {
        yearly(hebrew(5784, HebrewMonth.ADAR, 10)) shouldBe
            listOf(
                hebrew(5784, HebrewMonth.ADAR, 10),
                hebrew(5785, HebrewMonth.ADAR, 10),
                hebrew(5786, HebrewMonth.ADAR, 10),
                hebrew(5787, HebrewMonth.ADAR, 10),
            )
    }

    @Test
    fun `Adar II of a leap year recurs in Adar of a common year`() {
        yearly(hebrew(5784, HebrewMonth.ADAR_II, 14), count = 3) shouldBe
            listOf(
                hebrew(5784, HebrewMonth.ADAR_II, 14),
                hebrew(5785, HebrewMonth.ADAR, 14),
                hebrew(5786, HebrewMonth.ADAR, 14),
            )
    }

    @Test
    fun `Adar of a common year recurs in Adar II of a leap year`() {
        yearly(hebrew(5785, HebrewMonth.ADAR, 14), count = 3) shouldBe
            listOf(
                hebrew(5785, HebrewMonth.ADAR, 14),
                hebrew(5786, HebrewMonth.ADAR, 14),
                hebrew(5787, HebrewMonth.ADAR_II, 14),
            )
    }

    @Test
    fun `months after Adar keep their name across leap and common years`() {
        yearly(hebrew(5784, HebrewMonth.NISAN, 15)) shouldBe
            listOf(
                hebrew(5784, HebrewMonth.NISAN, 15),
                hebrew(5785, HebrewMonth.NISAN, 15),
                hebrew(5786, HebrewMonth.NISAN, 15),
                hebrew(5787, HebrewMonth.NISAN, 15),
            )
    }

    @Test
    fun `months before Adar are the same number in every year`() {
        yearly(hebrew(5784, HebrewMonth.TISHRI, 1), count = 3) shouldBe
            listOf(
                hebrew(5784, HebrewMonth.TISHRI, 1),
                hebrew(5785, HebrewMonth.TISHRI, 1),
                hebrew(5786, HebrewMonth.TISHRI, 1),
            )
    }

    @Test
    fun `30 Adar I, which Adar never has, follows the invalid-date policy`() {
        val start = hebrew(5784, HebrewMonth.ADAR, 30)
        engine
            .occurrences(start, RecurrenceRule(Frequency.YEARLY, count = 2, invalidDates = InvalidDatePolicy.SKIP))
            .map(HebrewCalendarSystem::fromJdn)
            .toList() shouldBe listOf(start, hebrew(5787, HebrewMonth.ADAR, 30))
        engine
            .occurrences(
                start,
                RecurrenceRule(Frequency.YEARLY, count = 2, invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH),
            ).map(HebrewCalendarSystem::fromJdn)
            .toList() shouldBe listOf(start, hebrew(5785, HebrewMonth.ADAR, 29))
    }
}
