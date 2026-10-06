/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.longs.shouldBeLessThan
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.TimingTest
import ir.taqvim.core.workdays.ShiftRotation
import ir.taqvim.core.workdays.ShiftType
import kotlin.system.measureNanoTime
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Budget of one month page's arithmetic (ADR-0018 addendum): three calendars per cell, the week-number column
 * (A-08) and a shift rotation (F-08), which is the heaviest page a user can configure.
 *
 * A swipe builds three of these, so the whole page has to stay well inside a frame even though it runs off the main
 * thread (BUG-2) — the pager's prefetch has to finish before the user reaches the page.
 */
class MonthPageTimingTest {
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
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
    private val today = gregorian(2026, 4, 10)
    private val rotation =
        ShiftRotation(
            id = 1,
            name = "Nights",
            anchor = today,
            pattern = listOf(ShiftType("D"), ShiftType("D"), ShiftType("N"), ShiftType("N"), ShiftType("-")),
        )
    private val builder =
        MonthPageBuilder(
            calendars =
                CalendarCalendars(
                    PERSIAN_FIRST.copy(
                        calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC),
                        showWeekNumbers = true,
                    ),
                ),
            language = persian,
            texts = texts,
            palette = palette,
            weekdayNames = isoWeekdays,
            rotations = listOf(rotation),
        )

    @Test
    @Tag(TimingTest.TAG)
    fun `a month page with three calendars, week numbers and a rotation builds within a frame`() {
        repeat(WARM_UP_PAGES) { builder.build(it, today, today, null) }

        val best =
            (1..MEASURED_RUNS).minOf {
                measureNanoTime { PAGES.forEach { page -> builder.build(page, today, today, null) } }
            }
        val perPage = best / PAGES.count() / NANOS_PER_MICRO

        println("T-801 month page (3 calendars, week numbers, 1 rotation) best of $MEASURED_RUNS: $perPage us")
        perPage shouldBeLessThan TimingTest.budget(BUDGET_MICROS)
    }

    private companion object {
        /** A year either way, so no page is served from a calendar's own month cache alone. */
        val PAGES = -12..12
        const val WARM_UP_PAGES = 24
        const val MEASURED_RUNS = 7
        const val NANOS_PER_MICRO = 1_000L

        /** A third of a 60 Hz frame: a swipe builds three pages. */
        const val BUDGET_MICROS = 5_000L
    }
}
