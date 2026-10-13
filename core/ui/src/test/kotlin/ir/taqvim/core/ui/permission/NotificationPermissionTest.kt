/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.permission

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T-1001/T-1101/T-1213: the notification permission is asked for only on Android 13+, only while it is missing, and
 * at most once per screen; a screen that never asks never prompts. When the app may not notify, the shared banner
 * says so and leads to the system screen that fixes it.
 */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val launched = mutableListOf<Any?>()
    private var answer = false

    private val registryOwner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I, O> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?,
                    ) {
                        launched += input
                        dispatchResult(requestCode, answer)
                    }
                }
        }

    private fun notificationsEnabled(enabled: Boolean) {
        shadowOf(application.getSystemService(NotificationManager::class.java))
            .setNotificationsEnabled(enabled)
    }

    private fun permission(): NotificationPermission {
        lateinit var permission: NotificationPermission
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                permission = rememberNotificationPermission()
            }
        }
        composeRule.waitForIdle()
        return permission
    }

    @Test
    fun onlyAndroid13AndLaterWithoutTheGrantNeedThePermission() {
        assertEquals(false, needsNotificationPermission(application, Build.VERSION_CODES.S_V2))
        assertEquals(true, needsNotificationPermission(application, Build.VERSION_CODES.TIRAMISU))
        shadowOf(application).grantPermissions(NOTIFICATION_PERMISSION)
        assertEquals(false, needsNotificationPermission(application, Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun theRequestAsksWhileMissingAndNotOnceGranted() {
        val permission = permission()

        permission.request()
        assertEquals(listOf<Any?>(NOTIFICATION_PERMISSION), launched)

        shadowOf(application).grantPermissions(NOTIFICATION_PERMISSION)
        permission.request()
        assertEquals(1, launched.size)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun aRefusalIsAnAnswerAndIsNotAskedAgain() {
        notificationsEnabled(false)
        val permission = permission()
        assertEquals(false, permission.allowed)

        permission.request()
        permission.request()
        permission.request()

        // One dialog, however many switches the user turns on afterwards.
        assertEquals(listOf<Any?>(NOTIFICATION_PERMISSION), launched)
        assertEquals(false, permission.allowed)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun grantingTheRequestLetsTheScreenNotifyWithoutLeavingIt() {
        notificationsEnabled(false)
        val permission = permission()
        answer = true

        notificationsEnabled(true)
        permission.request()
        composeRule.waitForIdle()

        assertEquals(true, permission.allowed)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S_V2])
    fun belowAndroid13NothingIsAskedAndNotificationsCanStillBeOff() {
        notificationsEnabled(false)
        val permission = permission()

        permission.request()

        assertTrue(launched.isEmpty())
        assertEquals(false, permission.allowed)
    }

    @Test
    fun theBannerExplainsAndOpensTheSystemNotificationSettings() {
        composeRule.setContent { MaterialTheme { NotificationsBlocked() } }

        composeRule
            .onNodeWithText("Notifications are turned off for this app, so this cannot alert you.")
            .assertExists()
        composeRule.onNodeWithText("Turn on notifications").performClick()

        val intent = notificationSettingsIntent(application)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(application.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
