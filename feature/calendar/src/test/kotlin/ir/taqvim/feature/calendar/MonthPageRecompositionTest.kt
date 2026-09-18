/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.uitesting.RecompositionCounter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * BUG-2: selecting a day must recompose only the cells that changed. The page used to be wrapped in a `Crossfade`,
 * which composed all [MonthLayout.CELLS] cells a second time on every tap, and its callbacks were rebuilt with every
 * page, which made every cell recompose (T-1802 keeps `DayCell` itself skippable).
 */
@RunWith(RobolectricTestRunner::class)
class MonthPageRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    private val builder =
        MonthPageBuilder(
            CalendarCalendars(PERSIAN_FIRST),
            persian,
            texts,
            palette,
            listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"),
        )

    /** 21 Farvardin 1405. */
    private val today = Jdn(2461141)

    @Test
    fun `selecting another day recomposes only the two cells that changed`() {
        var page by mutableStateOf(builder.build(offset = 0, today = today, selected = today, events = null))
        RecompositionCounter(DAY_CELL).use { counter ->
            composeRule.setContent { CalendarTestTheme { MonthPageView(page, onAction = {}) } }
            composeRule.waitForIdle()
            val firstComposition = counter.count
            check(firstComposition <= MonthLayout.CELLS) { "$firstComposition cells composed at first" }

            page = builder.build(offset = 0, today = today, selected = Jdn(today.value + 1), events = null)
            composeRule.waitForIdle()
            val afterSelection = counter.count - firstComposition
            check(afterSelection <= MAX_CELLS_PER_SELECTION) {
                "selecting a day recomposed $afterSelection cells, at most $MAX_CELLS_PER_SELECTION expected"
            }
        }
    }

    private companion object {
        const val DAY_CELL = "ir.taqvim.core.ui.component.DayCell"

        /** The cell that loses the selection and the one that gains it. */
        const val MAX_CELLS_PER_SELECTION = 2
    }
}
