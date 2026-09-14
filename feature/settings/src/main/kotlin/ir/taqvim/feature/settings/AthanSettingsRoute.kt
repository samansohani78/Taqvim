/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.ui.permission.rememberNotificationPermissionRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the athan settings; `:app` provides [AthanSettingsStore], [ExactAlarmAccess], [AthanSoundLibrary]
 * and [AthanPreview].
 */
val athanSettingsFeatureModule: Module =
    module {
        viewModelOf(::AthanSettingsViewModel)
    }

/**
 * The athan settings bound to their [AthanSettingsViewModel]; picks sounds with the system file picker, asks for the
 * notification permission when an athan is turned on and offers Do Not Disturb access for the Fajr bypass.
 */
@Composable
fun AthanSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: AthanSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestNotifications = rememberNotificationPermissionRequest()
    var dndAccessGranted by remember { mutableStateOf(dndAccessGranted(context)) }
    LifecycleResumeEffect(context) {
        dndAccessGranted = dndAccessGranted(context)
        onPauseOrDispose {}
    }
    val soundPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.onSoundPicked(uri?.toString())
        }
    val actions =
        remember(viewModel, context, soundPicker, requestNotifications) {
            AthanSettingsActions(
                onAlertToggled = { prayer, enabled ->
                    viewModel.onAlertToggled(prayer, enabled)
                    if (enabled) requestNotifications()
                },
                onGapStep = viewModel::onGapStep,
                onPickSound = { soundPicker.launch(arrayOf(SOUND_MIME_TYPE)) },
                onUseDefaultSound = viewModel::onUseDefaultSound,
                onPreview = viewModel::onPreview,
                onVolumeChanged = viewModel::onVolumeChanged,
                onVibrateChanged = viewModel::onVibrateChanged,
                onBypassDndChanged = viewModel::onBypassDndChanged,
                onIranTimeChanged = viewModel::onIranTimeChanged,
                onAllowExactAlarms = { openExactAlarmSettings(context) },
                onOpenDndAccess = { openDndAccessSettings(context) },
            )
        }
    AthanSettingsScreen(state, actions, modifier, dndAccessMissing = !dndAccessGranted)
}

/** MIME type offered in the sound picker. */
internal const val SOUND_MIME_TYPE: String = "audio/*"

/** The system screen that grants exact alarms to this app (API 31+), or `null` below API 31 where none is needed. */
internal fun exactAlarmSettingsIntent(context: Context): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else {
        null
    }

private fun openExactAlarmSettings(context: Context) {
    exactAlarmSettingsIntent(context)?.let { intent -> runCatching { context.startActivity(intent) } }
}

/** Whether the app may let sound through Do Not Disturb (needed by the Fajr bypass). */
internal fun dndAccessGranted(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted ?: false

private fun openDndAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
