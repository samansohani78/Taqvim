/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.data.database.backup.RecoveryResult
import ir.taqvim.data.database.backup.RestoreGate
import ir.taqvim.data.database.backup.RestoreState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/** ADR-0032: while a restore is not settled, the app shows the waiting screen instead of any data screen. */
@RunWith(AndroidJUnit4::class)
class RestoreHoldTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(PREFERENCES_TIMEOUT_MILLIS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun aNormalStartIsSettledAndShowsNoHold() {
        val gate = GlobalContext.get().get<RestoreGate>()
        assertEquals(RestoreState.SETTLED, gate.state.value)
        waitForTag(ONBOARDING_TAG)
        assertTrue(composeRule.onAllNodesWithTag(RESTORE_HOLD_TAG).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun anIncompleteRestoreReplacesEveryScreenWithTheExplanation() {
        waitForTag(ONBOARDING_TAG)
        GlobalContext.get().get<RestoreGate>().settle(RecoveryResult.StillPending)

        waitForTag(RESTORE_HOLD_TAG)
        assertTrue(composeRule.onAllNodesWithTag(ONBOARDING_TAG).fetchSemanticsNodes().isEmpty())
        val text = composeRule.activity.getString(R.string.restore_incomplete)
        composeRule.onNodeWithText(text).assertExists()
        composeRule.onNodeWithTag(RESTORE_HOLD_TAG).assertExists()
    }

    @Test
    fun aRestoreBeingFinishedShowsProgressInsteadOfTheApp() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent { TaqvimAppShell(restore = RestoreState.RECOVERING) }
        }

        waitForTag(RESTORE_HOLD_TAG)
        assertTrue(composeRule.onAllNodesWithTag(ONBOARDING_TAG).fetchSemanticsNodes().isEmpty())
        val text = composeRule.activity.getString(R.string.restore_finishing)
        composeRule.onNodeWithText(text).assertExists()
    }
}
