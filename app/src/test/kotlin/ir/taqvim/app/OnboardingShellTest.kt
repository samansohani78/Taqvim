/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.os.Looper
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.destinationTag
import ir.taqvim.data.preferences.UserPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * T-1501 first-run matrix in the running app (docs/PLAN.md: fa/en/ne/ckb, plus ar from the E2E list): a fresh install
 * shows the onboarding; choosing each language stores exactly that language's defaults; skipping opens the calendar
 * and the onboarding is not shown again.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class OnboardingShellTest(
    private val language: String,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    /**
     * Runs the main looper's pending work. Preference updates from a ViewModel run their transform in the caller's
     * (main) context, which Robolectric's paused looper only runs when it is idled.
     */
    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Waits, with a timeout, until [condition] holds, running the main looper's work between checks. */
    private fun waitFor(condition: () -> Boolean) {
        composeRule.waitUntil(PREFERENCES_TIMEOUT_MILLIS) {
            idleMainLooper()
            condition()
        }
    }

    private fun waitForTag(tag: String) {
        waitFor { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun choosingTheLanguageAppliesItsDefaultsAndSkippingOpensTheCalendar() {
        waitForTag(ONBOARDING_TAG)
        // The language list is lazy: rows out of view are not composed until it scrolls to them.
        waitFor { composeRule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(LANGUAGE_PREFIX + language))
        composeRule.onNodeWithTag(LANGUAGE_PREFIX + language).performClick()
        idleMainLooper()
        waitFor { storedPreferences().languageCode == language }

        assertEquals(UserPreferences.defaultsFor(language), storedPreferences())

        composeRule.onNodeWithTag(SKIP).performClick()
        idleMainLooper()
        waitForTag(destinationTag(AppDestination.Calendar))
        assertTrue(storedPreferences().onboardingCompleted)
        assertEquals(UserPreferences.defaultsFor(language).copy(onboardingCompleted = true), storedPreferences())

        composeRule.activityRule.scenario.recreate()
        waitForTag(destinationTag(AppDestination.Calendar))
        composeRule.onNodeWithTag(ONBOARDING_TAG).assertDoesNotExist()
    }

    companion object {
        private const val LANGUAGE_PREFIX = "onboarding:language:"
        private const val SKIP = "onboarding:skip"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = listOf("fa", "en", "ne", "ckb", "ar").map { arrayOf<Any>(it) }
    }
}
