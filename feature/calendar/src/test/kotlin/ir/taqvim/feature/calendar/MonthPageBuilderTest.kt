/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.graphics.Color
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.Test

/** T-801: month pages built from the calendars, the language and the day events. */
class MonthPageBuilderTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val persianMonths = requireNotNull(persian.monthNames.persian)
    private val texts =
        MonthTexts(
            monthTitle = { month, year -> "$month $year" },
            monthRange = { first, last -> "$first-$last" },
            today = "TODAY",
            holiday = "HOLIDAY",
            events = { count, formatted -> "EVENTS $count $formatted" },
            separator = " | ",
            newEvent = "NEW",
            week = { "W$it" },
        )
    private val palette = IndicatorPalette(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)
    private val isoWeekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)

    private fun builder(
        settings: CalendarSettings = PERSIAN_FIRST,
        language: LanguageSpec = persian,
    ): MonthPageBuilder = MonthPageBuilder(CalendarCalendars(settings), language, texts, palette, isoWeekdays)

    private fun digits(value: Int): String = Numerals.format(value.toLong(), persian.numerals)

    private fun longDate(day: Jdn): String =
        DateFormatter.format(PersianCalendarSystem.fromJdn(day), day.weekday(), persian, DateStyle.LONG)

    private fun event(
        kind: DayEventKind,
        holiday: Boolean = false,
    ): DayEventItem = DayEventItem("synthetic-$kind", kind, "Synthetic", holiday)

    @Test
    fun `moving the selection maps the built page instead of building it again`() {
        val page = builder().build(offset = 0, today = today, selected = today, events = null)
        val otherDay = page.days[5]

        val moved = page.withSelection(otherDay)

        // BUG-2: the mapped page must be exactly the page the builder would have built for that selection.
        moved shouldBe builder().build(offset = 0, today = today, selected = otherDay, events = null)
        moved.selectedIndex shouldBe 5
        moved.grid.cells.count { it.isSelected } shouldBe 1
        // Only the two cells that changed are new objects, so the grid recomposes those two.
        moved.grid.cells
            .filterIndexed { i, cell -> cell !== page.grid.cells[i] }
            .size shouldBe 2
    }

    @Test
    fun `selecting the same day changes nothing and a day off the page clears the selection`() {
        val page = builder().build(offset = 0, today = today, selected = today, events = null)

        page.withSelection(today) shouldBeSameInstanceAs page

        val elsewhere = page.withSelection(page.days.last() + 1)
        elsewhere.selectedIndex shouldBe -1
        elsewhere.grid.cells.none { it.isSelected } shouldBe true
    }

    @Test
    fun `Farvardin 1405 starts on its first Saturday and fills six weeks`() {
        val page = builder().build(offset = 0, today = today, selected = today, events = null)

        page.days.size shouldBe MonthLayout.CELLS
        page.days.first() shouldBe gregorian(2026, 3, 21)
        page.grid.weekdayLabels shouldContainExactly listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
        page.grid.cells.count { it.inCurrentMonth } shouldBe 31
        page.grid.cells
            .first()
            .dayLabel shouldBe digits(1)
        page.selectedIndex shouldBe 20
        page.grid.cells[20].isToday shouldBe true
        page.grid.cells[20].isSelected shouldBe true
        page.grid.cells.count { it.isToday || it.isSelected } shouldBe 1
        page.grid.cells[20].secondaryLabels shouldContainExactly
            CalendarCalendars(PERSIAN_FIRST).datesOf(today).drop(1).map { digits(it.day) }
        page.grid.longClickLabel shouldBe "NEW"
        page.grid.weekNumbers.shouldBeNull()

        val gregorianMonths = persian.monthNames.gregorian
        page.heading.title shouldBe "${persianMonths[0]} ${digits(1405)}"
        page.heading.subtitle.orEmpty() shouldStartWith "${gregorianMonths[2]}-${gregorianMonths[3]} ${digits(2026)} | "
    }

    @Test
    fun `every month of 1405 shows its own days and title`() {
        val builder = builder()
        val pages = (0..11).map { builder.build(it, today, today, null) }

        pages.forEachIndexed { index, page ->
            page.grid.cells.count { it.inCurrentMonth } shouldBe PersianCalendarSystem.monthLength(1405, index + 1)
            page.days.first().weekday() shouldBe Weekday.SATURDAY
            page.grid.rows shouldBe MonthLayout.WEEKS
            page.heading.title shouldBe "${persianMonths[index]} ${digits(1405)}"
        }
        pages.filter { page -> page.grid.cells.any { it.isSelected } }.map { it.offset } shouldContainExactly listOf(0)
    }

    @Test
    fun `events mark holidays, draw at most three dots and are spoken`() {
        val holiday = gregorian(2026, 4, 1)
        val friday = gregorian(2026, 4, 3)
        val holidayEvents =
            listOf(
                event(DayEventKind.OFFICIAL, holiday = true),
                event(DayEventKind.PERSONAL),
                event(DayEventKind.DEVICE),
                event(DayEventKind.SUBSCRIPTION),
            )
        val events =
            listOf(
                CalendarDay(holiday, isHoliday = true, isWeekend = false, holidayEvents),
                CalendarDay(friday, isHoliday = false, isWeekend = true, emptyList()),
            )
        val page = builder().build(0, today, today, events)
        val holidayCell = page.grid.cells[page.days.indexOf(holiday)]
        val fridayCell = page.grid.cells[page.days.indexOf(friday)]

        holidayCell.isHoliday shouldBe true
        holidayCell.indicators shouldContainExactly listOf(Color.Red, Color.Green, Color.Yellow)
        holidayCell.contentDescription shouldBe "${longDate(holiday)} | HOLIDAY | EVENTS 4 ${digits(4)}"
        fridayCell.isHoliday shouldBe true
        fridayCell.contentDescription shouldBe longDate(friday)
        page.grid.cells[page.selectedIndex].contentDescription shouldBe "${longDate(today)} | TODAY"
        page.grid.cells[page.days.indexOf(gregorian(2026, 4, 4))].isHoliday shouldBe false

        val loading = builder().build(0, today, today, events = null)
        loading.grid.cells[page.days.indexOf(friday)].isHoliday shouldBe true
        loading.grid.cells[page.days.indexOf(holiday)].indicators shouldBe emptyList()
    }

    @Test
    fun `week numbers count the weeks of the year from each row's first day in the month`() {
        val builder = builder(PERSIAN_FIRST.copy(showWeekNumbers = true))
        val farvardin = builder.build(0, today, today, null)
        val esfand = builder.build(-1, today, today, null)

        farvardin.grid.weekNumbers
            .orEmpty()
            .map { it.label } shouldContainExactly (1..6).map(::digits)
        farvardin.grid.weekNumbers
            .orEmpty()
            .first()
            .contentDescription shouldBe "W${digits(1)}"
        // 1 Esfand 1404 is day 337 of a year that began on a Friday; the last row is the first week of 1405.
        esfand.grid.weekNumbers
            .orEmpty()
            .first()
            .label shouldBe digits(49)
        esfand.grid.weekNumbers
            .orEmpty()
            .last()
            .label shouldBe digits(1)
    }

    @Test
    fun `an English Gregorian month names its Persian months in the subtitle`() {
        val settings =
            CalendarSettings(
                calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
                weekStart = Weekday.SUNDAY,
                islamicVariant = IslamicVariant.UMM_AL_QURA,
                languageCode = "en",
            )
        val builder = builder(settings, english)
        val april = builder.build(0, today, today, null)
        val persianNames = requireNotNull(english.monthNames.persian)
        val gregorianNames = english.monthNames.gregorian

        april.heading.title shouldBe "${gregorianNames[3]} 2026"
        april.heading.subtitle shouldBe "${persianNames[0]}-${persianNames[1]} 1405"
        april.days.first() shouldBe gregorian(2026, 3, 29)
        april.grid.weekdayLabels.first() shouldBe "Su"
        april.grid.cells[april.days.indexOf(gregorian(2026, 4, 1))].dayLabel shouldBe "1"

        val dey = builder(PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN)))
        dey.build(-3, today, today, null).heading.subtitle shouldBe
            "${persian.monthNames.gregorian[11]} ${digits(2025)}-${persian.monthNames.gregorian[0]} ${digits(2026)}"
    }

    @Test
    fun `seven weekday names are required`() {
        shouldThrow<IllegalArgumentException> {
            MonthPageBuilder(CalendarCalendars(PERSIAN_FIRST), persian, texts, palette, isoWeekdays.drop(1))
        }
    }

    @Test
    fun `Hebrew month titles follow leap and common years`() {
        val hebrew = CalendarSettings(listOf(CalendarSystem.HEBREW), Weekday.SUNDAY, IslamicVariant.UMM_AL_QURA, "en")
        val names = builder(hebrew, english)
        // 1 Tishri 5787 (a leap year) is 12 September 2026; 5786 is a common year.
        val tishri = gregorian(2026, 9, 12)
        names.build(5, tishri, tishri, null).heading.title shouldBe "Adar I 5787"
        names.build(6, tishri, tishri, null).heading.title shouldBe "Adar II 5787"
        names.build(12, tishri, tishri, null).heading.title shouldBe "Elul 5787"
        names.build(-7, tishri, tishri, null).heading.title shouldBe "Adar 5786"
        val hindi = requireNotNull(LanguageTable.forCode("hi"))
        builder(hebrew, hindi).build(5, tishri, tishri, null).heading.title shouldBe
            "अदार प्रथम ${Numerals.format(5787L, hindi.numerals)}"
    }
}
