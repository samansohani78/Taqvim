/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.HebrewCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1212 countdown texts and the configuration screen's countdown options. */
class WidgetCountdownBuilderTest {
    private val en = requireNotNull(LanguageTable.forCode("en"))
    private val fa = requireNotNull(LanguageTable.forCode("fa"))

    /** 22 Shahrivar 1405. */
    private val today = LocalDate(2026, 9, 13).toJdn()

    private fun gregorian(
        month: Int,
        day: Int,
        title: String = "",
    ) = WidgetCountdown(CalendarSystem.GREGORIAN, 2026, month, day, startJdn = today.value, title = title)

    @Test
    fun `days left are split into weeks and days`() {
        val trip = WidgetCountdownBuilder.view(gregorian(9, 30, "  Trip "), GregorianCalendarSystem, today, en)
        trip.title shouldBe "Trip"
        trip.status shouldBe CountdownStatus.UPCOMING
        trip.headline shouldBe CountdownPart(CountdownUnit.DAYS, 17, "17")
        trip.parts shouldBe
            listOf(CountdownPart(CountdownUnit.WEEKS, 2, "2"), CountdownPart(CountdownUnit.DAYS, 3, "3"))
        trip.date shouldBe LocalDate(2026, 9, 30)

        val week = WidgetCountdownBuilder.view(gregorian(9, 20), GregorianCalendarSystem, today, en)
        week.parts shouldBe listOf(CountdownPart(CountdownUnit.WEEKS, 1, "1"))
        week.title shouldBe "20 September 2026"
        WidgetCountdownBuilder.view(gregorian(9, 18), GregorianCalendarSystem, today, en).parts.shouldBeEmpty()
        WidgetCountdownBuilder.view(gregorian(9, 13), GregorianCalendarSystem, today, en).status shouldBe
            CountdownStatus.TODAY
    }

    @Test
    fun `time since is told in years, months and days in the language's digits`() {
        val birthday = WidgetCountdown(CalendarSystem.PERSIAN, 1370, 6, 15, startJdn = 0, mode = CountdownMode.SINCE)
        val age = WidgetCountdownBuilder.view(birthday, PersianCalendarSystem, today, fa)

        age.status shouldBe CountdownStatus.ELAPSED
        age.headline shouldBe CountdownPart(CountdownUnit.YEARS, 35, "۳۵")
        age.parts shouldBe
            listOf(CountdownPart(CountdownUnit.YEARS, 35, "۳۵"), CountdownPart(CountdownUnit.DAYS, 7, "۷"))
        age.title shouldBe "۱۵ شهریور ۱۳۷۰"

        val recent = birthday.copy(year = 1405, month = 6, day = 12)
        val days = WidgetCountdownBuilder.view(recent, PersianCalendarSystem, today, fa)
        days.headline shouldBe CountdownPart(CountdownUnit.DAYS, 10, "۱۰")
        days.parts.shouldBeEmpty()
        val months = WidgetCountdownBuilder.view(recent.copy(month = 4), PersianCalendarSystem, today, en)
        months.headline.unit shouldBe CountdownUnit.MONTHS
    }

    @Test
    fun `options start at today, convert between calendars and describe the chosen date`() {
        val occasion = WidgetOccasion("Nowruz", CalendarSystem.PERSIAN, 1406, 1, 1, true, "1 Farvardin 1406")
        val options =
            WidgetCountdownOptions(today, en, listOf(PersianCalendarSystem, GregorianCalendarSystem), listOf(occasion))

        val start = options.defaultCountdown()
        start shouldBe WidgetCountdown(CalendarSystem.PERSIAN, 1405, 6, 22, startJdn = today.value)
        options.inCalendar(start, CalendarSystem.GREGORIAN) shouldBe
            start.copy(calendar = CalendarSystem.GREGORIAN, year = 2026, month = 9, day = 13)
        options.inCalendar(start, CalendarSystem.NEPALI) shouldBe start
        options.arithmetic(CalendarSystem.NEPALI) shouldBe PersianCalendarSystem

        val choices = options.choices(start)
        choices.dateText shouldBe "22 Shahrivar 1405"
        choices.calendars shouldBe listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN)
        choices.monthNames.first() shouldBe "Farvardin"
        choices.monthNames.size shouldBe 12
        choices.years shouldBe CalendarLimits.years(PersianCalendarSystem)
        choices.occasions shouldBe listOf(occasion)
        choices.numerals shouldBe en.numerals
        shouldThrow<IllegalArgumentException> { WidgetCountdownOptions(today, en, emptyList(), emptyList()) }
    }

    @Test
    fun `Hebrew countdown choices follow the year's months`() {
        val options =
            WidgetCountdownOptions(today, en, listOf(HebrewCalendarSystem, GregorianCalendarSystem), emptyList())
        val start = options.defaultCountdown()
        start.calendar shouldBe CalendarSystem.HEBREW
        start.year shouldBe 5787

        val choices = options.choices(start)
        choices.monthNames.size shouldBe 13
        choices.monthNames[5] shouldBe "Adar I"
        choices.monthNamesIn(5786).size shouldBe 12
        choices.monthNamesIn(5786)[5] shouldBe "Adar"
        val hindi = requireNotNull(LanguageTable.forCode("hi"))
        WidgetCountdownOptions(today, hindi, listOf(HebrewCalendarSystem), emptyList())
            .choices(start)
            .monthNamesIn(5787)
            .last() shouldBe "एलूल"
    }
}
