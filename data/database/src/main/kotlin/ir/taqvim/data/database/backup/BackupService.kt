/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferencesRepository
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Backup and restore of the user's settings and personal data (F-11, T-605). Export and read run on [dispatcher]
 * because key derivation is deliberately slow.
 *
 * A restore writes two stores, the Room tables and the preferences, which cannot share one transaction (B09). It is
 * therefore recorded first in a [RestoreJournal] under [journalDirectory]: if applying fails, the previous data is put
 * back; if the process stops part-way, or putting the previous data back fails too, [recover] finishes the job on the
 * next start. Until then the restore is reported as [BackupError.RestorePending].
 */
class BackupService(
    private val database: TaqvimDatabase,
    private val preferences: UserPreferencesRepository,
    journalDirectory: File,
    private val codec: BackupCodec = BackupCodec(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val journal = RestoreJournal(journalDirectory, codec)
    private val lock = Mutex()

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

    /**
     * Replaces the personal data and preferences with [backup]; on failure the previous data is put back. Scheduled
     * alarms are left to the scheduler, which must only be told after [RestoreResult.Restored].
     */
    suspend fun restore(backup: RestorableBackup): RestoreResult =
        lock.withLock {
            if (finishPending() is RecoveryResult.StillPending) return@withLock pendingFailure()
            val previous = RestoreContents(database.backupDao().snapshot(), preferences.preferences.first())
            val target = RestoreContents(backup.data, backup.preferences)
            val recorded = attempt { withContext(dispatcher + NonCancellable) { journal.begin(target, previous) } }
            if (recorded != null) {
                journal.clear()
                return@withLock RestoreResult.Failed(BackupError.RestoreFailed(recorded))
            }
            val failure = withContext(NonCancellable) { attempt { apply(target) } }
            if (failure == null) return@withLock restored(backup.preview.rowCounts)
            val undone = withContext(NonCancellable) { undo(previous) }
            if (undone) RestoreResult.Failed(BackupError.RestoreFailed(failure)) else pendingFailure()
        }

    /**
     * Finishes a restore that the previous process left unfinished: applies the backup if it was interrupted, or puts
     * the previous data back if applying had failed. Call at start-up, before anything schedules from the data.
     */
    suspend fun recover(): RecoveryResult = lock.withLock { withContext(NonCancellable) { finishPending() } }

    private suspend fun finishPending(): RecoveryResult {
        val pending = withContext(dispatcher) { journal.pending() } ?: return RecoveryResult.NothingPending
        if (pending.direction == RestoreDirection.FORWARD && attempt { apply(pending.target) } == null) {
            journal.clear()
            return RecoveryResult.Completed(pending.target.data.rowCounts())
        }
        return if (undo(pending.previous)) RecoveryResult.RolledBack else RecoveryResult.StillPending
    }

    /** Puts [previous] back; `true` when it is in place and the record is gone. */
    private suspend fun undo(previous: RestoreContents): Boolean {
        val undone = attempt { journal.rollBack() } == null && attempt { apply(previous) } == null
        if (undone) journal.clear()
        return undone
    }

    private suspend fun apply(contents: RestoreContents) {
        database.backupDao().replaceAll(contents.data)
        preferences.update { current -> contents.preferences.keepingDeviceOnlyValuesOf(current) }
    }

    private fun restored(rowCounts: Map<BackupTable, Int>): RestoreResult {
        journal.clear()
        return RestoreResult.Restored(rowCounts)
    }

    private fun pendingFailure(): RestoreResult =
        RestoreResult.Failed(BackupError.RestorePending("an earlier restore is unfinished"))

    /** `null` when [block] succeeds, else the failure's class name; cancellation is never swallowed. */
    private suspend fun attempt(block: suspend () -> Unit): String? =
        runCatching { block() }.fold(
            onSuccess = { null },
            onFailure = { error ->
                if (error is CancellationException) throw error
                error.javaClass.name
            },
        )
}
