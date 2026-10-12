/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

/**
 * Calendar files (T-1003), stateless: import personal events from an `.ics` file and export them to one.
 *
 * [onPickImport] and [onPickExport] open the system file picker; the route reports what the user chose. Nothing
 * here reaches the network or the device's storage at large — only the one document the user handed over.
 */
@Composable
fun IcsFilesScreen(
    state: IcsFilesUiState,
    actions: IcsFilesActions,
    onPickImport: () -> Unit,
    onPickExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.settings_ics_files_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val count = state.eventCount
            if (count == null) {
                val description = stringResource(R.string.settings_ics_files_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                IcsFilesContent(state, count, actions, onPickImport, onPickExport)
            }
        }
    }
}

@Composable
private fun IcsFilesContent(
    state: IcsFilesUiState,
    eventCount: Int,
    actions: IcsFilesActions,
    onPickImport: () -> Unit,
    onPickExport: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_ics_files_intro), style = MaterialTheme.typography.bodyMedium)

        Text(stringResource(R.string.settings_ics_files_import_heading), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_ics_files_import_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SwitchItem(
            title = stringResource(R.string.settings_ics_files_replace),
            detail = stringResource(R.string.settings_ics_files_replace_detail),
            checked = state.duplicates == IcsDuplicates.REPLACE,
            onChange = { replace ->
                actions.onDuplicatesChanged(if (replace) IcsDuplicates.REPLACE else IcsDuplicates.SKIP)
            },
            enabled = !state.busy,
        )
        Button(onClick = onPickImport, enabled = !state.busy) {
            Text(stringResource(R.string.settings_ics_files_import))
        }

        Text(stringResource(R.string.settings_ics_files_export_heading), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_ics_files_export_detail, eventCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPickExport, enabled = !state.busy && eventCount > 0) {
            Text(stringResource(R.string.settings_ics_files_export))
        }

        OutcomeText(state)
    }
}

@Composable
private fun OutcomeText(state: IcsFilesUiState) {
    val importOutcome = state.importOutcome
    val exportOutcome = state.exportOutcome
    val text =
        when {
            importOutcome is IcsImportOutcome.Imported -> {
                stringResource(
                    R.string.settings_ics_files_imported,
                    importOutcome.created,
                    importOutcome.replaced,
                    importOutcome.skipped,
                )
            }

            importOutcome != null -> {
                stringResource(importFailureText(importOutcome))
            }

            exportOutcome is IcsExportOutcome.Exported -> {
                stringResource(R.string.settings_ics_files_exported, exportOutcome.events)
            }

            exportOutcome != null -> {
                stringResource(exportFailureText(exportOutcome))
            }

            else -> {
                return
            }
        }
    val succeeded = importOutcome is IcsImportOutcome.Imported || exportOutcome is IcsExportOutcome.Exported
    Column(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text,
            color = if (succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        // An import can succeed and still have left something out; saying so is the only way the user can tell.
        val problems = (importOutcome as? IcsImportOutcome.Imported)?.problems ?: 0
        if (problems > 0) {
            Text(
                stringResource(R.string.settings_ics_files_problems, problems),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@StringRes
internal fun importFailureText(outcome: IcsImportOutcome): Int =
    when (outcome) {
        is IcsImportOutcome.Imported -> R.string.settings_ics_files_imported
        IcsImportOutcome.NotCalendar -> R.string.settings_ics_files_error_not_calendar
        IcsImportOutcome.TooLarge -> R.string.settings_ics_files_error_too_large
        IcsImportOutcome.Unreadable -> R.string.settings_ics_files_error_unreadable
        IcsImportOutcome.Failed -> R.string.settings_ics_files_error_failed
    }

@StringRes
internal fun exportFailureText(outcome: IcsExportOutcome): Int =
    when (outcome) {
        is IcsExportOutcome.Exported -> R.string.settings_ics_files_exported
        IcsExportOutcome.Unwritable -> R.string.settings_ics_files_error_unwritable
        IcsExportOutcome.Failed -> R.string.settings_ics_files_error_failed
    }
