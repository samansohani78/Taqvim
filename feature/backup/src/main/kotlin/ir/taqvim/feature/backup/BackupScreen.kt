/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/** Backup and restore (T-1503), stateless apart from the passphrase inputs in [fields]. */
@Composable
fun BackupScreen(
    state: BackupUiState,
    actions: BackupActions,
    fields: PassphraseFields,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.backup_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.backup_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ExportCard(state.export, actions, fields)
                    ImportCard(state.import, actions, fields)
                }
            }
        }
    }
}

@Composable
private fun ExportCard(
    export: ExportState,
    actions: BackupActions,
    fields: PassphraseFields,
) {
    CardColumn {
        SectionHeading(R.string.backup_export_title)
        Text(stringResource(R.string.backup_export_explanation), style = MaterialTheme.typography.bodyMedium)
        EncryptSwitch(export, actions.onEncryptChanged)
        val problem by remember(fields) {
            derivedStateOf { Passphrases.problem(fields.passphrase.text, fields.confirmation.text) }
        }
        if (export.encrypt) PassphraseInputs(fields, problem)
        Button(
            onClick = { actions.onExport(export.encrypt) },
            enabled = !export.working && (!export.encrypt || problem == null),
        ) { Text(stringResource(R.string.backup_export_action)) }
        when {
            export.working -> {
                ProgressLine(R.string.backup_export_working)
            }

            export.exportedSizeText != null -> {
                ResultMessage(
                    stringResource(R.string.backup_export_done, export.exportedSizeText),
                    isError = false,
                    onDismiss = actions.onExportMessageDismissed,
                )
            }

            export.failure != null -> {
                ResultMessage(
                    stringResource(BackupLabels.failure(export.failure)),
                    isError = true,
                    onDismiss = actions.onExportMessageDismissed,
                )
            }
        }
    }
}

@Composable
private fun EncryptSwitch(
    export: ExportState,
    onChange: (Boolean) -> Unit,
) {
    val title = stringResource(R.string.backup_export_encrypt)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            val detail = if (export.encrypt) R.string.backup_export_encrypt_on else R.string.backup_export_encrypt_off
            Text(stringResource(detail), style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = export.encrypt,
            onCheckedChange = onChange,
            enabled = !export.working,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun PassphraseInputs(
    fields: PassphraseFields,
    problem: PassphraseProblem?,
) {
    val strength by remember(fields) { derivedStateOf { Passphrases.strength(fields.passphrase.text) } }
    OutlinedSecureTextField(
        state = fields.passphrase,
        label = { Text(stringResource(R.string.backup_passphrase)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.backup_strength, stringResource(BackupLabels.strength(strength))),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    OutlinedSecureTextField(
        state = fields.confirmation,
        label = { Text(stringResource(R.string.backup_passphrase_confirm)) },
        modifier = Modifier.fillMaxWidth(),
    )
    val shown =
        when (problem) {
            PassphraseProblem.TOO_SHORT -> fields.passphrase.text.isNotEmpty()
            PassphraseProblem.MISMATCH -> fields.confirmation.text.isNotEmpty()
            null -> false
        }
    if (problem != null && shown) {
        Text(
            stringResource(BackupLabels.problem(problem)),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Text(stringResource(R.string.backup_passphrase_warning), style = MaterialTheme.typography.bodySmall)
}

/** A rounded card of the backup screens. */
@Composable
internal fun CardColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
internal fun SectionHeading(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun ProgressLine(
    @StringRes text: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.padding(4.dp))
        Text(stringResource(text), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
internal fun ResultMessage(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        FlowRow { TextButton(onClick = onDismiss) { Text(stringResource(R.string.backup_dismiss)) } }
    }
}
