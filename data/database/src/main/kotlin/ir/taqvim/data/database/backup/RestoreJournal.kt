/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.preferences.UserPreferences
import java.io.File

/** Everything a restore writes: the personal tables and the preferences. */
internal data class RestoreContents(
    val data: PersonalData,
    val preferences: UserPreferences,
)

/** Which way an unfinished restore is completed. */
internal enum class RestoreDirection {
    /** Apply the backup (the process stopped while it was being applied). */
    FORWARD,

    /** Put the previous data back (applying the backup failed). */
    ROLLBACK,
}

/** A restore that was started and not finished. */
internal data class PendingRestore(
    val direction: RestoreDirection,
    val target: RestoreContents,
    val previous: RestoreContents,
)

/**
 * Durable record of a restore in progress (B09), kept as files in [directory] (app-private and excluded from backups).
 * The backup to apply and the data it replaces are written first; the direction file, written last, marks the record
 * as complete, so a record without it is ignored. Every file is replaced atomically (written beside it, then renamed).
 */
internal class RestoreJournal(
    private val directory: File,
    private val codec: BackupCodec,
) {
    private val direction = File(directory, "direction")
    private val target = File(directory, "target.bin")
    private val previous = File(directory, "previous.bin")

    /** Records that [target] is about to replace [previous]. */
    fun begin(
        target: RestoreContents,
        previous: RestoreContents,
    ) {
        clear()
        directory.mkdirs()
        replace(this.previous, encode(previous))
        replace(this.target, encode(target))
        replace(direction, RestoreDirection.FORWARD.name.toByteArray())
    }

    /** Switches the record to undoing the restore. */
    fun rollBack() = replace(direction, RestoreDirection.ROLLBACK.name.toByteArray())

    /** Whether a restore is recorded as unfinished; a cheap check that reads no snapshot. */
    fun isRecorded(): Boolean = direction.isFile

    /** The unfinished restore, or `null` when there is none (or its record is incomplete or unreadable). */
    fun pending(): PendingRestore? {
        val way =
            direction
                .takeIf { it.isFile }
                ?.readText()
                ?.let { name -> RestoreDirection.entries.find { it.name == name } }
        val wanted = decode(target)
        val before = decode(previous)
        return if (way != null && wanted != null && before != null) PendingRestore(way, wanted, before) else null
    }

    /** Forgets the record; the direction file goes first so a partly deleted record is never read. */
    fun clear() {
        direction.delete()
        target.delete()
        previous.delete()
    }

    private fun encode(contents: RestoreContents): ByteArray =
        codec.encode(contents.data, contents.preferences, JOURNAL_METADATA, BackupProtection.None)

    private fun decode(file: File): RestoreContents? =
        file
            .takeIf { it.isFile }
            ?.let { (codec.decode(it.readBytes()) as? BackupReadResult.Ready)?.backup }
            ?.let { RestoreContents(it.data, it.preferences) }

    private fun replace(
        file: File,
        bytes: ByteArray,
    ) {
        val staging = File(directory, "${file.name}.new")
        staging.writeBytes(bytes)
        check(staging.renameTo(file)) { "could not store ${file.name}" }
    }

    private companion object {
        val JOURNAL_METADATA = BackupMetadata("restore-journal", 0)
    }
}
