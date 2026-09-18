/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Instant
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** Columns of the `iran-official-*-days.csv` fixtures. */
private const val PERSIAN_COLUMN = 0
private const val WEEKDAY_COLUMN = 1
private const val GREGORIAN_COLUMN = 3

/** A-02 against the University of Tehran Calendar Center's published data (golden/persian, docs/sources). */
class PersianCalendarOfficialTest {
    private val persian = PersianCalendarSystem

    private fun rows(name: String): List<List<String>> =
        GoldenFile
            .load("golden/persian/$name")
            .lines
            .drop(1)
            .map { it.split(',') }

    private fun date(
        system: CalendarSystem,
        iso: String,
    ): CalendarDate {
        val (year, month, day) = iso.split('-').map(String::toInt)
        return CalendarDate(system, year, month, day)
    }

    private fun gregorianJdn(iso: String) = GregorianCalendarSystem.toJdn(date(CalendarSystem.GREGORIAN, iso))

    @Test
    fun `leap years match the official table for 1206 to 1498`() {
        val table = rows("official-leap-years-1206-1498.csv")

        table shouldHaveSize 293
        table.filter { (year, marker) -> persian.isLeapYear(year.toInt()) != marker.isNotEmpty() }.shouldBeEmpty()
    }

    @TestFactory
    fun `every day of the official calendars converts both ways`(): List<DynamicTest> =
        OfficialIranCalendars.imported.map { calendar ->
            DynamicTest.dynamicTest(calendar.fixture) {
                val days = OfficialIranCalendars.rows(calendar.fixture)
                days shouldHaveSize calendar.days
                days
                    .filterNot { row ->
                        val jdn = gregorianJdn(row[GREGORIAN_COLUMN])
                        val expected = date(CalendarSystem.PERSIAN, row[PERSIAN_COLUMN])
                        persian.toJdn(expected) == jdn && persian.fromJdn(jdn) == expected &&
                            jdn.weekday().isoNumber == row[WEEKDAY_COLUMN].toInt()
                    }.shouldBeEmpty()
            }
        }

    @Test
    fun `official Nowruz instants start the official calendars`() {
        rows("official-nowruz-instants.csv").forEach { (year, instant, firstDay) ->
            val expected = gregorianJdn(firstDay)

            PersianYearStartRule.firstDayOfYear(Instant.parse(instant)) shouldBe expected
            persian.firstDayOfYear(year.toInt()) shouldBe expected
        }
    }

    @Test
    fun `century boundaries match the Calendar Center note`() {
        rows("century-boundaries.csv").forEach { (iso, weekday) ->
            val day = date(CalendarSystem.PERSIAN, iso)

            persian.toJdn(day).weekday().isoNumber shouldBe weekday.toInt()
            if (day.month == 12) persian.monthLength(day.year, day.month) shouldBe day.day
        }
    }
}
