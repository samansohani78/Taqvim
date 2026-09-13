/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1302 UI: descriptions for TalkBack, actions, states and the orientation lock. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompassScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun show(
        state: CompassUiState,
        actions: CompassActions = CompassActions(),
    ) {
        composeRule.setContent { CompassTestTheme { CompassScreen(state, actions, animatePath = false) } }
    }

    @Test
    fun dialDescribesHeadingQiblaSunAndMoon() {
        show(CompassFixtures.dial(heading = 40.0))
        composeRule.onNodeWithText("Compass").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Heading 45 degrees, northeast").assertIsDisplayed()
        composeRule.onNodeWithText("45° northeast").assertIsDisplayed()
        composeRule.onNodeWithText("Relative to true north").assertIsDisplayed()
        composeRule.onNodeWithText("Qibla", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("right", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("above the horizon", substring = true).assertCountEquals(2)
        composeRule.onNodeWithText("Moon", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Low accuracy", substring = true).assertDoesNotExist()
    }

    @Test
    fun actionsAndCalibrationHint() {
        val calls = mutableListOf<String>()
        val actions = CompassActions(onToggleFrozen = { calls += "stop" }, onToggleSunPath = { calls += "path" })
        show(
            CompassFixtures.dial(accuracy = CompassAccuracy.UNRELIABLE, showSunPath = true).copy(frozen = true),
            actions,
        )
        composeRule.onNodeWithText("Low accuracy", substring = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("Stop")
            .performScrollTo()
            .assertIsSelected()
            .performClick()
        composeRule
            .onNodeWithText("Sun path")
            .performScrollTo()
            .assertIsSelected()
            .performClick()
        assertEquals(listOf("stop", "path"), calls)
    }

    @Test
    fun withoutAPlaceTheHeadingIsMagnetic() {
        show(CompassFixtures.dial(place = null, heading = 200.0))
        composeRule.onNodeWithText("Relative to magnetic north").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a place", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sun path").assertDoesNotExist()
    }

    @Test
    fun loadingAndMissingSensors() {
        var state by mutableStateOf(CompassUiState())
        composeRule.setContent { CompassTestTheme { CompassScreen(state, CompassActions()) } }
        composeRule.onNodeWithContentDescription("Starting the compass").assertExists()
        state = CompassUiState(CompassContent.SensorUnavailable)
        composeRule.onNodeWithText("No compass sensor").assertIsDisplayed()
    }

    @Test
    fun theRouteLocksTheScreenOrientationWhileShown() {
        val viewModel =
            CompassViewModel(
                settingsSource = { flowOf(CompassFixtures.settings()) },
                sensors = FakeSensors(orientation = flowOf(CompassFixtures.reading(40.0))),
                declination = CompassFixtures.FIVE_EAST,
                clock = FakeClock(CompassFixtures.NOON),
            )
        val activity = composeRule.activity
        val before = activity.requestedOrientation
        var shown by mutableStateOf(true)
        composeRule.setContent { CompassTestTheme { if (shown) CompassRoute(viewModel = viewModel) } }
        composeRule.onNodeWithText("Relative to true north").assertIsDisplayed()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR, activity.requestedOrientation)
        composeRule.onNodeWithText("Stop").performScrollTo().performClick()
        composeRule.onNodeWithText("Stop").assertIsSelected()
        shown = false
        composeRule.waitForIdle()
        assertEquals(before, activity.requestedOrientation)
    }
}
