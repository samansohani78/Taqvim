/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.astronomy.ZodiacSystem
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1300 UI tests: the three views, the time controls, the header and the dialogs. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AstronomyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tehran = AstronomyFixtures.tehran()
    private val noon = AstronomyFixtures.at("2026-06-21T12:00", tehran)
    private val sky = AstronomyFixtures.sky(tehran, noon, isNow = false)

    private fun show(
        state: AstronomyUiState,
        actions: AstronomyActions = AstronomyActions(),
    ) {
        composeRule.setContent { AstronomyTestTheme { AstronomyScreen(state, actions) } }
    }

    @Test
    fun eachModeShowsItsView() {
        show(AstronomyUiState(AstronomyMode.EARTH, sky))
        composeRule.onNodeWithText("Astronomy").assertIsDisplayed()
        composeRule.onNodeWithText("Tehran").assertIsDisplayed()
        composeRule.onNodeWithText("Daylight").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Overview").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Next solar eclipse").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Total, ", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun moonAndSunViews() {
        show(AstronomyUiState(AstronomyMode.MOON, sky))
        composeRule.onNodeWithContentDescription("Moon: ", substring = true).assertExists()
        composeRule.onNodeWithText("Moonrise").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sunView() {
        show(AstronomyUiState(AstronomyMode.SUN, sky))
        composeRule.onNodeWithContentDescription("Sun path from sunrise at", substring = true).assertExists()
        composeRule.onNodeWithText("Solar noon").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun actionsAreReported() {
        val calls = mutableListOf<String>()
        val actions =
            AstronomyActions(
                onMode = { calls += "mode:$it" },
                onStepDays = { calls += "days:$it" },
                onStepYears = { calls += "years:$it" },
                onMinuteOfDay = { calls += "minute" },
                onNow = { calls += "now" },
                onPickDate = { calls += "pick" },
                onOpenDialog = { calls += "dialog:$it" },
            )
        show(AstronomyUiState(AstronomyMode.EARTH, sky), actions)
        composeRule.onNodeWithText("Moon").performClick()
        composeRule.onNodeWithText("Previous year").performClick()
        composeRule.onNodeWithText("Next day").performClick()
        composeRule.onNodeWithText("Choose date").performClick()
        composeRule.onNodeWithText("Now").performClick()
        composeRule.onNodeWithContentDescription("Time of day").performSemanticsAction(SemanticsActions.SetProgress) {
            it(600f)
        }
        composeRule.onNodeWithText("Planetary hours").performScrollTo().performClick()
        assertEquals(
            listOf("mode:MOON", "years:-1", "days:1", "pick", "now", "minute", "dialog:PLANETARY_HOURS"),
            calls,
        )
    }

    @Test
    fun horoscopeDialogListsHousesAndCloses() {
        var dismissed = false
        val dialog = AstronomyDialogs.build(AstronomyDialogKind.HOROSCOPE, tehran, noon)
        show(AstronomyUiState(content = sky, dialog = dialog), AstronomyActions(onDismissDialog = { dismissed = true }))
        composeRule.onNodeWithText("Chart for ", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chart wheel with twelve houses").assertExists()
        composeRule.onNodeWithText("Ascendant").assertExists()
        composeRule.onNodeWithText("House 12").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        assertEquals(true, dismissed)
    }

    @Test
    fun planetaryHoursMarkTheCurrentHourAndOtherDialogs() {
        val hours = AstronomyDialogs.build(AstronomyDialogKind.PLANETARY_HOURS, tehran, noon)
        show(AstronomyUiState(content = sky, dialog = hours))
        // The selected mode tab and the current hour.
        composeRule.onAllNodes(isSelected()).assertCountEquals(2)
        composeRule.onNodeWithText("1. Sun (Day)", substring = true).assertExists()
    }

    @Test
    fun polarHoroscopeAndMoonInScorpio() {
        val tromso = AstronomyFixtures.tromso()
        val polar = AstronomyDialogs.build(AstronomyDialogKind.YEAR_HOROSCOPE, tromso, noon)
        show(AstronomyUiState(content = sky, dialog = polar))
        composeRule.onNodeWithText("Chart for the March equinox", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Houses cannot be calculated", substring = true).assertExists()
    }

    @Test
    fun moonInScorpioDialog() {
        val scorpio = AstronomyDialog.MoonInScorpio("1405", ZodiacSystem.TROPICAL, persistentListOf())
        show(AstronomyUiState(content = sky, dialog = scorpio))
        composeRule.onNodeWithText("Moon in Scorpio in 1405").assertIsDisplayed()
        composeRule.onNodeWithText("No periods in this year.").assertExists()
        composeRule.onNodeWithText("Tropical sign Scorpio", substring = true).assertExists()
    }

    @Test
    fun loadingAndMissingPlaceStates() {
        show(AstronomyUiState(content = AstronomyContent.NoLocation))
        composeRule.onNodeWithText("No place chosen").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a city", substring = true).assertIsDisplayed()
    }

    @Test
    fun loadingState() {
        show(AstronomyUiState())
        composeRule.onNodeWithContentDescription("Loading the sky").assertExists()
    }
}
