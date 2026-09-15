/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.TaqvimAppShell
import ir.taqvim.app.finishOnboarding
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/** T-1502 map pick in the running shell: the location settings open the map, and a confirmed point comes back saved. */
@RunWith(AndroidJUnit4::class)
class MapPickFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun finishFirstRun() {
        composeRule.finishOnboarding(showsShell = false)
    }

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun aPointPickedOnTheMapBecomesThePlaceAndReturnsToTheLocationSettings() {
        composeRule.setContent { TaqvimAppShell(link = AppDestination.LocationSettings) }
        waitFor {
            onAllNodesWithTag(
                destinationTag(AppDestination.LocationSettings),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Coordinates").performClick()
        composeRule.onNodeWithText("Pick on the map").performScrollTo().performClick()

        waitFor { onAllNodesWithContentDescription("World map").fetchSemanticsNodes().isNotEmpty() }
        val map = composeRule.onAllNodesWithContentDescription("World map").onFirst()
        val actions = map.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle { actions.first { it.label == "Pick the map center" }.action() }
        waitFor { onAllNodesWithText("Use location").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Use location").performClick()

        waitFor { onAllNodesWithTag(destinationTag(AppDestination.MapPick)).fetchSemanticsNodes().isEmpty() }
        waitFor {
            onAllNodesWithTag(
                destinationTag(AppDestination.LocationSettings),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(Coordinates(0.0, 0.0), storedPlace())
    }

    /** Waits for [condition], running the main looper so that saves started from ViewModels and screens finish. */
    private fun waitFor(condition: ComposeTestRule.() -> Boolean) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.condition()
        }
    }

    /** The stored place, read on a worker thread so the main looper is never blocked. */
    private fun storedPlace(): Coordinates? {
        val repository = GlobalContext.get().get<UserPreferencesRepository>()
        var place: Coordinates? = null
        val reader =
            thread {
                val stored = runBlocking { withTimeout(READ_TIMEOUT_MILLIS) { repository.preferences.first().place } }
                place = stored?.coordinates
            }
        reader.join(TIMEOUT_MILLIS)
        return place
    }

    private companion object {
        /** Saving describes the point from the bundled city catalog, which is parsed on first use. */
        const val TIMEOUT_MILLIS = 30_000L
        const val READ_TIMEOUT_MILLIS = 10_000L
    }
}
