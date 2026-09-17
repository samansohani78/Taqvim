/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.os.Looper
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1500 UI test: subscriptions are added, refreshed, paused and removed, and the network switch is stored. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SubscriptionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun click(node: SemanticsNodeInteraction) {
        node.performScrollTo().performClick()
        settle()
    }

    @Test
    fun subscriptionsAreManagedThroughTheRoute() {
        val store = FakeSubscriptionsStore()
        val settings = FakeGeneralSettingsStore(LocationFixtures.english)
        val viewModel = SubscriptionsViewModel(store, settings)
        composeRule.setContent {
            LocationTestTheme { SubscriptionsRoute(viewModel = viewModel) }
        }
        settle()

        composeRule.onNodeWithText("Holidays feed").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Paused").performScrollTo().assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("ftp://example.org/a.ics")
        click(composeRule.onNodeWithText("Subscribe"))
        composeRule.onNodeWithText("Enter an address that starts with https:// or webcal://").assertIsDisplayed()

        click(composeRule.onNodeWithContentDescription("Refresh Holidays feed"))
        assertEquals(listOf(1L), store.refreshed)
        composeRule.onNodeWithText("Calendar updated").assertIsDisplayed()

        click(composeRule.onNodeWithText("Team"))
        click(composeRule.onNodeWithContentDescription("Remove Holidays feed"))
        click(composeRule.onNodeWithText("Refresh over the internet"))

        assertEquals(listOf(2L to true), store.current.map { it.id to it.enabled })
        assertEquals(false, settings.current.subscriptionsNetworkAllowed)
    }

    @Test
    fun validAddressesAreAdded() {
        val store = FakeSubscriptionsStore(items = emptyList())
        val viewModel = SubscriptionsViewModel(store, FakeGeneralSettingsStore(LocationFixtures.english))
        composeRule.setContent {
            LocationTestTheme { SubscriptionsRoute(viewModel = viewModel) }
        }
        settle()
        composeRule.onNodeWithText("No calendar subscriptions yet").assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("webcal://example.org/team.ics")
        click(composeRule.onNodeWithText("Subscribe"))

        assertEquals(listOf("webcal://example.org/team.ics"), store.added)
        composeRule.onNodeWithText("Subscribed").assertIsDisplayed()
    }

    @Test
    fun loadingIsAnnounced() {
        composeRule.setContent {
            LocationTestTheme { SubscriptionsScreen(SubscriptionsUiState(), SubscriptionsActions()) }
        }

        composeRule.onNodeWithContentDescription("Loading subscriptions").assertIsDisplayed()
    }
}
