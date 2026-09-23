/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.uitesting.assertLayoutFits
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * R10, T-1701: [MonthDayList] routes the same actions as [MonthGrid], stacks its days as full-width rows instead of
 * a grid, honours the caller's real font scale (nothing here coerces it, unlike a grid cell), keeps a 48 dp minimum
 * touch target and does not clip any label on a narrow window at font scale 2.0. [MonthDayList] is a `LazyColumn`, so
 * a row past the initial viewport is scrolled to by index (`performScrollToIndex`) before it is queried.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthDayListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun monthDayListRoutesDayAndWeekActions() {
        var clicked = -1
        var longClicked = -1
        var week = -1
        composeRule.setContent {
            TestTheme {
                MonthDayList(
                    sampleMonth(),
                    onDayClick = { clicked = it },
                    onDayLongClick = { longClicked = it },
                    onWeekClick = { week = it },
                )
            }
        }
        // sampleMonth() has 7 columns, so a week's block is one header item followed by its 7 days.
        scrollTo(flatDayIndex(cellIndex = 10))
        composeRule.onNodeWithContentDescription("Day 10").performClick()
        scrollTo(flatDayIndex(cellIndex = 12))
        composeRule.onNodeWithContentDescription("Day 12").performTouchInput { longClick() }
        scrollTo(flatWeekHeaderIndex(row = 2))
        composeRule.onNodeWithContentDescription("Week 3").performClick()
        clicked shouldBe 10
        longClicked shouldBe 12
        week shouldBe 2
    }

    @Test
    fun monthDayListStacksRowsFullWidthInsteadOfAGrid() {
        composeRule.setContent {
            TestTheme { MonthDayList(sampleMonth().copy(weekNumbers = null), {}, Modifier.width(NORMAL_WIDTH)) }
        }
        val first = composeRule.onNodeWithContentDescription("Day 0").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithContentDescription("Day 1").getUnclippedBoundsInRoot()
        // A grid places same-row cells side by side (equal top, increasing left, `PickerAndGridTest`); a list stacks
        // them instead (equal left, increasing top).
        assertTrue("consecutive days are stacked, not side by side", second.top >= first.bottom)
        second.left shouldBe first.left
    }

    @Test
    fun monthDayListHasAFortyEightDpMinimumTouchTarget() {
        composeRule.setContent {
            TestTheme { MonthDayList(sampleMonth(), onDayClick = {}, onWeekClick = {}) }
        }
        composeRule.onNodeWithContentDescription("Day 5").assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        composeRule.onNodeWithContentDescription("Week 1").assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun monthDayListAtDefaultScaleMatchesTheDocumentedBaseline() {
        composeRule.setContent {
            TestTheme { MonthDayList(busyRowModel(), {}, Modifier.width(NORMAL_WIDTH)) }
        }
        // BASELINE_AT_SCALE_ONE and the font-scale-two threshold in `monthDayListHonoursTheRealFontScaleWithNoCap`
        // are both measured from this exact four-line cell (day number, two secondary dates and a shift label, so
        // its height is dominated by real text content rather than the row's 48 dp touch-target floor).
        busyRowHeight() shouldBe (BASELINE_AT_SCALE_ONE plusOrMinus TOLERANCE)
    }

    @Test
    fun monthDayListHonoursTheRealFontScaleWithNoCap() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(SCREENSHOT_LARGE_SCALE)) {
                TestTheme { MonthDayList(busyRowModel(), {}, Modifier.width(NORMAL_WIDTH)) }
            }
        }
        val largeHeight = busyRowHeight()

        // MonthGrid caps a cell's density at MAX_CELL_FONT_SCALE (1.3); a grid-style cap on this same row would top
        // out at BASELINE_AT_SCALE_ONE * 1.3. The list must grow well past that, proving nothing here caps the scale.
        largeHeight shouldBeGreaterThan BASELINE_AT_SCALE_ONE * MAX_CELL_FONT_SCALE
    }

    /** A four-line cell (day, two secondary dates, a shift label) whose height is content-, not floor-, dominated. */
    private fun busyRowModel(): MonthGridModel =
        MonthGridModel(
            weekdayLabels = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"),
            cells =
                List(DAYS_PER_WEEK) { index ->
                    DayCellModel(
                        dayLabel = "23",
                        contentDescription = "Busy $index",
                        secondaryLabels = listOf("11", "1447"),
                        shiftLabel = "Shift",
                    )
                },
        )

    private fun busyRowHeight(): Float {
        val bounds = composeRule.onNodeWithContentDescription("Busy 0").getUnclippedBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    @Test
    fun monthDayListDoesNotClipTextOnA320dpWindowAtFontScaleTwo() {
        val busyDay =
            DayCellModel(
                dayLabel = "23",
                contentDescription = "Busy day",
                secondaryLabels = listOf("11", "1447"),
                indicators = listOf(Color.Red, Color.Blue, Color.Green),
                shiftLabel = "Shift reminder",
                isHoliday = true,
            )
        val model =
            MonthGridModel(
                weekdayLabels = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"),
                cells = List(NARROW_MONTH_CELLS) { busyDay },
            )
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(SCREENSHOT_LARGE_SCALE)) {
                TestTheme { MonthDayList(model, {}, Modifier.width(NARROW_WINDOW)) }
            }
        }
        composeRule.assertLayoutFits()
    }

    private fun scrollTo(index: Int) {
        composeRule.onNodeWithTag(MONTH_DAY_LIST_TAG).performScrollToIndex(index)
    }

    /** [MonthDayList]'s flat `LazyColumn` position of the day at [cellIndex] in a 7-column, week-numbered month. */
    private fun flatDayIndex(cellIndex: Int): Int {
        val row = cellIndex / DAYS_PER_WEEK
        val column = cellIndex % DAYS_PER_WEEK
        return row * (DAYS_PER_WEEK + 1) + 1 + column
    }

    /** [MonthDayList]'s flat `LazyColumn` position of the week-number header of [row]. */
    private fun flatWeekHeaderIndex(row: Int): Int = row * (DAYS_PER_WEEK + 1)

    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
        val NORMAL_WIDTH = 360.dp
        val NARROW_WINDOW = 320.dp
        const val SCREENSHOT_LARGE_SCALE = 2.0f
        const val NARROW_MONTH_CELLS = 7
        const val DAYS_PER_WEEK = 7

        // Measured once from this test's own theme, for the four-line "Busy" cell (see the baseline test above).
        const val BASELINE_AT_SCALE_ONE = 88.0f
        const val TOLERANCE = 2.0f
    }
}
