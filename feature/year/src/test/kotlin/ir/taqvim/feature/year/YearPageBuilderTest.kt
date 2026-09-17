/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.TimingTest
import kotlin.system.measureTimeMillis
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** T-805: year pages — month grids, today, holidays and weekends, headings and build time. */
class YearPageBuilderTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val texts =
        YearTexts(
            monthTitle = { month, year -> "$month $year" },
            range = { first, last -> "$first–$last" },
            today = "today",
            holidays = { _, formatted -> "$formatted holidays" },
            separator = " | ",
        )

    /** ISO order, Monday first. */
    private val weekdayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)

    private fun builder(
        settings: YearSettings = PERSIAN_FIRST,
        language: LanguageSpec = persian,
    ): YearPageBuilder = YearPageBuilder(YearCalendars(settings), language, texts, weekdayNames)

    private fun persianDigits(value: Int): String = Numerals.format(value.toLong(), persian.numerals)

    @Test
    fun `every month of 1405 starts on its weekday and fills its own days`() {
        val calendars = YearCalendars(PERSIAN_FIRST)
        val page = builder().build(0, 1405, today, days = null)

        page.year shouldBe 1405
        page.weekdayLabels shouldContainExactly listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
        page.months.size shouldBe 12
        page.months.map { it.firstDay } shouldContainExactly calendars.monthStarts(0, 1405)
        page.months.map { it.name } shouldContainExactly requireNotNull(persian.monthNames.persian)
        page.months.forEach { month ->
            val date = PERSIAN_ARITHMETIC.fromJdn(month.firstDay)
            val lead = month.firstDay.weekday().daysAfter(Weekday.SATURDAY)
            month.cells.size shouldBe YearPageBuilder.CELLS
            month.cells.indexOfFirst { it != null } shouldBe lead
            month.cells.count { it != null } shouldBe PERSIAN_ARITHMETIC.monthLength(date.year, date.month)
            month.cells[lead]?.label shouldBe persianDigits(1)
        }
    }

    @Test
    fun `today, holidays and weekends mark their days and the month summary`() {
        val calendars = YearCalendars(PERSIAN_FIRST)
        val islamicRepublicDay = gregorian(2026, 4, 1)
        val days =
            calendars.yearDays(0, 1405).map { day ->
                YearDay(day, isHoliday = day == islamicRepublicDay, isWeekend = day.weekday() == Weekday.FRIDAY)
            }
        val farvardin = builder().build(0, 1405, today, days).months.first()
        val lead = farvardin.cells.indexOfFirst { it != null }

        farvardin.cells[lead + 20]?.isToday shouldBe true
        farvardin.cells[lead + 11]?.tone shouldBe MiniDayTone.OFF_DAY
        farvardin.cells[lead + 6]?.tone shouldBe MiniDayTone.OFF_DAY
        farvardin.cells[lead + 1]?.tone shouldBe MiniDayTone.NORMAL
        farvardin.description shouldBe "${persian.monthNames.persian?.first()} ${persianDigits(1405)} | today | " +
            "${persianDigits(1)} holidays"
        builder().build(0, 1405, today, days).months[1].description shouldBe
            "${persian.monthNames.persian?.get(1)} ${persianDigits(1405)}"
    }

    @Test
    fun `source flags take precedence and the language's weekend is used while they load`() {
        val calendars = YearCalendars(PERSIAN_FIRST)
        val noWeekends = calendars.yearDays(0, 1405).map { YearDay(it, isHoliday = false, isWeekend = false) }
        // 7 Farvardin 1405 (2026-03-27) is a Friday, the weekend in Persian.
        val friday = 6

        builder()
            .build(0, 1405, today, days = null)
            .months
            .first()
            .cells[friday]
            ?.tone shouldBe MiniDayTone.OFF_DAY
        builder()
            .build(0, 1405, today, noWeekends)
            .months
            .first()
            .cells[friday]
            ?.tone shouldBe MiniDayTone.NORMAL
    }

    @Test
    fun `headings show the same days' years in the other calendars`() {
        val heading = builder().heading(0, 1405)
        val islamic = YearCalendars(PERSIAN_FIRST).arithmetic[2]
        val days = YearCalendars(PERSIAN_FIRST).yearDays(0, 1405)
        val firstIslamicYear = persianDigits(islamic.fromJdn(days.start).year)
        val lastIslamicYear = persianDigits(islamic.fromJdn(days.endInclusive).year)
        val islamicYears = "$firstIslamicYear–$lastIslamicYear"

        heading.title shouldBe persianDigits(1405)
        heading.subtitle shouldBe "${persianDigits(2026)}–${persianDigits(2027)} | $islamicYears"

        val gregorianFirst =
            YearSettings(
                listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
                Weekday.SUNDAY,
                IslamicVariant.UMM_AL_QURA,
                "en",
            )
        builder(gregorianFirst, english).heading(0, 2026) shouldBe YearHeading("2026", "1404–1405")
        builder(gregorianFirst.copy(calendars = listOf(CalendarSystem.GREGORIAN)), english)
            .heading(0, 2026)
            .subtitle
            .shouldBeNull()
    }

    @Test
    fun `months without names in the language are numbered and seven weekday names are required`() {
        val chinese = requireNotNull(LanguageTable.forCode("zh"))
        val page = builder(PERSIAN_FIRST.copy(languageCode = "zh"), chinese).build(0, 1405, today, days = null)

        page.months.first().name shouldBe Numerals.format(1, chinese.numerals)
        shouldThrow<IllegalArgumentException> {
            YearPageBuilder(YearCalendars(PERSIAN_FIRST), persian, texts, weekdayNames.drop(1))
        }
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a year page builds within a frame budget`() {
        val builder = builder()
        val days = YearCalendars(PERSIAN_FIRST).yearDays(0, 1405).map { YearDay(it, false, false) }
        repeat(WARM_UP_RUNS) { builder.build(0, 1405, today, days) }

        // Timing is sensitive to machine load: the best of several runs is compared with the budget.
        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { builder.build(0, 1405, today, days) } }

        best.toInt() shouldBeLessThan TimingTest.budget(BUDGET_MILLIS)
    }

    private companion object {
        val PERSIAN_ARITHMETIC = YearCalendars(PERSIAN_FIRST).arithmetic.first()
        const val WARM_UP_RUNS = 5
        const val MEASURED_RUNS = 10

        /** One frame at 60 Hz. */
        const val BUDGET_MILLIS = 16
    }

    @Test
    fun `a Hebrew leap year shows 13 named months and a common year 12`() {
        val hebrew = PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.HEBREW), languageCode = "en")
        val names = builder(hebrew, english)
        val leap = names.build(0, 5787, today, emptyList()).months.map { it.name }
        leap.size shouldBe 13
        leap.subList(5, 7) shouldContainExactly listOf("Adar I", "Adar II")
        leap.last() shouldBe "Elul"
        val common = names.build(0, 5786, today, emptyList()).months.map { it.name }
        common.size shouldBe 12
        common[5] shouldBe "Adar"
        val hindi = requireNotNull(LanguageTable.forCode("hi"))
        builder(hebrew.copy(languageCode = "hi"), hindi)
            .build(0, 5787, today, emptyList())
            .months
            .last()
            .name shouldBe "एलूल"
    }
}
