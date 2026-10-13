/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.permission

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * The runtime permission that lets notifications show on Android 13 (API 33) and later (`POST_NOTIFICATIONS`). It is
 * a dangerous permission: the user grants it in a system dialog, may refuse it, and may take it back afterwards.
 */
public const val NOTIFICATION_PERMISSION: String = "android.permission.POST_NOTIFICATIONS"

/** Whether [context] still has to ask for [NOTIFICATION_PERMISSION] on a device running [sdkInt]. */
public fun needsNotificationPermission(
    context: Context,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean =
    sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(NOTIFICATION_PERMISSION) != PackageManager.PERMISSION_GRANTED

/**
 * Whether the system would show a notification this app posts. This is the question a feature that notifies has to
 * answer, and it is wider than [needsNotificationPermission]: below Android 13 there is no permission to ask for but
 * the user can still turn the app's notifications off, and on Android 13 and later they can take the grant back.
 */
public fun notificationsAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true

/**
 * Whether this app may notify, and the request that asks for the permission when it may not (T-1001, T-1101, T-1213).
 */
@Stable
public class NotificationPermission internal constructor(
    private val allowedState: MutableState<Boolean>,
    private val ask: () -> Unit,
) {
    /** `false` when the system will not show this app's notifications, so a feature that notifies cannot. */
    public val allowed: Boolean get() = allowedState.value

    /** Asks for the permission when it is missing. Call it when the user turns on something that notifies. */
    public fun request() {
        ask()
    }
}

/**
 * The notification permission of the screen being composed, re-read whenever it resumes so that an answer given in
 * the system settings is seen on the way back.
 *
 * The request is made at most once per screen: a refusal is an answer, and asking again at every switch would be
 * nagging. Nothing here runs on its own — a screen that never calls [NotificationPermission.request] never asks,
 * which is why the app asks for nothing at startup.
 */
@Composable
public fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val allowed = remember(context) { mutableStateOf(notificationsAllowed(context)) }
    val asked = rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            allowed.value = notificationsAllowed(context)
        }
    LifecycleResumeEffect(context) {
        allowed.value = notificationsAllowed(context)
        onPauseOrDispose {}
    }
    return remember(context, launcher) {
        NotificationPermission(allowed) {
            if (needsNotificationPermission(context) && !asked.value) {
                asked.value = true
                launcher.launch(NOTIFICATION_PERMISSION)
            }
        }
    }
}

/**
 * Whether the system will show this app's notifications, for the parts of a screen that are far from the route that
 * knows. A screen provides it from its [rememberNotificationPermission]; the default is `true` so that a screen which
 * notifies nobody — and every preview and screenshot — shows no warning.
 */
public val LocalNotificationsAllowed: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }
