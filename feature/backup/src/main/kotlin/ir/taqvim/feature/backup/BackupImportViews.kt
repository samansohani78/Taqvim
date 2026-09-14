/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The restore card: choosing a file, the passphrase prompt, the preview, the confirmation and the result. */
@Composable
internal fun ImportCard(
    stage: ImportStage,
    actions: BackupActions,
    fields: PassphraseFields,
) {
    CardColumn {
        SectionHeading(R.string.backup_import_title)
        Text(stringResource(R.string.backup_import_explanation), style = MaterialTheme.typography.bodyMedium)
        when (stage) {
            ImportStage.Idle -> {
                Button(onClick = actions.onChooseImport) { Text(stringResource(R.string.backup_import_choose)) }
            }

            ImportStage.Reading -> {
                ProgressLine(R.string.backup_import_reading)
            }

            is ImportStage.NeedsPassphrase -> {
                PassphrasePrompt(stage.wrongPassphrase, actions, fields)
            }

            is ImportStage.Preview -> {
                PreviewContent(stage.preview, actions)
            }

            ImportStage.Restoring -> {
                ProgressLine(R.string.backup_import_restoring)
            }

            is ImportStage.Restored -> {
                ResultMessage(
                    stringResource(R.string.backup_import_restored, stage.rowsText),
                    isError = false,
                    onDismiss = actions.onCancelImport,
                )
            }

            is ImportStage.Failed -> {
                ResultMessage(
                    stringResource(BackupLabels.failure(stage.failure)),
                    isError = true,
                    onDismiss = actions.onCancelImport,
                )
            }
        }
    }
    if (stage is ImportStage.Preview && stage.confirming) RestoreConfirmDialog(actions)
}

@Composable
private fun PassphrasePrompt(
    wrongPassphrase: Boolean,
    actions: BackupActions,
    fields: PassphraseFields,
) {
    Text(stringResource(R.string.backup_import_encrypted))
    if (wrongPassphrase) {
        Text(
            stringResource(R.string.backup_error_wrong_passphrase),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    OutlinedSecureTextField(
        state = fields.importPassphrase,
        label = { Text(stringResource(R.string.backup_passphrase)) },
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onSubmitImportPassphrase, enabled = fields.importPassphrase.text.isNotEmpty()) {
            Text(stringResource(R.string.backup_import_open))
        }
        TextButton(onClick = actions.onCancelImport) { Text(stringResource(R.string.backup_cancel)) }
    }
}

@Composable
private fun PreviewContent(
    preview: BackupPreviewText,
    actions: BackupActions,
) {
    PreviewLine(R.string.backup_preview_created, preview.createdText)
    PreviewLine(R.string.backup_preview_app_version, preview.appVersion)
    PreviewLine(R.string.backup_preview_format, preview.formatText)
    val protection = if (preview.encrypted) R.string.backup_preview_encrypted else R.string.backup_preview_plain
    PreviewLine(R.string.backup_preview_protection, stringResource(protection))
    PreviewLine(R.string.backup_preview_language, preview.languageName)
    PreviewLine(R.string.backup_preview_place, preview.placeName ?: stringResource(R.string.backup_preview_no_place))
    preview.rows.forEach { PreviewLine(BackupLabels.table(it.table), it.countText) }
    Text(stringResource(R.string.backup_restore_warning), color = MaterialTheme.colorScheme.error)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onRestoreRequested) { Text(stringResource(R.string.backup_restore)) }
        TextButton(onClick = actions.onCancelImport) { Text(stringResource(R.string.backup_cancel)) }
    }
}

@Composable
private fun PreviewLine(
    @StringRes label: Int,
    value: String,
) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RestoreConfirmDialog(actions: BackupActions) {
    AlertDialog(
        onDismissRequest = actions.onRestoreDismissed,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = { Text(stringResource(R.string.backup_restore_warning)) },
        confirmButton = {
            TextButton(onClick = actions.onRestoreConfirmed) { Text(stringResource(R.string.backup_restore_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onRestoreDismissed) { Text(stringResource(R.string.backup_cancel)) }
        },
    )
}
