/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/** The privacy dashboard (T-1503, F-15), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    actions: PrivacyActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.privacy_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.privacy_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                PrivacyContent(state, actions)
            }
        }
    }
    state.confirmClear?.let { ClearConfirmDialog(it, actions) }
}

@Composable
private fun PrivacyContent(
    state: PrivacyUiState,
    actions: PrivacyActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.privacy_permission_free), style = MaterialTheme.typography.bodyMedium)
        SectionHeading(R.string.privacy_data_title)
        state.data.forEach { DataItem(it, actions) }
        state.clearFailed?.let {
            Text(
                stringResource(R.string.privacy_clear_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        SectionHeading(R.string.privacy_permissions_title)
        state.permissions.forEach { PermissionItem(it, actions) }
    }
}

@Composable
private fun DataItem(
    row: StoredDataRow,
    actions: PrivacyActions,
) {
    CardColumn {
        Text(stringResource(BackupLabels.dataTitle(row.kind)), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(BackupLabels.dataWhere(row.kind)), style = MaterialTheme.typography.bodySmall)
        val amount =
            when {
                row.kind != StoredDataKind.LOCATION -> stringResource(R.string.privacy_entries, row.countText)
                row.empty -> stringResource(R.string.privacy_location_none)
                else -> stringResource(R.string.privacy_location_set)
            }
        Text(amount, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        val clear = BackupLabels.clearAction(row.kind)
        if (row.canClear && clear != null) {
            TextButton(onClick = { actions.onClearRequested(row.kind) }) { Text(stringResource(clear)) }
        }
    }
}

@Composable
private fun PermissionItem(
    status: PermissionStatus,
    actions: PrivacyActions,
) {
    val title = stringResource(BackupLabels.permissionTitle(status.kind))
    CardColumn {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(BackupLabels.permissionWhy(status.kind)), style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(
                if (status.granted) R.string.privacy_permission_granted else R.string.privacy_permission_not_granted,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (status.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val description = stringResource(R.string.privacy_open_settings_for, title)
        TextButton(
            onClick = { actions.onOpenPermissionSettings(status.kind) },
            modifier = Modifier.semantics { contentDescription = description },
        ) { Text(stringResource(R.string.privacy_open_settings)) }
    }
}

@Composable
private fun ClearConfirmDialog(
    kind: StoredDataKind,
    actions: PrivacyActions,
) {
    AlertDialog(
        onDismissRequest = actions.onClearDismissed,
        title = { Text(stringResource(R.string.privacy_clear_confirm_title)) },
        text = { Text(stringResource(BackupLabels.dataTitle(kind))) },
        confirmButton = {
            TextButton(onClick = actions.onClearConfirmed) { Text(stringResource(R.string.privacy_clear_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onClearDismissed) { Text(stringResource(R.string.backup_cancel)) }
        },
    )
}
