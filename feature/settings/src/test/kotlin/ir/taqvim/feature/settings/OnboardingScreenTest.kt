/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.events.EventSource
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1501 through the route: choices are stored, pages change, the location page is embedded and skip finishes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val onboarding = FakeOnboardingStore()
    private var finished = 0

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun show(store: FakeGeneralSettingsStore) {
        val viewModel = OnboardingViewModel(store, onboarding)
        composeRule.setContent {
            LocationTestTheme {
                OnboardingRoute(
                    onFinished = { finished++ },
                    viewModel = viewModel,
                    location = { Box(it.testTag(LOCATION)) },
                )
            }
        }
        settle()
    }

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        settle()
    }

    /** A row of the page's lazy list, scrolled into view since only the rows in view are composed. */
    private fun row(tag: String): SemanticsNodeInteraction {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(tag))
        return composeRule.onNodeWithTag(tag)
    }

    private fun clickRow(tag: String) {
        row(tag).performClick()
        settle()
    }

    @Test
    fun choosingALanguageAndCompletingThePagesStoresEverything() {
        val store = FakeGeneralSettingsStore(LocationFixtures.english)
        show(store)

        composeRule.onNodeWithText(context.getString(R.string.onboarding_language_title)).assertExists()
        row(OnboardingTags.language("en")).assertIsSelected()
        clickRow(OnboardingTags.language("ne"))
        assertEquals("ne", store.current.languageCode)
        row(OnboardingTags.language("ne")).assertIsSelected()

        click(OnboardingTags.NEXT)
        composeRule.onNodeWithTag(LOCATION).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_location_title)).assertExists()

        click(OnboardingTags.NEXT)
        row(OnboardingTags.source(EventSource.ANCIENT_IRAN.name)).assertIsOff()
        clickRow(OnboardingTags.source(EventSource.ANCIENT_IRAN.name))
        row(OnboardingTags.source(EventSource.ANCIENT_IRAN.name)).assertIsOn()
        assertEquals(true, EventSource.ANCIENT_IRAN in store.current.enabledEventSources)
        composeRule.onNodeWithText(context.getString(R.string.onboarding_done)).assertExists()
        composeRule.onNodeWithTag(OnboardingTags.SKIP).assertDoesNotExist()

        click(OnboardingTags.BACK)
        composeRule.onNodeWithTag(LOCATION).assertExists()
        click(OnboardingTags.NEXT)
        click(OnboardingTags.NEXT)
        assertEquals(1, onboarding.completions)
        assertEquals(1, finished)
    }

    @Test
    fun skipAndSystemBackFinishTheOnboarding() {
        show(FakeGeneralSettingsStore(LocationFixtures.persian))
        click(OnboardingTags.NEXT)

        Espresso.pressBack()
        settle()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_language_title)).assertExists()
        assertEquals(0, onboarding.completions)

        click(OnboardingTags.SKIP)
        assertEquals(1, onboarding.completions)
        assertEquals(1, finished)
    }

    @Test
    fun theLoadingStateIsDescribed() {
        composeRule.setContent {
            LocationTestTheme { OnboardingScreen(OnboardingUiState(), OnboardingActions()) }
        }
        composeRule.onNodeWithContentDescription(context.getString(R.string.onboarding_loading)).assertExists()
    }

    private companion object {
        const val LOCATION = "test:location"
    }
}
