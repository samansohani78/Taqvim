/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * BUG-2: a day cell no longer uses `combinedClickable`, so the taps, long presses, keys, pressed state layer and
 * accessibility actions it gave for free are checked here (T-1700).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class DayCellInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0
    private var longClicks = 0

    private fun setCell(longClick: Boolean = true) {
        composeRule.setContent {
            TestTheme {
                DayCell(
                    DayCellModel(dayLabel = "7", contentDescription = "Day 7"),
                    onClick = { clicks++ },
                    modifier = Modifier.size(64.dp),
                    onLongClick = if (longClick) ({ longClicks++ }) else null,
                    longClickLabel = "New event",
                )
            }
        }
    }

    @Test
    fun aTapClicksAndALongPressLongClicks() {
        setCell()
        val cell = composeRule.onNodeWithContentDescription("Day 7")

        cell.performClick()
        cell.performTouchInput { longClick() }

        clicks shouldBe 1
        longClicks shouldBe 1
    }

    @Test
    fun accessibilityServicesSeeAButtonWithAClickAndALabelledLongClick() {
        setCell()
        val node = composeRule.onNodeWithContentDescription("Day 7").fetchSemanticsNode()

        node.config[SemanticsProperties.Role] shouldBe Role.Button
        node.config[SemanticsActions.OnLongClick].label shouldBe "New event"
        composeRule.onNodeWithContentDescription("Day 7").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithContentDescription("Day 7").performSemanticsAction(SemanticsActions.OnLongClick)
        clicks shouldBe 1
        longClicks shouldBe 1
    }

    @Test
    fun aCellWithoutALongClickOffersNone() {
        setCell(longClick = false)
        val node = composeRule.onNodeWithContentDescription("Day 7").fetchSemanticsNode()

        node.config.contains(SemanticsActions.OnLongClick) shouldBe false
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun enterAndTheDPadCentreClickTheFocusedCell() {
        setCell()
        val cell = composeRule.onNodeWithContentDescription("Day 7")

        cell.requestFocus()
        cell.performKeyInput { pressKey(Key.Enter) }
        cell.performKeyInput { pressKey(Key.DirectionCenter) }
        cell.performKeyInput { pressKey(Key.Spacebar) }

        clicks shouldBe 2
    }

    @Test
    fun aPressIsShownAsAStateLayerUntilRelease() {
        setCell()
        val cell = composeRule.onNodeWithContentDescription("Day 7")
        // Low in the middle of the cell: inside its rounded shape and clear of the day number.
        val probe = { cell.captureToImage().toPixelMap().let { it[it.width / 2, it.height * 9 / 10] } }
        val idle = probe()

        cell.performTouchInput { down(center) }
        val pressed = probe()
        cell.performTouchInput { up() }
        val released = probe()

        pressed shouldNotBe idle
        released shouldBe idle
    }
}
