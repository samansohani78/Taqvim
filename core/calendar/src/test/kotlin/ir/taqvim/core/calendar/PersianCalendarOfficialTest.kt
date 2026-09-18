/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** Columns of the `golden/persian/official/<year>.csv` fixtures. */
private const val PERSIAN_COLUMN = 0
private const val WEEKDAY_COLUMN = 1
private const val GREGORIAN_COLUMN = 3

/** Columns of `official-nowruz-instants-1360-1403.csv` and of the month-start history. */
private const val LIST_YEAR = 0
private const val LIST_LEAP = 1
private const val LIST_INSTANT = 2
private const val LIST_DAY_PERSIAN = 3
private const val LIST_DAY_GREGORIAN = 4
private const val LIST_WEEKDAY = 5
private const val HISTORY_GREGORIAN = 1
private const val HISTORY_PERSIAN = 2

/** How far a published Nowruz instant may be from the computed March equinox (brief item 3: ±1 minute). */
private val EQUINOX_TOLERANCE = 60.seconds

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
            equinoxOffset(instant) shouldBeLessThanOrEqual EQUINOX_TOLERANCE.inWholeMilliseconds
        }
    }

    @Test
    fun `the Calendar Center's list of past Nowruz instants matches the computed years`() {
        val list = rows("official-nowruz-instants-1360-1403.csv")

        list shouldHaveSize 44
        list
            .filterNot { row ->
                val year = row[LIST_YEAR].toInt()
                val day = gregorianJdn(row[LIST_DAY_GREGORIAN])
                val startsOnTime =
                    row[LIST_INSTANT].isEmpty() ||
                        PersianYearStartRule.firstDayOfYear(Instant.parse(row[LIST_INSTANT])) ==
                        persian.firstDayOfYear(year)
                persian.isLeapYear(year) == row[LIST_LEAP].isNotEmpty() && startsOnTime &&
                    persian.fromJdn(day) == date(CalendarSystem.PERSIAN, row[LIST_DAY_PERSIAN]) &&
                    day.weekday().isoNumber == row[LIST_WEEKDAY].toInt()
            }.shouldBeEmpty()
    }

    @Test
    fun `every published Nowruz instant is within a minute of the computed equinox`() {
        val offsets =
            rows("official-nowruz-instants-1360-1403.csv")
                .filter { it[LIST_INSTANT].isNotEmpty() }
                .associate { it[LIST_YEAR] to equinoxOffset(it[LIST_INSTANT]) }

        offsets.filterValues { it > EQUINOX_TOLERANCE.inWholeMilliseconds }.keys.shouldBeEmpty()
        println("Nowruz instants 1360–1403: largest difference from the computed equinox ${offsets.values.max()} ms")
    }

    @Test
    fun `official month-start history names the same day in both calendars`() {
        OfficialIranCalendars.history
            .filterNot { row ->
                val day = gregorianJdn(row[HISTORY_GREGORIAN])
                persian.fromJdn(day) == date(CalendarSystem.PERSIAN, row[HISTORY_PERSIAN])
            }.shouldBeEmpty()
    }

    /** Milliseconds between the ISO-8601 instant [text] and the computed March equinox of its Gregorian year. */
    private fun equinoxOffset(text: String): Long {
        val equinox = checkNotNull(CalendarAstronomy.marchEquinox(text.substringBefore('-').toInt()))
        return (Instant.parse(text) - equinox).absoluteValue.inWholeMilliseconds
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
