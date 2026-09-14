/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.permission

import android.app.Application
import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The notification permission is asked for only on Android 13+ and only while it is missing. */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val launched = mutableListOf<Any?>()

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
                        dispatchResult(requestCode, false)
                    }
                }
        }

    private fun request(): () -> Unit {
        var request: () -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                request = rememberNotificationPermissionRequest()
            }
        }
        composeRule.waitForIdle()
        return request
    }

    @Test
    fun onlyAndroid13AndLaterWithoutTheGrantNeedIt() {
        assertEquals(false, needsNotificationPermission(application, Build.VERSION_CODES.S_V2))
        assertEquals(true, needsNotificationPermission(application, Build.VERSION_CODES.TIRAMISU))
        shadowOf(application).grantPermissions(NOTIFICATION_PERMISSION)
        assertEquals(false, needsNotificationPermission(application, Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    fun theRequestAsksWhileMissingAndNotOnceGranted() {
        val ask = request()

        ask()
        assertEquals(listOf<Any?>(NOTIFICATION_PERMISSION), launched)

        shadowOf(application).grantPermissions(NOTIFICATION_PERMISSION)
        ask()
        assertEquals(1, launched.size)
    }
}
