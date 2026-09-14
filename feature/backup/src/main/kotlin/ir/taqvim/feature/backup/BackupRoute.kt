/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin bindings of backup/restore and the privacy dashboard; `:app` provides [BackupOperations],
 * [BackupLanguageSource], [PrivacyDataSource] and [PermissionStatusSource].
 */
val backupFeatureModule: Module =
    module {
        viewModel { BackupViewModel(get(), get()) }
        viewModel { PrivacyViewModel(get(), get(), get()) }
    }

/** Backup and restore bound to [BackupViewModel]; files are chosen with the system document pickers. */
@Composable
fun BackupRoute(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Deliberately `remember`, not `rememberSaveable`: passphrases must never reach saved instance state.
    val fields = remember { PassphraseFields() }
    DisposableEffect(fields) { onDispose { fields.clearAll() } }
    val plainPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PLAIN_MIME_TYPE)) { uri ->
            uri?.let { viewModel.onExportDestination(it.toString(), CharArray(0)) }
        }
    val encryptedPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ENCRYPTED_MIME_TYPE)) { uri ->
            uri?.let { viewModel.onExportDestination(it.toString(), fields.takeExportPassphrase()) }
        }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.onImportSource(uri?.toString())
        }
    val actions =
        remember(viewModel, fields, plainPicker, encryptedPicker, importPicker) {
            BackupActions(
                onEncryptChanged = viewModel::onEncryptChanged,
                onExport = { encrypt ->
                    if (encrypt) encryptedPicker.launch(ENCRYPTED_FILE_NAME) else plainPicker.launch(PLAIN_FILE_NAME)
                },
                onExportMessageDismissed = viewModel::onExportMessageDismissed,
                onChooseImport = { importPicker.launch(arrayOf(IMPORT_MIME_TYPE)) },
                onSubmitImportPassphrase = { viewModel.onImportPassphrase(fields.takeImportPassphrase()) },
                onCancelImport = {
                    fields.importPassphrase.clearText()
                    viewModel.onImportDismissed()
                },
                onRestoreRequested = viewModel::onRestoreRequested,
                onRestoreDismissed = viewModel::onRestoreDismissed,
                onRestoreConfirmed = viewModel::onRestoreConfirmed,
            )
        }
    BackupScreen(state, actions, fields, modifier)
}

/** The privacy dashboard bound to [PrivacyViewModel]; permissions are checked again whenever it is shown. */
@Composable
fun PrivacyRoute(
    modifier: Modifier = Modifier,
    viewModel: PrivacyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    val actions =
        remember(viewModel, context) {
            PrivacyActions(
                onClearRequested = viewModel::onClearRequested,
                onClearConfirmed = viewModel::onClearConfirmed,
                onClearDismissed = viewModel::onClearDismissed,
                onOpenPermissionSettings = { kind -> openPermissionSettings(context, kind) },
            )
        }
    PrivacyScreen(state, actions, modifier)
}

/** MIME type and suggested name of a plain JSON export. */
internal const val PLAIN_MIME_TYPE: String = "application/json"
internal const val PLAIN_FILE_NAME: String = "taqvim-export.json"

/** MIME type and suggested name of an encrypted backup. */
internal const val ENCRYPTED_MIME_TYPE: String = "application/octet-stream"
internal const val ENCRYPTED_FILE_NAME: String = "taqvim-backup.taqvim"

/** Any file can be offered for import: providers rarely know the backup's type. */
internal const val IMPORT_MIME_TYPE: String = "*/*"

/** The system screen where the user grants or revokes [kind] for this app. */
internal fun permissionSettingsIntent(
    context: Context,
    kind: PermissionKind,
): Intent {
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
    val intent =
        when (kind) {
            PermissionKind.LOCATION, PermissionKind.CALENDAR -> {
                appDetails
            }

            PermissionKind.NOTIFICATIONS -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }

            PermissionKind.EXACT_ALARMS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
                } else {
                    appDetails
                }
            }

            PermissionKind.DO_NOT_DISTURB -> {
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            }
        }
    return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

private fun openPermissionSettings(
    context: Context,
    kind: PermissionKind,
) {
    runCatching { context.startActivity(permissionSettingsIntent(context, kind)) }
}
