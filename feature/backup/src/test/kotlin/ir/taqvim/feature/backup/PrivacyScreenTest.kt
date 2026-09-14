/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import android.app.Application
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1503 UI tests: the privacy dashboard lists data and permissions, clears after confirmation, opens settings. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrivacyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val data = FakePrivacyData()
    private val permissions = FakePermissions()

    private fun showRoute() {
        val viewModel = PrivacyViewModel(data, permissions, FakeLanguages(BackupFixtures.english))
        composeRule.setContent { BackupTestTheme { PrivacyRoute(viewModel = viewModel) } }
        settle()
    }

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
        settle()
    }

    @Test
    fun dataIsListedAndClearedAfterConfirmation() {
        showRoute()

        composeRule.onNodeWithText("Personal events").assertIsDisplayed()
        composeRule.onNodeWithText("Entries: 140").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("A location is saved.").performScrollTo().assertIsDisplayed()

        click("Forget location")
        composeRule.onNodeWithText("Clear this data?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        settle()
        assertEquals(emptyList<StoredDataKind>(), data.cleared)

        click("Forget location")
        composeRule.onNodeWithText("Clear").performClick()
        settle()
        assertEquals(listOf(StoredDataKind.LOCATION), data.cleared)
        composeRule.onNodeWithText("No location saved.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun failedClearingIsExplained() {
        data.clearSucceeds = false
        showRoute()

        click("Clear recent searches")
        composeRule.onNodeWithText("Clear").performClick()
        settle()

        composeRule.onNodeWithText("The data could not be cleared. Try again.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun permissionRowsOpenTheirSystemSettings() {
        showRoute()
        assertTrue(permissions.refreshes >= 1)
        val application = ApplicationProvider.getApplicationContext<Application>()

        fun open(title: String): android.content.Intent {
            composeRule.onNodeWithContentDescription("Open settings for $title").performScrollTo().performClick()
            settle()
            return shadowOf(application).nextStartedActivity
        }

        open("Location").let {
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, it.action)
            assertEquals("package:${application.packageName}", it.dataString)
        }
        open("Calendar").let { assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, it.action) }
        open("Notifications").let {
            assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, it.action)
            assertEquals(application.packageName, it.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        }
        open("Exact alarms").let { assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, it.action) }
        open("Do Not Disturb access").let {
            assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, it.action)
        }
    }

    @Test
    fun loadingIsAnnounced() {
        composeRule.setContent { BackupTestTheme { PrivacyScreen(PrivacyUiState(), PrivacyActions()) } }

        composeRule.onNodeWithContentDescription("Loading privacy information").assertIsDisplayed()
    }
}
