/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.Test

/** T-303 with synthetic events; weekends come from the T-200 language table (CLDR). */
class HolidayCalendarTest {
    private val newYear =
        event("test.new-year", CalendarSystem.PERSIAN, EventRule.Fixed(1, 1), isHoliday = true)
            .copy(source = EventSource.IRAN_OFFICIAL)
    private val secondDay = event("test.second-day", CalendarSystem.PERSIAN, EventRule.Fixed(1, 2))
    private val gregorianHoliday =
        event("test.january-first", CalendarSystem.GREGORIAN, EventRule.Fixed(1, 1), isHoliday = true)
    private val alwaysShown =
        event("test.always-shown", CalendarSystem.PERSIAN, EventRule.Fixed(1, 3), isHoliday = true)
            .copy(flags = setOf(EventFlag.ALWAYS_DISPLAYED))
    private val lookup = EventLookup(listOf(newYear, secondDay, gregorianHoliday, alwaysShown))

    private fun persian(
        month: Int,
        day: Int,
    ): Jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, month, day))

    private fun gregorian(
        year: Int,
        month: Int,
        day: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day))

    @Test
    fun `holidays and their reasons come from enabled sources only`() {
        val iranOnly = HolidayCalendar(lookup, setOf(EventSource.IRAN_OFFICIAL), weekend = setOf(Weekday.FRIDAY))

        iranOnly.holidayReasons(persian(1, 1)).map { it.definition.id.value } shouldBe listOf("test.new-year")
        iranOnly.isHoliday(persian(1, 1)) shouldBe true
        iranOnly.isHoliday(persian(1, 2)) shouldBe false
        iranOnly.holidayReasons(persian(1, 3)).shouldBeEmpty()
        iranOnly.isHoliday(gregorian(2026, 1, 1)) shouldBe false

        val bothSources = setOf(EventSource.IRAN_OFFICIAL, EventSource.INTERNATIONAL)
        val both = HolidayCalendar(lookup, bothSources, setOf(Weekday.FRIDAY))
        both.isHoliday(gregorian(2026, 1, 1)) shouldBe true
        both.isHoliday(persian(1, 3)) shouldBe true
    }

    @Test
    fun `weekends and workdays follow the configured weekend`() {
        val calendar = HolidayCalendar(lookup, setOf(EventSource.IRAN_OFFICIAL), weekend = setOf(Weekday.FRIDAY))
        val friday = gregorian(2026, 3, 20)
        val saturdayNewYear = persian(1, 1)
        val sunday = persian(1, 2)

        friday.weekday() shouldBe Weekday.FRIDAY
        calendar.isWeekend(friday) shouldBe true
        calendar.isWorkday(friday) shouldBe false
        calendar.isWeekend(saturdayNewYear) shouldBe false
        calendar.isWorkday(saturdayNewYear) shouldBe false
        calendar.isWorkday(sunday) shouldBe true
        HolidayCalendar(lookup, emptySet(), emptySet()).isWorkday(saturdayNewYear) shouldBe true
    }

    @Test
    fun `weekends by language come from the CLDR language table`() {
        val expected =
            mapOf(
                "fa" to setOf(Weekday.FRIDAY),
                "prs" to setOf(Weekday.THURSDAY, Weekday.FRIDAY),
                "ar" to setOf(Weekday.FRIDAY, Weekday.SATURDAY),
                "en" to setOf(Weekday.SATURDAY, Weekday.SUNDAY),
                "hi" to setOf(Weekday.SUNDAY),
            )

        expected.forEach { (code, weekend) ->
            val language = requireNotNull(LanguageTable.forCode(code)) { "language $code is missing" }
            HolidayCalendar.forLanguage(lookup, setOf(EventSource.IRAN_OFFICIAL), language).weekend shouldBe weekend
        }
    }

    @Test
    fun `a holiday a law created is one only in the years the law covers`() {
        // 8 Rabi al-Awwal became an Iranian holiday in AH 1440 and 2 Shawwal in AH 1433: the official calendars of
        // the years before print those days without (تعطیل). Holiday determination does not go through
        // EventVisibilityPolicy, so it has to apply the validity itself (T-303, D-02).
        val fromYear = 1440
        val lawful =
            event("test.law-made-holiday", CalendarSystem.ISLAMIC, EventRule.Fixed(3, 8), isHoliday = true)
                .copy(
                    source = EventSource.IRAN_OFFICIAL,
                    validity = Validity(CalendarSystem.ISLAMIC, fromYear, null, Citation("https://example.org", "t")),
                )
        val calendar =
            HolidayCalendar(EventLookup(listOf(lawful)), setOf(EventSource.IRAN_OFFICIAL), weekend = emptySet())
        val islamic = CalendarProvider.DEFAULT.calendarFor(CalendarSystem.ISLAMIC)!!

        val before = islamic.toJdn(CalendarDate(CalendarSystem.ISLAMIC, fromYear - 1, 3, 8))
        val first = islamic.toJdn(CalendarDate(CalendarSystem.ISLAMIC, fromYear, 3, 8))
        val later = islamic.toJdn(CalendarDate(CalendarSystem.ISLAMIC, fromYear + 3, 3, 8))

        calendar.isHoliday(before) shouldBe false
        calendar.holidayReasons(before).shouldBeEmpty()
        calendar.isHoliday(first) shouldBe true
        calendar.isHoliday(later) shouldBe true
    }
}
