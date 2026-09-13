/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1100 UI tests: what each state shows, which row is announced as selected, and the actions. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tehran = TimesFixtures.tehran()
    private val noon = TimesFixtures.at("2026-06-21T12:00", tehran)

    private fun show(
        state: TimesUiState,
        actions: TimesActions = TimesActions(),
    ) {
        composeRule.setContent { TimesTestTheme { TimesScreen(state, actions) } }
    }

    @Test
    fun todayHighlightsTheNextTimeAndShowsTheCountdown() {
        show(TimesStateMapper.map(tehran, 0, noon, expanded = false))
        composeRule.onNodeWithText("Prayer times").assertIsDisplayed()
        composeRule.onNodeWithText("Tehran").assertIsDisplayed()
        composeRule.onNode(isSelected()).assertTextContains("Dhuhr")
        composeRule.onNodeWithText("Dhuhr in", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Time until Dhuhr").assertExists()
        composeRule.onNodeWithContentDescription("Sun path from sunrise at", substring = true).assertExists()
        composeRule.onNodeWithText("Sunset").assertDoesNotExist()
        composeRule.onNodeWithText("Today").assertDoesNotExist()
    }

    @Test
    fun actionsAreReported() {
        val calls = mutableListOf<String>()
        val actions =
            TimesActions(
                onPreviousDay = { calls += "previous" },
                onNextDay = { calls += "next" },
                onToday = { calls += "today" },
                onToggleExpanded = { calls += "toggle" },
                onPrintReport = { calls += "print" },
            )
        show(TimesStateMapper.map(tehran, 1, noon, expanded = true), actions)
        composeRule.onNodeWithText("Previous day").performClick()
        composeRule.onNodeWithText("Next day").performClick()
        composeRule.onNodeWithText("Today").performClick()
        composeRule.onNodeWithText("Sunset").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Midnight").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show fewer times").performScrollTo().performClick()
        composeRule.onNodeWithText("Print monthly report").performScrollTo().performClick()
        assertEquals(listOf("previous", "next", "today", "toggle", "print"), calls)
    }

    @Test
    fun polarDaysExplainWhyThereAreNoTimes() {
        show(TimesStateMapper.map(TimesFixtures.tromso(), 0, noon, expanded = false))
        composeRule.onNodeWithText("The Sun does not set on this day", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Show all times").assertDoesNotExist()
    }

    @Test
    fun loadingAndMissingPlaceStates() {
        show(TimesUiState(TimesContent.NoLocation))
        composeRule.onNodeWithText("No place chosen").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a city", substring = true).assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        show(TimesUiState())
        composeRule.onNodeWithContentDescription("Loading prayer times").assertExists()
    }

    @Test
    fun routeBindsTheViewModelAndPrintsTheReport() {
        val viewModel = TimesViewModel({ MutableStateFlow(tehran) }, FakeClock(noon))
        composeRule.setContent { TimesTestTheme { TimesRoute(viewModel = viewModel) } }
        composeRule.onNodeWithText("Show all times").performScrollTo().performClick()
        composeRule.onNodeWithText("Sunset").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Print monthly report").performScrollTo().performClick()
        composeRule.onNodeWithText("Next day").performScrollTo().performClick()
        composeRule.onNodeWithText("Today").performScrollTo().performClick()
        composeRule.onNodeWithText("Today").assertDoesNotExist()
        composeRule.onNodeWithText("Previous day").performScrollTo().performClick()
        composeRule.onNodeWithText("Today").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reportLabelsComeFromResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val labels = reportLabels(context.resources)
        assertEquals(PrayerKind.entries.size, labels.prayers.size)
        assertEquals("Fajr", labels.prayers.getValue(PrayerKind.FAJR))
        assertTrue(labels.title("Tehran", "1", "31").contains("Tehran"))
        assertEquals("Date", labels.date)
    }
}
