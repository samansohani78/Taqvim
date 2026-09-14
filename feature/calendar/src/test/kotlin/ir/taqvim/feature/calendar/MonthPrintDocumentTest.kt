/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import org.junit.jupiter.api.Test

/** T-803 "print month": the printable document of a month page. */
class MonthPrintDocumentTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val english = requireNotNull(LanguageTable.forCode("en"))
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
    private val printTexts = MonthPrintTexts(events = "MONTH EVENTS", noEvents = "NO EVENTS", separator = " | ")
    private val palette = IndicatorPalette(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)
    private val isoWeekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)

    private fun document(
        language: LanguageSpec,
        events: (CalendarCalendars) -> List<CalendarDay>,
    ): Pair<MonthPage, String> {
        val calendars = CalendarCalendars(PERSIAN_FIRST.copy(languageCode = language.code))
        val days = events(calendars)
        val page = MonthPageBuilder(calendars, language, texts, palette, isoWeekdays).build(0, today, today, days)
        return page to MonthPrintDocument.html(page, days, calendars, language, printTexts)
    }

    @Test
    fun `a Persian month prints right to left with its grid and the events of its own days`() {
        val nowruz = gregorian(2026, 3, 21)
        val (page, html) =
            document(persian) { calendars ->
                val grid = MonthLayout.gridDays(calendars.monthStartAt(today, 0), PERSIAN_FIRST.weekStart).toList()
                grid.map { day ->
                    val events =
                        when (day) {
                            nowruz -> listOf(DayEventItem("nowruz", DayEventKind.OFFICIAL, "Nowruz <&>", true))
                            grid.last() -> listOf(DayEventItem("later", DayEventKind.PERSONAL, "Next month", false))
                            else -> emptyList()
                        }
                    CalendarDay(day, day == nowruz, isWeekend = false, events)
                }
            }

        html shouldStartWith "<!DOCTYPE html><html lang=\"${persian.localeTag}\" dir=\"rtl\">"
        html shouldContain "<h1>${page.heading.title}</h1>"
        html shouldContain "<p class=\"sub\">"
        Regex("<th>").findAll(html).count() shouldBe MonthLayout.DAYS_PER_WEEK
        Regex("<td").findAll(html).count() shouldBe page.grid.cells.size
        Regex("<td class=\"out").findAll(html).count() shouldBe page.grid.cells.count { !it.inCurrentMonth }
        val nowruzDate = PersianCalendarSystem.fromJdn(nowruz)
        val longDate = DateFormatter.format(nowruzDate, nowruz.weekday(), persian, DateStyle.LONG)
        html shouldContain "<h2>MONTH EVENTS</h2><ul><li class=\"holiday\">$longDate: Nowruz &lt;&amp;&gt;</li></ul>"
        html shouldNotContain "Nowruz <&>"
        html shouldNotContain "Next month"
    }

    @Test
    fun `a month without events says so and a left-to-right language prints left to right`() {
        val (_, html) = document(english) { emptyList() }

        html shouldStartWith "<!DOCTYPE html><html lang=\"${english.localeTag}\" dir=\"ltr\">"
        html shouldContain "<h2>MONTH EVENTS</h2><p>NO EVENTS</p>"
        html shouldNotContain "<ul>"
    }

    @Test
    fun `text is escaped for element content and attributes`() {
        MonthPrintDocument.escape("<a href=\"x\">'&'</a>") shouldBe
            "&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;"
    }
}
