/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import org.junit.jupiter.api.Test

/** T-805: available calendars, year boundaries and the offered year range. */
class YearCalendarsTest {
    private val calendars = YearCalendars(PERSIAN_FIRST)

    @Test
    fun `available calendars keep the user's order without duplicates or Nepali`() {
        val mixed =
            listOf(CalendarSystem.NEPALI, CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN)

        YearCalendars(PERSIAN_FIRST.copy(calendars = mixed)).systems shouldContainExactly
            listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN)
        YearCalendars(PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.NEPALI))).systems shouldContainExactly
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
        calendars.yearStart(1, 0) shouldBe calendars.yearStart(1, 1)
        calendars.yearStart(1, YearCalendars.MAX_YEAR + 5) shouldBe calendars.yearStart(1, YearCalendars.MAX_YEAR)
        calendars.yearOf(1, calendars.yearStart(1, YearCalendars.MAX_YEAR) + 400) shouldBe YearCalendars.MAX_YEAR

        val ummAlQura = YearCalendars(PERSIAN_FIRST.copy(islamicVariant = IslamicVariant.UMM_AL_QURA))
        ummAlQura.systems[2] shouldBe CalendarSystem.ISLAMIC
        ummAlQura.monthStarts(2, 1447).size shouldBe 12
    }
}
