/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.component.MONTH_DAY_LIST_TAG
import ir.taqvim.core.uitesting.assertAccessible
import ir.taqvim.core.uitesting.assertLayoutFits
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * R10, T-1701: the month page keeps [MonthGrid][ir.taqvim.core.ui.component.MonthGrid] at the system font scales
 * PLAN's screenshot matrix already covers below the large-text tier (1.0, 1.3), and switches to
 * [MonthDayList][ir.taqvim.core.ui.component.MonthDayList] at the large-text tier (1.75, 2.0), identified here by
 * [MONTH_DAY_LIST_TAG]. At font scale 2.0 on a 320 dp-wide window — narrower than any supported phone — nothing is
 * clipped and the row keeps its accessible labels and touch targets.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthDisplayModeSwitchTest {
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
    private val page = builder.build(offset = 0, today = today, selected = today, events = null)

    private fun setContentAt(fontScale: Float) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                CalendarTestTheme { MonthPageView(page, onAction = {}) }
            }
        }
    }

    private fun listTagNodes() = composeRule.onAllNodes(hasTestTag(MONTH_DAY_LIST_TAG)).fetchSemanticsNodes()

    @Test
    fun `the grid is used at font scale 1_0`() {
        setContentAt(1.0f)
        listTagNodes().shouldBeEmpty()
    }

    @Test
    fun `the grid is used at font scale 1_3`() {
        setContentAt(1.3f)
        listTagNodes().shouldBeEmpty()
    }

    @Test
    fun `the list is used at font scale 1_75`() {
        setContentAt(1.75f)
        listTagNodes().shouldNotBeEmpty()
    }

    @Test
    fun `the list is used at font scale 2_0`() {
        setContentAt(2.0f)
        listTagNodes().shouldNotBeEmpty()
    }

    @Test
    fun `no text is clipped at font scale two on a 320dp window`() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(NARROW_TEST_SCALE)) {
                CalendarTestTheme { Box(Modifier.width(NARROW_WINDOW)) { MonthPageView(page, onAction = {}) } }
            }
        }
        composeRule.assertLayoutFits()
    }

    @Test
    fun `the list stays accessible at font scale two on a 320dp window`() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(NARROW_TEST_SCALE)) {
                CalendarTestTheme { Box(Modifier.width(NARROW_WINDOW)) { MonthPageView(page, onAction = {}) } }
            }
        }
        composeRule.assertAccessible()
    }

    private companion object {
        const val NARROW_TEST_SCALE = 2.0f
        val NARROW_WINDOW = 320.dp
    }
}
