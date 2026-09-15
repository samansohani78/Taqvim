/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PickerAndGridTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun hasState(state: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

    private fun setWheel(
        label: String,
        value: Float,
    ) {
        composeRule
            .onNodeWithContentDescription(label)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }

    @Test
    fun numberWheelIsOneAdjustableControl() {
        var value by mutableStateOf(5)
        composeRule.setContent {
            TestTheme { NumberWheel(value, 1..31, { value = it }, "Day", Modifier.width(96.dp), { "d$it" }) }
        }
        val wheel = composeRule.onNodeWithContentDescription("Day")
        wheel.assert(hasState("d5"))
        wheel.performSemanticsAction(SemanticsActions.SetProgress) { it(12f) }
        composeRule.waitForIdle()
        assertEquals(12, value)
        wheel.assert(hasState("d12"))
        wheel.performSemanticsAction(SemanticsActions.SetProgress) { it(Float.NaN) }
        composeRule.waitForIdle()
        assertEquals(1, value)
        value = 31
        composeRule.waitForIdle()
        wheel.assert(hasState("d31"))
        assertEquals(31, value)
        value = 10
        composeRule.waitForIdle()
        wheel.performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertTrue("swiping up moves to a later value, got $value", value > 10)
    }

    @Test
    fun numberWheelWorksAcrossEveryIntValue() {
        var value by mutableStateOf(1405)
        composeRule.setContent {
            TestTheme {
                NumberWheel(
                    value,
                    Int.MIN_VALUE..Int.MAX_VALUE,
                    { value = it },
                    "Year",
                    Modifier.width(96.dp),
                )
            }
        }
        val wheel = composeRule.onNodeWithContentDescription("Year")
        wheel.assert(hasState("1405"))
        wheel.performSemanticsAction(SemanticsActions.SetProgress) { it(1406f) }
        composeRule.waitForIdle()
        assertEquals(1406, value)
        value = 2_000_000_000
        composeRule.waitForIdle()
        wheel.assert(hasState("2000000000"))
        assertEquals(2_000_000_000, value)
        wheel.performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertTrue("swiping up moves to a later year, got $value", value > 2_000_000_000)
        value = Int.MIN_VALUE
        composeRule.waitForIdle()
        wheel.assert(hasState(Int.MIN_VALUE.toString()))
        assertEquals(Int.MIN_VALUE, value)
    }

    @Test
    fun datePickerClampsTheDayAndConfirms() {
        var confirmed: DateSelection? = null
        var dismissed = 0
        composeRule.setContent {
            TestTheme { DatePickerContent(samplePicker(), { confirmed = it }, { dismissed++ }) }
        }
        composeRule.onNodeWithContentDescription("Day").assert(hasState("30"))
        composeRule.onNodeWithContentDescription("Month").assert(hasState("Month 12"))
        setWheel("Year", 1404f)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Day").assert(hasState("29"))
        setWheel("Month", 1f)
        setWheel("Day", 31f)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(DateSelection(1404, 1, 31), confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun datePickerSheetShowsThePickerInABottomSheet() {
        var confirmed: DateSelection? = null
        var dismissed = 0
        composeRule.setContent {
            TestTheme { DatePickerSheet(samplePicker(DateSelection(1405, 1, 1)), { confirmed = it }, { dismissed++ }) }
        }
        composeRule.onNodeWithText("Go to date").assertExists()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(DateSelection(1405, 1, 1), confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun monthGridRoutesDayAndWeekActions() {
        var clicked = -1
        var longClicked = -1
        var week = -1
        composeRule.setContent {
            TestTheme {
                MonthGrid(
                    sampleMonth(),
                    onDayClick = { clicked = it },
                    modifier = Modifier.width(392.dp).height(320.dp),
                    onDayLongClick = { longClicked = it },
                    onWeekClick = { week = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Day 10").performClick()
        composeRule.onNodeWithContentDescription("Day 12").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Week 3").performClick()
        assertEquals(10, clicked)
        assertEquals(12, longClicked)
        assertEquals(2, week)
        val first = composeRule.onNodeWithContentDescription("Day 0").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithContentDescription("Day 1").getUnclippedBoundsInRoot()
        assertTrue(first.left < second.left)
        assertEquals((first.right - first.left).value, (second.right - second.left).value, 0.5f)
    }

    @Test
    fun monthGridMirrorsInRtlAndMakesSquareCellsWhenHeightIsUnbounded() {
        composeRule.setContent {
            TestTheme(rtl = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MonthGrid(sampleMonth().copy(weekNumbers = null), {}, Modifier.width(350.dp))
                }
            }
        }
        val first = composeRule.onNodeWithContentDescription("Day 0").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithContentDescription("Day 1").getUnclippedBoundsInRoot()
        assertTrue(first.left > second.left)
        assertEquals((first.right - first.left).value, (first.bottom - first.top).value, 1f)
        composeRule.onNodeWithContentDescription("Week 1").assertDoesNotExist()
    }

    @Test
    fun monthGridLayoutMeasuresEveryChildOncePerPass() {
        val columns = 7
        val rows = 2
        val children = columns + rows * (columns + 1)
        val counts = IntArray(children)
        val fixed = BooleanArray(children)
        var width by mutableStateOf(300.dp)
        composeRule.setContent {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                MonthGridLayout(columns, rows, hasWeekColumn = true, modifier = Modifier.width(width)) {
                    repeat(children) { index ->
                        Box(
                            Modifier.layout { measurable, constraints ->
                                counts[index]++
                                fixed[index] = constraints.hasFixedWidth && constraints.hasFixedHeight
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            },
                        )
                    }
                }
                MonthGridLayout(columns, 1, hasWeekColumn = false) { repeat(columns * 2) { Box(Modifier) } }
            }
        }
        composeRule.waitForIdle()
        assertEquals(List(children) { 1 }, counts.toList())
        assertTrue(fixed.drop(columns).all { it })
        width = 340.dp
        composeRule.waitForIdle()
        // Week numbers keep their fixed 32 dp constraints, so Compose skips them; every other child is measured again.
        val weekColumn = setOf(columns, 2 * columns + 1)
        assertEquals(List(children) { if (it in weekColumn) 1 else 2 }, counts.toList())
    }
}
