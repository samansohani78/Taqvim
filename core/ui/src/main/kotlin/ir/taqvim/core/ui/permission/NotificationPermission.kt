/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The runtime permission that lets notifications show on Android 13 (API 33) and later (`POST_NOTIFICATIONS`). */
public const val NOTIFICATION_PERMISSION: String = "android.permission.POST_NOTIFICATIONS"

/** Whether [context] still has to ask for [NOTIFICATION_PERMISSION] on a device running [sdkInt]. */
public fun needsNotificationPermission(
    context: Context,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean =
    sdkInt >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(NOTIFICATION_PERMISSION) != PackageManager.PERMISSION_GRANTED

/**
 * A function that asks for the notification permission when it is still missing (T-1001, T-1002, T-1101). Call it
 * when the user turns on something that notifies. The answer is not needed: without the permission the app keeps
 * working and only its notifications stay hidden, so the request is never repeated in a loop.
 */
@Composable
public fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(context, launcher) {
        { if (needsNotificationPermission(context)) launcher.launch(NOTIFICATION_PERMISSION) }
    }
}
