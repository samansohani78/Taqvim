/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import kotlinx.coroutines.flow.Flow

/** What to do with an imported event whose iCalendar UID is already stored (T-1003). */
enum class IcsDuplicates {
    /** Overwrite the stored event, its rule and its reminders, keeping its colour and creation time. */
    REPLACE,

    /** Keep the stored event and count the incoming one as skipped. */
    SKIP,
}

/** What importing an `.ics` file did. */
sealed interface IcsImportOutcome {
    /**
     * Events [created], [replaced] and [skipped], and how many components the reader could not take fully
     * ([problems]: an unsupported recurrence, an unreadable field). A file can import and still have problems.
     */
    data class Imported(
        val created: Int,
        val replaced: Int,
        val skipped: Int,
        val problems: Int,
    ) : IcsImportOutcome

    /** The file is not a readable iCalendar stream; nothing was stored. */
    data object NotCalendar : IcsImportOutcome

    /** The file is larger than Taqvim reads. */
    data object TooLarge : IcsImportOutcome

    /** The picked file is gone, or the app may no longer read it. */
    data object Unreadable : IcsImportOutcome

    /** Something else failed; nothing was stored. */
    data object Failed : IcsImportOutcome
}

/** What exporting to an `.ics` file did. */
sealed interface IcsExportOutcome {
    data class Exported(
        val events: Int,
    ) : IcsExportOutcome

    /** The picked file could not be written, or the app may no longer write it. */
    data object Unwritable : IcsExportOutcome

    data object Failed : IcsExportOutcome
}

/**
 * Imports and exports personal events as `.ics` files (T-1003, ADR-0054); bound in `:app` over the iCalendar reader,
 * writer and the Storage Access Framework.
 *
 * Both directions act only on a document the user picked. Taqvim needs no storage permission to read or write it,
 * and no network permission, because the file is already on the device by the time it gets here.
 */
interface IcsFilesStore {
    /** How many personal events an export would write; the screen uses it to say what is about to leave. */
    fun eventCount(): Flow<Int>

    /** Imports the `.ics` document at the content address [uri] (from the system file picker). */
    suspend fun import(
        uri: String,
        duplicates: IcsDuplicates,
    ): IcsImportOutcome

    /** Writes every personal event as iCalendar text to the document at [uri] (from the system file picker). */
    suspend fun export(uri: String): IcsExportOutcome
}
