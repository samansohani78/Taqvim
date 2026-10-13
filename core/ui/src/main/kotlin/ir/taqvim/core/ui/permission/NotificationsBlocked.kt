/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.R

/**
 * Says that the feature the user just turned on cannot alert them, because the system is not showing this app's
 * notifications, and offers the system screen where that is changed. Shown next to the switch that turned the
 * feature on, so the explanation is where the expectation was made.
 */
@Composable
public fun NotificationsBlocked(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.shared_notifications_blocked),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        TextButton(onClick = { openNotificationSettings(context) }) {
            Text(stringResource(R.string.shared_notifications_allow))
        }
    }
}

/** The system screen where this app's notifications are turned on or off. */
internal fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openNotificationSettings(context: Context) {
    runCatching { context.startActivity(notificationSettingsIntent(context)) }
}
