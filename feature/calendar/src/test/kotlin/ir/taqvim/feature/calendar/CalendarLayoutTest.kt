/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DateSelection
import org.junit.jupiter.api.Test

/** T-806 window layouts and the T-803 go-to-date picker model. */
class CalendarLayoutTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val labels = DatePickerLabels("title", "year", "month", "day", "confirm", "cancel")

    @Test
    fun `compact widths stack the panes and medium or expanded widths put them side by side`() {
        (0 until MEDIUM_WIDTH_DP).forEach { calendarLayoutFor(it, isTabletop = false) shouldBe CalendarLayout.STACKED }
        listOf(MEDIUM_WIDTH_DP, 673, 840, 1280, 2560).forEach {
            calendarLayoutFor(it, isTabletop = false) shouldBe CalendarLayout.TWO_PANE
        }
    }

    @Test
    fun `a foldable in tabletop posture puts the month above the fold at any width`() {
        listOf(320, MEDIUM_WIDTH_DP, 673, 1280).forEach {
            calendarLayoutFor(it, isTabletop = true) shouldBe CalendarLayout.TABLETOP
        }
    }

    @Test
    fun `the go-to-date picker starts at the selected day in the primary calendar`() {
        val content = DayDetailsSamples.content("fa")
        val model = goToDateModel(content, labels)

        model.initial shouldBe DateSelection(1405, 1, 1)
        model.years shouldBe CalendarLimits.pagedYears(PersianCalendarSystem, content.today)
        model.years.last shouldBe
            PersianCalendarSystem.fromJdn(content.today).year + CalendarLimits.MAX_MONTH_OFFSET / 12 - 1
        model.monthNames shouldBe persian.monthNames.persian
        model.daysInMonth(1405, 1) shouldBe 31
        model.daysInMonth(1405, 12) shouldBe PersianCalendarSystem.monthLength(1405, 12)
        model.daysInMonth(1405, 13) shouldBe PersianCalendarSystem.monthLength(1405, 12)
        model.formatNumber(1405) shouldBe Numerals.format(1405L, persian.numerals)
        model.labels shouldBe labels
    }

    @Test
    fun `month names fall back to English names`() {
        val kurdish = requireNotNull(LanguageTable.forCode("ckb"))

        monthNamesOf(english, CalendarSystem.ISLAMIC) shouldBe english.monthNames.islamic
        monthNamesOf(kurdish, CalendarSystem.PERSIAN) shouldBe english.monthNames.persian
        monthNamesOf(persian, CalendarSystem.NEPALI).first() shouldBe "Baishakh"
    }
}
