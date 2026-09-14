/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.app.Application
import android.app.NotificationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.ui.permission.NOTIFICATION_PERMISSION
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-1101 UI tests: each athan control persists its value through the route; warnings and states are shown. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AthanSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val preview = FakePreview()
    private val launchedInputs = mutableListOf<List<Any?>>()

    /** Answers every picker launch with [picked] at once. */
    private fun registryOwner(picked: String?) =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I, O> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?,
                    ) {
                        launchedInputs += (input as? Array<*>)?.toList() ?: listOf(input)
                        // A permission request (String input) answers "denied"; a sound pick answers the picked file.
                        dispatchResult(requestCode, if (input is String) false else picked?.let(Uri::parse))
                    }
                }
        }

    private fun showRoute(
        store: FakeAthanStore,
        exactAllowed: Boolean = true,
        picked: String? = AthanFixtures.SOUND_URI,
    ) {
        val viewModel = AthanSettingsViewModel(store, FakeExactAlarms(exactAllowed), AthanFixtures.library, preview)
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(picked)) {
                LocationTestTheme { AthanSettingsRoute(viewModel = viewModel) }
            }
        }
        settle()
    }

    /** Runs the main looper past the view model's debounce, then lets Compose settle. */
    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        composeRule.waitForIdle()
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
        settle()
    }

    @Test
    fun prayerSwitchesAndGapsPersist() {
        val store = FakeAthanStore(LocationFixtures.english)
        showRoute(store)

        click("Fajr")
        composeRule.onNodeWithText("Fajr").assertIsOn()
        repeat(2) {
            composeRule.onNodeWithContentDescription("One minute later for Fajr").performClick()
            settle()
        }
        composeRule.onNodeWithContentDescription("One minute earlier for Fajr").performClick()
        settle()
        click("Isha")
        click("Isha")

        assertEquals(PrayerAlert(enabled = true, gapMinutes = 1), store.current.alerts[AthanPrayerKind.FAJR])
        assertEquals(PrayerAlert(enabled = false, gapMinutes = 0), store.current.alerts[AthanPrayerKind.ISHA])
        composeRule.onNodeWithText("1 min after the prayer time").assertIsDisplayed()
        composeRule.onNodeWithText("Isha").assertIsOff()
    }

    @Test
    fun optionSwitchesAndVolumePersist() {
        val store = FakeAthanStore(LocationFixtures.english, AthanFixtures.someOn)
        showRoute(store)

        click("Vibrate")
        click("Fajr athan in Do Not Disturb")
        click("Iran time")
        composeRule
            .onNodeWithContentDescription("Volume")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(35f) }
        settle()

        assertEquals(false, store.current.vibrate)
        assertEquals(true, store.current.bypassDndForFajr)
        assertEquals(true, store.current.useIranTime)
        assertEquals(35, store.current.volumePercent)
        composeRule.onNodeWithText("35%").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun soundPickDefaultAndPreviewPersist() {
        val store = FakeAthanStore(LocationFixtures.english)
        showRoute(store)

        click("Choose sound")
        assertEquals(listOf(listOf(SOUND_MIME_TYPE)), launchedInputs)
        assertEquals(AthanSoundChoice(AthanFixtures.SOUND_URI, "Tehran athan"), store.current.sound)
        composeRule.onNodeWithText("Tehran athan").assertIsDisplayed()

        click("Play preview")
        assertEquals(listOf(AthanSoundChoice(AthanFixtures.SOUND_URI, "Tehran athan") to 80), preview.plays)
        click("Stop preview")
        assertEquals(1, preview.stops)

        click("Use default sound")
        assertEquals(null, store.current.sound)
        composeRule.onNodeWithText("Default alarm sound").assertIsDisplayed()
    }

    @Test
    fun unusableFilesAndBlockedExactAlarmsAreExplained() {
        val store = FakeAthanStore(LocationFixtures.english, AthanFixtures.someOn)
        showRoute(store, exactAllowed = false, picked = "file:///sdcard/unknown.bin")

        click("Choose sound")
        composeRule.onNodeWithText("That file could not be used. Choose another sound.").assertIsDisplayed()
        assertEquals(null, store.current.sound)

        composeRule.onNodeWithText("Athan may sound late because exact alarms are not allowed.").performScrollTo()
        click("Allow exact alarms")
        val started = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, started.action)
        assertEquals("package", started.data?.scheme)
    }

    @Test
    fun cancelledPicksChangeNothingAndTheBypassNeedsFajr() {
        val store = FakeAthanStore(LocationFixtures.english)
        showRoute(store, picked = null)

        click("Choose sound")
        assertEquals(0, store.updates)
        composeRule.onNodeWithText("Fajr athan in Do Not Disturb").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Turn on the Fajr athan to use this.").assertIsDisplayed()
    }

    @Test
    fun turningAnAthanOnAsksForNotificationsOnlyWhileMissing() {
        val store = FakeAthanStore(LocationFixtures.english)
        showRoute(store)

        click("Fajr")
        click("Fajr")
        assertEquals(listOf(listOf(NOTIFICATION_PERMISSION)), launchedInputs)

        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(NOTIFICATION_PERMISSION)
        click("Isha")
        assertEquals(1, launchedInputs.size)
    }

    @Test
    fun theFajrBypassOffersDoNotDisturbAccessUntilGranted() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val notifications = application.getSystemService(NotificationManager::class.java)
        shadowOf(notifications).setNotificationPolicyAccessGranted(false)
        val store = FakeAthanStore(LocationFixtures.english, AthanFixtures.someOn)
        showRoute(store)
        val missing = "Allow Do Not Disturb access so the Fajr athan can sound."
        composeRule.onNodeWithText(missing).assertDoesNotExist()

        click("Fajr athan in Do Not Disturb")
        composeRule.onNodeWithText(missing).performScrollTo().assertIsDisplayed()
        click("Allow access")
        val started = shadowOf(application).nextStartedActivity
        assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, started.action)
    }

    @Test
    fun loadingIsAnnounced() {
        composeRule.setContent {
            LocationTestTheme { AthanSettingsScreen(AthanSettingsUiState(), AthanSettingsActions()) }
        }

        composeRule.onNodeWithContentDescription("Loading athan settings").assertIsDisplayed()
    }
}
