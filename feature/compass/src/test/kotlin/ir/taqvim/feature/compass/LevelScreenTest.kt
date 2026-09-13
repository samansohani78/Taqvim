/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1303 UI: calibration persists across screens, readings, ruler tab and states. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LevelScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val g = 9.81
    private val tilted = GravitySample.Reading(Vector3(sin(3.0 * PI / 180) * g, 0.0, cos(3.0 * PI / 180) * g))

    private fun viewModel(store: FakeCalibrationStore) =
        LevelViewModel({ flowOf(CompassFixtures.settings()) }, FakeSensors(gravity = MutableStateFlow(tilted)), store)

    @Test
    fun calibrationPersistsAcrossScreens() {
        val store = FakeCalibrationStore()
        var generation by mutableIntStateOf(0)
        composeRule.setContent {
            CompassTestTheme {
                key(generation) {
                    val viewModel = remember { viewModel(store) }
                    LevelRoute(viewModel = viewModel)
                }
            }
        }
        composeRule.onNodeWithText("Lying flat").assertIsDisplayed()
        composeRule.onNodeWithText("Tilted 3.0° across, 0.0° along").assertIsDisplayed()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR, composeRule.activity.requestedOrientation)
        composeRule.onNodeWithText("Calibrate").performScrollTo().performClick()
        composeRule.onNodeWithText("The surface is level").assertIsDisplayed()
        composeRule.waitUntil { DeviceOrientation.FLAT in store.stored.value.offsets }

        generation++
        composeRule.onNodeWithText("Reset calibration").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("The surface is level").assertIsDisplayed()
        composeRule.onNodeWithText("Reset calibration").performClick()
        composeRule.onNodeWithText("Tilted 3.0° across, 0.0° along").assertExists()
        composeRule.waitUntil {
            store.stored.value.offsets
                .isEmpty()
        }
    }

    @Test
    fun uprightReadingsRulerAndStates() {
        val calls = mutableListOf<String>()
        val actions =
            LevelActions(
                onSelectTab = { calls += it.name },
                onSelectUnit = { calls += it.name },
            )
        var state by mutableStateOf(
            LevelUiState(
                content =
                    LevelStateMapper.content(
                        LevelSample.Measured(Vector3(0.2, g, 0.0), DeviceOrientation.PORTRAIT),
                        LevelCalibration(),
                        ir.taqvim.core.i18n.NumeralSystem.LATIN,
                    ),
            ),
        )
        composeRule.setContent { CompassTestTheme { LevelScreen(state, actions) } }
        composeRule.onNodeWithText("Upright (portrait)").assertIsDisplayed()
        composeRule.onNodeWithText("Tilted 1.2°").assertIsDisplayed()
        composeRule.onNodeWithText("Reset calibration").assertDoesNotExist()
        composeRule.onNodeWithText("Ruler").performClick()
        state = state.copy(tab = LevelTab.RULER)
        composeRule.onNodeWithContentDescription("Ruler along the top edge of the screen").assertExists()
        composeRule.onNodeWithText("in").performClick()
        assertEquals(listOf("RULER", "INCHES"), calls)
        state = LevelUiState(content = LevelContent.SensorUnavailable)
        composeRule.onNodeWithText("No motion sensor").assertIsDisplayed()
        state = LevelUiState()
        composeRule.onNodeWithContentDescription("Starting the level").assertExists()
        assertTrue(calls.isNotEmpty())
    }
}
