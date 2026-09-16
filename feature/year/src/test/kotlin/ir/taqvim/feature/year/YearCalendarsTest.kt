/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import org.junit.jupiter.api.Test

/** T-805: available calendars, year boundaries and the offered year range. */
class YearCalendarsTest {
    private val calendars = YearCalendars(PERSIAN_FIRST)

    @Test
    fun `calendars keep the user's order without duplicates`() {
        val mixed =
            listOf(CalendarSystem.NEPALI, CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN)

        YearCalendars(PERSIAN_FIRST.copy(calendars = mixed)).systems shouldContainExactly
            listOf(CalendarSystem.NEPALI, CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN)
        YearCalendars(PERSIAN_FIRST.copy(calendars = emptyList())).systems shouldContainExactly
            listOf(CalendarSystem.GREGORIAN)
        calendars.clampIndex(-1) shouldBe 0
        calendars.clampIndex(9) shouldBe 2
    }

    @Test
    fun `the Persian year 1405 runs from Nowruz 2026 to the day before Nowruz 1406`() {
        val nowruz = gregorian(2026, 3, 21)
        val days = calendars.yearDays(0, 1405)

        calendars.yearStart(0, 1405) shouldBe nowruz
        days.start shouldBe nowruz
        days.endInclusive + 1 shouldBe calendars.yearStart(0, 1406)
        days.dayCount.toInt() shouldBeInRange 365..366
        calendars.monthStarts(0, 1405).size shouldBe 12
        calendars.monthStarts(0, 1405).first() shouldBe nowruz
        calendars.yearOf(0, nowruz - 1) shouldBe 1404
        calendars.yearOf(0, nowruz) shouldBe 1405
        calendars.yearOf(1, nowruz) shouldBe 2026
    }

    @Test
    fun `every month start begins its month and follows the previous month`() {
        listOf(0, 1, 2).forEach { index ->
            val year = calendars.yearOf(index, gregorian(2026, 4, 10))
            val starts = calendars.monthStarts(index, year)
            val calendar = calendars.arithmetic[index]
            starts.forEach { calendar.fromJdn(it).day shouldBe 1 }
            starts.zipWithNext().forEach { (first, next) ->
                val date = calendar.fromJdn(first)
                (next - first).toInt() shouldBe calendar.monthLength(date.year, date.month)
            }
        }
    }

    @Test
    fun `years stay within the offered range and the Islamic calendar follows the variant`() {
        calendars.arithmetic.indices.forEach { index ->
            val years = calendars.years(index)
            years shouldBe CalendarLimits.years(calendars.arithmetic[index])
            calendars.yearStart(index, 0) shouldBe calendars.yearStart(index, 0.coerceIn(years))
            calendars.yearStart(index, Int.MAX_VALUE) shouldBe calendars.yearStart(index, years.last)
            calendars.yearStart(index, Int.MIN_VALUE) shouldBe calendars.yearStart(index, years.first)
            calendars.yearOf(index, CalendarLimits.LAST_DAY) shouldBe years.last
            calendars.yearOf(index, CalendarLimits.FIRST_DAY) shouldBe years.first
            calendars.yearDays(index, years.last).endInclusive shouldBeLessThanOrEqualTo CalendarLimits.LAST_DAY
            calendars.monthStarts(index, years.first).first() shouldBeGreaterThanOrEqualTo CalendarLimits.FIRST_DAY
        }
        YearCalendars.pickerYears(1405, calendars.years(0)) shouldBe 405..2405
        YearCalendars.pickerYears(Int.MAX_VALUE, calendars.years(0)) shouldBe
            (calendars.years(0).last - YearCalendars.PICKER_SPAN)..calendars.years(0).last
        YearCalendars.pickerYears(Int.MIN_VALUE, calendars.years(0)) shouldBe
            calendars.years(0).first..(calendars.years(0).first + YearCalendars.PICKER_SPAN)
        YearCalendars.pickerYears(5, 1..10) shouldBe 1..10
        // Rows of four keep the years they had when every year from 1 was listed.
        YearCalendars.pickerYears(2026, 1..5000, 4) shouldBe 1025..3026
        calendars.arithmetic.indices.forEach { index ->
            val years = calendars.years(index)
            val listed = YearCalendars.pickerYears(1405, years, 4)
            Math.floorMod(listed.first.toLong() - years.first, 4L) shouldBe 0L
            (1405 in listed) shouldBe true
            Math.floorMod(listed.first - 1, 4) shouldBe 0
        }

        val ummAlQura = YearCalendars(PERSIAN_FIRST.copy(islamicVariant = IslamicVariant.UMM_AL_QURA))
        ummAlQura.systems[2] shouldBe CalendarSystem.ISLAMIC
        ummAlQura.monthStarts(2, 1447).size shouldBe 12
    }
}
