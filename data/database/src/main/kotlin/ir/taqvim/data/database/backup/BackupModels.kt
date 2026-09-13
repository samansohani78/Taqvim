/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.preferences.UserPreferences

/** Tables a backup carries, for the restore preview. */
enum class BackupTable {
    PERSONAL_EVENTS,
    EVENT_RECURRENCES,
    REMINDERS,
    SHIFT_ROTATIONS,
    SHIFT_ROTATION_RECORDS,
    ICS_SUBSCRIPTIONS,
    WORKDAY_PROFILES,
}

/** Written into every backup: the app version that made it and when. */
data class BackupMetadata(
    val appVersion: String,
    val createdAtEpochMillis: Long,
)

/** How an exported backup is protected. */
sealed interface BackupProtection {
    /** Plain JSON export, readable by anyone who has the file. */
    data object None : BackupProtection

    /** Encrypted with a key derived from [passphrase]; the caller may wipe the array after use. */
    class Passphrase(
        val passphrase: CharArray,
    ) : BackupProtection
}

/** What the user sees before a restore replaces their data. */
data class BackupPreview(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val encrypted: Boolean,
    val rowCounts: Map<BackupTable, Int>,
    val preferences: UserPreferences,
)

/** A backup that was read, decrypted and validated; pass it to `BackupService.restore`. */
class RestorableBackup internal constructor(
    val preview: BackupPreview,
    internal val data: PersonalData,
    internal val preferences: UserPreferences,
)

/** Why a backup could not be read or restored. Reasons are diagnostics, not user-facing text. */
sealed interface BackupError {
    /** Neither an encrypted backup nor a Taqvim JSON export. */
    data object NotABackup : BackupError

    /** The file ends before its header or tag. */
    data object Truncated : BackupError

    /** Written by a newer (or unknown) format version. */
    data class UnsupportedFormat(
        val formatVersion: Int,
    ) : BackupError

    /** The backup is encrypted and no passphrase was given. */
    data object PassphraseRequired : BackupError

    /** The passphrase does not match the one the backup was made with. */
    data object WrongPassphrase : BackupError

    /** The file was modified or damaged (authentication failed, or the JSON is malformed). */
    data object Corrupted : BackupError

    /** Well-formed, but the document breaks the format (missing fields, dangling references, invalid values). */
    data class InvalidContent(
        val reason: String,
    ) : BackupError

    /** Storage refused the restore; nothing was changed in the database. */
    data class RestoreFailed(
        val reason: String,
    ) : BackupError
}

/** Result of reading a backup file. */
sealed interface BackupReadResult {
    data class Ready(
        val backup: RestorableBackup,
    ) : BackupReadResult

    data class Failed(
        val error: BackupError,
    ) : BackupReadResult
}

/** Result of applying a backup. */
sealed interface RestoreResult {
    data class Restored(
        val rowCounts: Map<BackupTable, Int>,
    ) : RestoreResult

    data class Failed(
        val error: BackupError,
    ) : RestoreResult
}

/** Rows per table in [this]. */
internal fun PersonalData.rowCounts(): Map<BackupTable, Int> =
    mapOf(
        BackupTable.PERSONAL_EVENTS to events.size,
        BackupTable.EVENT_RECURRENCES to recurrences.size,
        BackupTable.REMINDERS to reminders.size,
        BackupTable.SHIFT_ROTATIONS to shiftRotations.size,
        BackupTable.SHIFT_ROTATION_RECORDS to shiftRecords.size,
        BackupTable.ICS_SUBSCRIPTIONS to icsSubscriptions.size,
        BackupTable.WORKDAY_PROFILES to workdayProfiles.size,
    )
