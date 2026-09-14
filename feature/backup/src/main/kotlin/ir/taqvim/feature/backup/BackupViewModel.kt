/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * Backup and restore (T-1503, F-11): export to a chosen file, plain or encrypted; import with a passphrase prompt, a
 * preview and a confirmed restore. Passphrases arrive as arrays that are wiped as soon as the operation using them ends
 * (also when it is cancelled); they are never stored in the state, so they do not survive process death.
 */
class BackupViewModel(
    private val operations: BackupOperations,
    languages: BackupLanguageSource,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    private val export = MutableStateFlow(ExportModel())
    private val import = MutableStateFlow<ImportModel>(ImportModel.Idle)
    private var sourceUri: String? = null
    private var opened: OpenedBackup? = null

    init {
        combine(languages.language(), export, import) { language, export, import ->
            BackupStateMapper.state(language, export, import, zone)
        }.onEach { state.value = it }
            .launchIn(viewModelScope)
    }

    fun onEncryptChanged(encrypt: Boolean) {
        export.update { if (it.working) it else ExportModel(encrypt = encrypt) }
    }

    /** Writes the backup to [destinationUri]; [passphrase] is used only when encrypting and is always wiped. */
    fun onExportDestination(
        destinationUri: String,
        passphrase: CharArray,
    ) {
        val current = export.value
        val usable = !current.encrypt || passphrase.size >= Passphrases.MIN_LENGTH
        if (current.working || !usable) {
            Passphrases.wipe(passphrase)
            if (!usable) export.update { it.copy(failure = BackupFailure.PASSPHRASE_REQUIRED, exportedBytes = null) }
            return
        }
        export.update { it.copy(working = true, failure = null, exportedBytes = null) }
        viewModelScope
            .launch {
                val result = operations.export(destinationUri, passphrase.takeIf { current.encrypt })
                export.update {
                    when (result) {
                        is BackupExportResult.Exported -> it.copy(working = false, exportedBytes = result.sizeBytes)
                        is BackupExportResult.Failed -> it.copy(working = false, failure = result.failure)
                    }
                }
            }.invokeOnCompletion { Passphrases.wipe(passphrase) }
    }

    fun onExportMessageDismissed() {
        export.update { it.copy(exportedBytes = null, failure = null) }
    }

    /** A file picked for import; `null` when the picker was cancelled. */
    fun onImportSource(uri: String?) {
        if (uri == null || import.value.busy()) return
        sourceUri = uri
        opened = null
        open(uri, passphrase = null)
    }

    /** The passphrase typed for the encrypted backup being imported; always wiped. */
    fun onImportPassphrase(passphrase: CharArray) {
        val uri = sourceUri
        if (uri == null || import.value !is ImportModel.NeedsPassphrase) {
            Passphrases.wipe(passphrase)
            return
        }
        open(uri, passphrase)
    }

    fun onRestoreRequested() {
        import.update { (it as? ImportModel.Preview)?.copy(confirming = true) ?: it }
    }

    fun onRestoreDismissed() {
        import.update { (it as? ImportModel.Preview)?.copy(confirming = false) ?: it }
    }

    /** Replaces the user's data with the previewed backup. */
    fun onRestoreConfirmed() {
        val backup = opened ?: return
        if (import.value !is ImportModel.Preview) return
        import.value = ImportModel.Restoring
        viewModelScope.launch {
            import.value =
                when (val result = operations.restore(backup)) {
                    is BackupRestoreResult.Restored -> ImportModel.Restored(result.rowCounts)
                    is BackupRestoreResult.Failed -> ImportModel.Failed(result.failure)
                }
            forgetSource()
        }
    }

    /** Leaves the passphrase prompt, preview or result and returns to choosing a file. */
    fun onImportDismissed() {
        if (import.value.busy()) return
        forgetSource()
        import.value = ImportModel.Idle
    }

    private fun open(
        uri: String,
        passphrase: CharArray?,
    ) {
        import.value = ImportModel.Reading
        viewModelScope
            .launch {
                import.value =
                    when (val result = operations.open(uri, passphrase)) {
                        is BackupOpenResult.Ready -> {
                            opened = result.backup
                            ImportModel.Preview(result.backup.summary, confirming = false)
                        }

                        is BackupOpenResult.Failed -> {
                            failedOpen(result.failure)
                        }
                    }
            }.invokeOnCompletion { passphrase?.let(Passphrases::wipe) }
    }

    private fun failedOpen(failure: BackupFailure): ImportModel =
        when (failure) {
            BackupFailure.PASSPHRASE_REQUIRED -> {
                ImportModel.NeedsPassphrase(wrongPassphrase = false)
            }

            BackupFailure.WRONG_PASSPHRASE -> {
                ImportModel.NeedsPassphrase(wrongPassphrase = true)
            }

            else -> {
                forgetSource()
                ImportModel.Failed(failure)
            }
        }

    private fun forgetSource() {
        sourceUri = null
        opened = null
    }

    private fun ImportModel.busy(): Boolean = this == ImportModel.Reading || this == ImportModel.Restoring
}
