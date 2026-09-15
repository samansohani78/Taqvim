/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.destinationTag
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/** How long app tests wait for preference writes and the screens that follow them. */
internal const val PREFERENCES_TIMEOUT_MILLIS: Long = 10_000

/** The stored preferences of the running app. */
internal fun storedPreferences(): UserPreferences =
    runBlocking {
        GlobalContext
            .get()
            .get<UserPreferencesRepository>()
            .preferences
            .first()
    }

/**
 * Marks the first-run onboarding (T-1501) of the running app as done, as a returning user's device would have it, and
 * waits until the calendar is shown when a composition is running.
 */
internal fun ComposeTestRule.finishOnboarding(showsShell: Boolean = true) {
    runBlocking {
        GlobalContext.get().get<UserPreferencesRepository>().update { it.copy(onboardingCompleted = true) }
    }
    if (showsShell) {
        waitUntil(PREFERENCES_TIMEOUT_MILLIS) {
            onAllNodesWithTag(destinationTag(AppDestination.Calendar)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
