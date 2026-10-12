/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.core.net.toUri
import ir.taqvim.core.ics.IcsParseResult
import ir.taqvim.core.ics.IcsReader
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.events.ics.DocumentError
import ir.taqvim.data.events.ics.DocumentRead
import ir.taqvim.data.events.ics.DocumentWrite
import ir.taqvim.data.events.ics.DuplicatePolicy
import ir.taqvim.data.events.ics.IcsDocuments
import ir.taqvim.data.events.ics.IcsExporter
import ir.taqvim.data.events.ics.IcsImporter
import ir.taqvim.data.events.ics.ImportResult
import ir.taqvim.feature.settings.IcsDuplicates
import ir.taqvim.feature.settings.IcsExportOutcome
import ir.taqvim.feature.settings.IcsFilesStore
import ir.taqvim.feature.settings.IcsImportOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [IcsFilesStore] (T-1003) over the iCalendar reader and writer and the Storage Access Framework.
 *
 * The document is read or written through [IcsDocuments], which holds a size cap, so a file the picker offers but
 * Taqvim will not take is refused rather than read into memory. Everything else — matching by UID, keeping the
 * stored colour and creation time, minting a UID on export so a round trip matches instead of duplicating — is the
 * importer's and exporter's own behaviour and is unchanged by this binding.
 */
internal class DocumentIcsFilesStore(
    private val documents: IcsDocuments,
    private val importer: IcsImporter,
    private val exporter: IcsExporter,
    private val database: TaqvimDatabase,
) : IcsFilesStore {
    override fun eventCount(): Flow<Int> =
        database
            .privacyDao()
            .observeCounts()
            .map { it.personalEvents }
            .distinctUntilChanged()

    override suspend fun import(
        uri: String,
        duplicates: IcsDuplicates,
    ): IcsImportOutcome =
        when (val read = documents.read(uri.toUri())) {
            is DocumentRead.Text -> store(read.text, duplicates)
            is DocumentRead.Failed -> read.error.toImportOutcome()
        }

    private suspend fun store(
        text: String,
        duplicates: IcsDuplicates,
    ): IcsImportOutcome =
        when (val result = importer.import(text, duplicates.toPolicy())) {
            is ImportResult.Unreadable -> {
                IcsImportOutcome.NotCalendar
            }

            is ImportResult.Imported -> {
                IcsImportOutcome.Imported(
                    created = result.created,
                    replaced = result.replaced,
                    skipped = result.skipped,
                    // Both lists are things the reader could not take fully, so the page reports them together.
                    problems = result.warnings.size + result.problems.size,
                )
            }
        }

    override suspend fun export(uri: String): IcsExportOutcome {
        val text = exporter.export()
        return when (val written = documents.write(uri.toUri(), text)) {
            DocumentWrite.Written -> IcsExportOutcome.Exported(eventsIn(text))
            is DocumentWrite.Failed -> written.error.toExportOutcome()
        }
    }

    /**
     * How many components the written file holds. Counted from the text that was actually written rather than from
     * the row count, because an event with overridden occurrences writes more than one component and the user is
     * being told what is in the file.
     */
    private fun eventsIn(text: String): Int =
        when (val parsed = IcsReader.read(text)) {
            is IcsParseResult.Success -> parsed.calendar.events.size
            is IcsParseResult.Failure -> 0
        }
}

private fun IcsDuplicates.toPolicy(): DuplicatePolicy =
    when (this) {
        IcsDuplicates.REPLACE -> DuplicatePolicy.REPLACE
        IcsDuplicates.SKIP -> DuplicatePolicy.SKIP
    }

private fun DocumentError.toImportOutcome(): IcsImportOutcome =
    when (this) {
        DocumentError.TOO_LARGE -> IcsImportOutcome.TooLarge
        DocumentError.NOT_FOUND, DocumentError.ACCESS_DENIED -> IcsImportOutcome.Unreadable
        DocumentError.IO_ERROR -> IcsImportOutcome.Failed
    }

private fun DocumentError.toExportOutcome(): IcsExportOutcome =
    when (this) {
        DocumentError.NOT_FOUND, DocumentError.ACCESS_DENIED -> IcsExportOutcome.Unwritable
        DocumentError.TOO_LARGE, DocumentError.IO_ERROR -> IcsExportOutcome.Failed
    }
