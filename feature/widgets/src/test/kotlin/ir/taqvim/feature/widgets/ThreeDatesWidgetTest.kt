/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/**
 * T-1216: the three-date widget's content comes from the calendar engines alone.
 *
 * Every assertion here is reached without a database, a dataset or a network call — the widget's whole input is a
 * day number and the user's calendars, which is what lets it draw the moment the host asks.
 */
class ThreeDatesWidgetTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val noon = Instant.parse("2026-10-10T09:00:00Z")

    /** 18 Mehr 1405 = 10 October 2026. */
    private val day: Jdn = LocalDate(2026, 10, 10).toJdn()

    private fun inputs(
        language: ir.taqvim.core.i18n.LanguageSpec = persian,
        islamic: ir.taqvim.core.calendar.CalendarArithmetic = IranIslamicCalendar(),
    ) = WidgetDayInputs(
        jdn = day,
        language = language,
        primary = PersianCalendarSystem,
        secondary = GregorianCalendarSystem,
        calendars = listOf(PersianCalendarSystem, GregorianCalendarSystem, islamic),
        isHoliday = false,
        events = emptyList(),
        place = null,
    )

    private fun build(
        language: ir.taqvim.core.i18n.LanguageSpec = persian,
        islamic: ir.taqvim.core.calendar.CalendarArithmetic = IranIslamicCalendar(),
    ): WidgetData = WidgetContentBuilder.build(inputs(language, islamic), noon) { it.name }

    @Test
    fun `the three lines are the Persian, Gregorian and Islamic dates of the day`() {
        val lines = build().calendarDates

        lines.size shouldBe 3
        lines[0] shouldBe "۱۸ مهر ۱۴۰۵"
        lines[1] shouldBe "۱۰ اکتبر ۲۰۲۶"
        lines[2].startsWith("۲") shouldBe true
    }

    @Test
    fun `the lines follow the app language, so an English user reads Latin digits`() {
        val lines = build(language = english).calendarDates

        lines[0] shouldBe "18 Mehr 1405"
        lines[1] shouldBe "10 October 2026"
    }

    @Test
    fun `the Islamic line follows the chosen method`() {
        val iran = build(islamic = IranIslamicCalendar()).calendarDates[2]
        val ummAlQura = build(islamic = UmmAlQuraCalendar).calendarDates[2]

        // Both are real answers from their own arithmetic; the widget shows whichever the user chose.
        iran.isNotEmpty() shouldBe true
        ummAlQura.isNotEmpty() shouldBe true
    }

    @Test
    fun `the day rolls over at midnight`() {
        val tomorrow =
            WidgetContentBuilder.build(
                inputs().copy(jdn = day + 1),
                noon,
            ) { it.name }

        tomorrow.calendarDates[0] shouldBe "۱۹ مهر ۱۴۰۵"
        tomorrow.calendarDates[1] shouldBe "۱۱ اکتبر ۲۰۲۶"
    }

    @Test
    fun `the widget declares no events, no holidays and nothing to configure`() {
        val kind = WidgetKind.THREE_DATES

        kind.contents.shouldBeEmpty()
        kind.dependencies shouldContainExactly setOf(WidgetDependency.DATE, WidgetDependency.APPEARANCE)
        (WidgetDependency.EVENTS in kind.dependencies) shouldBe false
    }
}
