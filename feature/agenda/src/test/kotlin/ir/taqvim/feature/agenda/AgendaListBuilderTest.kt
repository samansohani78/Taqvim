/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-901 rows: month headers in the primary calendar, days in the language's dates and digits, agenda filtering. */
class AgendaListBuilderTest {
    private fun build(
        settings: AgendaSettings,
        mode: AgendaMode,
        window: AgendaWindow = AgendaWindow.INITIAL,
        today: Jdn = TODAY,
        events: Map<Jdn, List<AgendaEvent>> = SAMPLE_EVENTS,
    ): AgendaList {
        val calendars = AgendaCalendars(settings)
        val days =
            calendars.range(today, window.first, window.last).map { AgendaDay(it, false, false, events[it].orEmpty()) }
        val builder = AgendaListBuilder(calendars, AgendaListBuilder.languageFor(settings.languageCode))
        return builder.build(today, window, days, mode)
    }

    @Test
    fun `the month list shows every day of Mordad to Aban 1405 under Persian headers`() {
        val list = build(PERSIAN_FA, AgendaMode.MONTH_LIST)
        val headers = list.items.filterIsInstance<AgendaMonthHeader>()
        headers.map { it.title.name } shouldContainExactly listOf("مرداد", "شهریور", "مهر", "آبان")
        headers.map { it.title.year }.toSet() shouldBe setOf("۱۴۰۵")
        headers.map { it.offset } shouldContainExactly listOf(-1, 0, 1, 2)
        list.items.filterIsInstance<AgendaDayRow>() shouldHaveSize 31 + 31 + 30 + 30
        list.items.first().shouldBeInstanceOf<AgendaMonthHeader>()

        val today = list.items[list.todayIndex].shouldBeInstanceOf<AgendaDayRow>()
        today.jdn shouldBe TODAY
        today.isToday.shouldBeTrue()
        today.dayNumber shouldBe "۲۲"
        today.longDate shouldBe "یکشنبه ۲۲ شهریور ۱۴۰۵"
        today.otherDates shouldHaveSize 2
        (list.items[list.todayIndex - 22] as AgendaMonthHeader).title.name shouldBe "شهریور"

        val shahrivar = headers[1]
        shahrivar.otherCalendars shouldHaveSize 2
        shahrivar.otherCalendars
            .first()
            .first.year shouldBe "۲۰۲۶"
        (shahrivar.otherCalendars.first().first == shahrivar.otherCalendars.first().last) shouldBe false
    }

    @Test
    fun `the agenda keeps days with events and today, and notes months without events`() {
        val list = build(GREGORIAN_EN, AgendaMode.AGENDA)
        val days = list.items.filterIsInstance<AgendaDayRow>()
        days.map { it.jdn } shouldContainExactly listOf(TODAY - 10, TODAY, TODAY + 2, TODAY + 5, TODAY + 40)
        days.first { it.jdn == TODAY + 2 }.events.map { it.title } shouldContainExactly
            listOf("Holiday event", "Birthday")
        days.first { it.jdn == TODAY }.longDate shouldBe "Sunday, September 13, 2026"
        days.first { it.jdn == TODAY }.dayNumber shouldBe "13"

        // August (offset -1) and November (offset 2) 2026 have no events.
        list.items.filterIsInstance<AgendaEmptyMonth>().map { it.offset } shouldContainExactly listOf(-1, 2)
        val headers = list.items.filterIsInstance<AgendaMonthHeader>()
        headers.first().title shouldBe MonthName("August", "2026")
        headers
            .first()
            .otherCalendars
            .single()
            .first.name shouldBe "Mordad"
    }

    @Test
    fun `holidays and weekends come from the events or from the language`() {
        val calendars = AgendaCalendars(GREGORIAN_EN)
        val window = AgendaWindow(0, 0)
        val days =
            calendars.range(TODAY, 0, 0).map { day ->
                AgendaDay(day, isHoliday = day == TODAY + 1, isWeekend = day == TODAY + 1, SAMPLE_EVENTS[day].orEmpty())
            }
        val rows =
            AgendaListBuilder(calendars, AgendaListBuilder.languageFor("en"))
                .build(TODAY, window, days, AgendaMode.MONTH_LIST)
                .items
                .filterIsInstance<AgendaDayRow>()
        rows.first { it.jdn == TODAY + 1 }.isHoliday.shouldBeTrue()
        rows.first { it.jdn == TODAY + 1 }.isWeekend.shouldBeTrue()
        rows.first { it.jdn == TODAY }.isWeekend shouldBe false

        // Days not loaded yet take the weekend from the language (en: Saturday and Sunday).
        val unloaded =
            AgendaListBuilder(calendars, AgendaListBuilder.languageFor("en"))
                .build(TODAY, window, emptyList(), AgendaMode.MONTH_LIST)
                .items
                .filterIsInstance<AgendaDayRow>()
        unloaded.first { it.jdn == TODAY }.isWeekend.shouldBeTrue()
        unloaded.first { it.jdn == TODAY + 1 }.isWeekend shouldBe false
    }

    @Test
    fun `unknown languages and unavailable calendars fall back`() {
        val settings = AgendaSettings(listOf(CalendarSystem.NEPALI), GREGORIAN_EN.islamicVariant, "xx")
        val list = build(settings, AgendaMode.AGENDA, AgendaWindow(0, 0))
        val header = list.items.first().shouldBeInstanceOf<AgendaMonthHeader>()
        header.otherCalendars shouldHaveSize 0
        AgendaListBuilder.languageFor("xx") shouldBe LanguageTable.languages.first()
        Numerals.parseLong(header.title.year) shouldBe 2026L
        list.items.filterIsInstance<AgendaDayRow>().map { it.jdn } shouldContainExactly
            listOf(TODAY - 10, TODAY, TODAY + 2, TODAY + 5)
    }

    @Test
    fun `rows are unique, in day order and grouped under their month for any window, mode and language`(): Unit =
        runBlocking {
            val arabic = AgendaSettings(listOf(CalendarSystem.ISLAMIC), PERSIAN_FA.islamicVariant, "ar")
            val settings = listOf(PERSIAN_FA, GREGORIAN_EN, arabic)
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-18_000..18_000),
                Arb.list(Arb.boolean(), 0..20),
                Arb.element(settings),
                Arb.element(AgendaMode.entries),
            ) { shift, steps, setting, mode ->
                val window = steps.fold(AgendaWindow.INITIAL) { w, later -> if (later) w.later() else w.earlier() }
                val today = TODAY + shift
                val list = build(setting, mode, window, today, mapOf(today + 3 to listOf(event("e", "E"))))
                list.items.map { it.key }.toSet() shouldHaveSize list.items.size
                val days = list.items.filterIsInstance<AgendaDayRow>().map { it.jdn }
                days.zipWithNext().all { (a, b) -> a < b }.shouldBeTrue()
                list.items.filterIsInstance<AgendaMonthHeader>().map { it.offset } shouldContainExactly
                    (window.first..window.last).toList()
                (list.todayIndex >= 0) shouldBe (0 in window)
            }
        }
}
