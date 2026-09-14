/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/** State of the backup screen (T-1503). Passphrases are never part of it. */
data class BackupUiState(
    val loading: Boolean = true,
    val export: ExportState = ExportState(),
    val import: ImportStage = ImportStage.Idle,
)

/** The export card. */
data class ExportState(
    /** Whether the backup is encrypted with a passphrase instead of written as plain JSON. */
    val encrypt: Boolean = true,
    val working: Boolean = false,
    /** Size of the file just written, in kilobytes in the app's digits, or `null`. */
    val exportedSizeText: String? = null,
    val failure: BackupFailure? = null,
)

/** Where the import stands. */
sealed interface ImportStage {
    data object Idle : ImportStage

    data object Reading : ImportStage

    /** The backup is encrypted; [wrongPassphrase] after a passphrase that did not match. */
    data class NeedsPassphrase(
        val wrongPassphrase: Boolean,
    ) : ImportStage

    /** The backup was read; [confirming] while the overwrite warning is shown. */
    data class Preview(
        val preview: BackupPreviewText,
        val confirming: Boolean,
    ) : ImportStage

    data object Restoring : ImportStage

    /** Restored [rowsText] rows (in the app's digits). */
    data class Restored(
        val rowsText: String,
    ) : ImportStage

    data class Failed(
        val failure: BackupFailure,
    ) : ImportStage
}

/** The restore preview, formatted for the app language. */
data class BackupPreviewText(
    /** Creation day in the language's calendar. */
    val createdText: String,
    val appVersion: String,
    val formatText: String,
    val encrypted: Boolean,
    /** Native name of the language stored in the backup, or its code when unknown. */
    val languageName: String,
    val placeName: String?,
    val rows: ImmutableList<TableCountRow>,
)

data class TableCountRow(
    val table: BackupTableKind,
    val countText: String,
)

/** User actions of the backup screen. */
@Immutable
data class BackupActions(
    val onEncryptChanged: (Boolean) -> Unit = {},
    /** Asks for a destination; the argument tells whether the backup is encrypted. */
    val onExport: (Boolean) -> Unit = {},
    val onExportMessageDismissed: () -> Unit = {},
    val onChooseImport: () -> Unit = {},
    val onSubmitImportPassphrase: () -> Unit = {},
    val onCancelImport: () -> Unit = {},
    val onRestoreRequested: () -> Unit = {},
    val onRestoreDismissed: () -> Unit = {},
    val onRestoreConfirmed: () -> Unit = {},
)
