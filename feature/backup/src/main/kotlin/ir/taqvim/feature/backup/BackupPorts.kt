/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import ir.taqvim.core.i18n.LanguageSpec
import kotlinx.coroutines.flow.Flow

/** Tables a backup carries, shown in the restore preview. */
enum class BackupTableKind {
    PERSONAL_EVENTS,
    EVENT_RECURRENCES,
    REMINDERS,
    SHIFT_ROTATIONS,
    SHIFT_ROTATION_RECORDS,
    ICS_SUBSCRIPTIONS,
    WORKDAY_PROFILES,
}

/** Why a backup could not be written, read or restored. */
enum class BackupFailure {
    /** Neither an encrypted Taqvim backup nor a Taqvim JSON export. */
    NOT_A_BACKUP,

    /** The file ends too early. */
    TRUNCATED,

    /** Made by a newer version of Taqvim. */
    UNSUPPORTED_FORMAT,

    /** The backup is encrypted and no passphrase was given. */
    PASSPHRASE_REQUIRED,

    /** The passphrase does not match the one the backup was made with. */
    WRONG_PASSPHRASE,

    /** The file was changed or damaged. */
    CORRUPTED,

    /** Readable, but its content breaks the backup format. */
    INVALID_CONTENT,

    /** The device refused to replace the data; nothing was changed. */
    RESTORE_FAILED,

    /** The chosen file could not be opened. */
    FILE_UNREADABLE,

    /** The chosen destination could not be written. */
    FILE_UNWRITABLE,
}

/** What a backup contains, shown before it replaces the user's data. */
data class BackupSummary(
    val formatVersion: Int,
    /** Version of the app that made the backup. */
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val encrypted: Boolean,
    val rowCounts: Map<BackupTableKind, Int>,
    /** Language code stored in the backup's settings. */
    val languageCode: String,
    /** Name of the place stored in the backup's settings, or `null` when none is stored. */
    val placeName: String?,
)

/** A backup that was read and validated and can be restored. */
interface OpenedBackup {
    val summary: BackupSummary
}

/** Result of writing a backup. */
sealed interface BackupExportResult {
    data class Exported(
        val sizeBytes: Long,
    ) : BackupExportResult

    data class Failed(
        val failure: BackupFailure,
    ) : BackupExportResult
}

/** Result of reading a backup for its preview. */
sealed interface BackupOpenResult {
    data class Ready(
        val backup: OpenedBackup,
    ) : BackupOpenResult

    data class Failed(
        val failure: BackupFailure,
    ) : BackupOpenResult
}

/** Result of replacing the user's data with a backup. */
sealed interface BackupRestoreResult {
    data class Restored(
        val rowCounts: Map<BackupTableKind, Int>,
    ) : BackupRestoreResult

    data class Failed(
        val failure: BackupFailure,
    ) : BackupRestoreResult
}

/**
 * Backup file operations (F-11); bound in `:app` over the T-605 backup service and the Storage Access Framework. A
 * passphrase array belongs to the caller, who wipes it after the call returns; implementations must not keep it.
 */
interface BackupOperations {
    /** Writes the current data to [destinationUri], encrypted with [passphrase] or as plain JSON when `null`. */
    suspend fun export(
        destinationUri: String,
        passphrase: CharArray?,
    ): BackupExportResult

    /** Reads the backup at [sourceUri] for its preview; nothing is applied yet. */
    suspend fun open(
        sourceUri: String,
        passphrase: CharArray?,
    ): BackupOpenResult

    /** Replaces the user's data and settings with [backup]. */
    suspend fun restore(backup: OpenedBackup): BackupRestoreResult
}

/** The app language, for digits, dates and language names; bound in `:app` over the user preferences. */
fun interface BackupLanguageSource {
    fun language(): Flow<LanguageSpec>
}
