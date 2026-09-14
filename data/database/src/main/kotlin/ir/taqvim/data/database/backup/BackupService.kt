/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Backup and restore of the user's settings and personal data (F-11, T-605). Export and read run on [dispatcher]
 * because key derivation is deliberately slow. A restore replaces the personal tables in one Room transaction and then
 * the preferences; if the transaction fails, nothing is changed.
 */
class BackupService(
    private val database: TaqvimDatabase,
    private val preferences: UserPreferencesRepository,
    private val codec: BackupCodec = BackupCodec(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** The backup file of the current data and preferences. */
    suspend fun export(
        metadata: BackupMetadata,
        protection: BackupProtection,
    ): ByteArray {
        val data = database.backupDao().snapshot()
        val current = preferences.preferences.first()
        return withContext(dispatcher) { codec.encode(data, current, metadata, protection) }
    }

    /** Reads a backup file for preview; nothing is applied yet. */
    suspend fun read(
        bytes: ByteArray,
        passphrase: CharArray? = null,
    ): BackupReadResult = withContext(dispatcher) { codec.decode(bytes, passphrase) }

    /** Replaces the personal data and preferences with [backup]. Scheduled alarms are left to the scheduler. */
    suspend fun restore(backup: RestorableBackup): RestoreResult =
        runCatching {
            database.backupDao().replaceAll(backup.data)
            preferences.update { current -> backup.preferences.keepingDeviceOnlyValuesOf(current) }
        }.fold(
            onSuccess = { RestoreResult.Restored(backup.preview.rowCounts) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                RestoreResult.Failed(BackupError.RestoreFailed(error.javaClass.name))
            },
        )
}
