/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone

/** The export card as the view model keeps it: raw values, formatted by [BackupStateMapper]. */
internal data class ExportModel(
    val encrypt: Boolean = true,
    val working: Boolean = false,
    val exportedBytes: Long? = null,
    val failure: BackupFailure? = null,
)

/** The import as the view model keeps it. */
internal sealed interface ImportModel {
    data object Idle : ImportModel

    data object Reading : ImportModel

    data class NeedsPassphrase(
        val wrongPassphrase: Boolean,
    ) : ImportModel

    data class Preview(
        val summary: BackupSummary,
        val confirming: Boolean,
    ) : ImportModel

    data object Restoring : ImportModel

    data class Restored(
        val rowCounts: Map<BackupTableKind, Int>,
    ) : ImportModel

    data class Failed(
        val failure: BackupFailure,
    ) : ImportModel
}

/** Formats the backup screen for the app language: digits, the creation day and language names. */
internal object BackupStateMapper {
    private const val BYTES_PER_KILOBYTE = 1_024L

    fun state(
        language: LanguageSpec,
        export: ExportModel,
        import: ImportModel,
        zone: TimeZone,
    ): BackupUiState =
        BackupUiState(
            loading = false,
            export =
                ExportState(
                    encrypt = export.encrypt,
                    working = export.working,
                    exportedSizeText = export.exportedBytes?.let { digits(kilobytes(it), language) },
                    failure = export.failure,
                ),
            import = stage(language, import, zone),
        )

    /** Size in whole kilobytes, rounded up so a small file never shows zero. */
    fun kilobytes(bytes: Long): Long = (bytes + BYTES_PER_KILOBYTE - 1) / BYTES_PER_KILOBYTE

    private fun stage(
        language: LanguageSpec,
        import: ImportModel,
        zone: TimeZone,
    ): ImportStage =
        when (import) {
            ImportModel.Idle -> {
                ImportStage.Idle
            }

            ImportModel.Reading -> {
                ImportStage.Reading
            }

            is ImportModel.NeedsPassphrase -> {
                ImportStage.NeedsPassphrase(import.wrongPassphrase)
            }

            is ImportModel.Preview -> {
                ImportStage.Preview(preview(language, import.summary, zone), import.confirming)
            }

            ImportModel.Restoring -> {
                ImportStage.Restoring
            }

            is ImportModel.Restored -> {
                ImportStage.Restored(
                    digits(
                        import.rowCounts.values
                            .sum()
                            .toLong(),
                        language,
                    ),
                )
            }

            is ImportModel.Failed -> {
                ImportStage.Failed(import.failure)
            }
        }

    private fun preview(
        language: LanguageSpec,
        summary: BackupSummary,
        zone: TimeZone,
    ) = BackupPreviewText(
        createdText = createdText(summary.createdAtEpochMillis, language, zone),
        appVersion = summary.appVersion,
        formatText = digits(summary.formatVersion.toLong(), language),
        encrypted = summary.encrypted,
        languageName = LanguageTable.forCode(summary.languageCode)?.nativeName ?: summary.languageCode,
        placeName = summary.placeName,
        rows =
            BackupTableKind.entries
                .map { TableCountRow(it, digits((summary.rowCounts[it] ?: 0).toLong(), language)) }
                .toImmutableList(),
    )

    /** The creation day in the language's first Persian or Gregorian calendar (Gregorian when it has neither). */
    fun createdText(
        epochMillis: Long,
        language: LanguageSpec,
        zone: TimeZone,
    ): String {
        val day = Instant.fromEpochMilliseconds(epochMillis).toJdn(zone)
        val calendar =
            language.calendars.firstOrNull { it == CalendarSystem.PERSIAN || it == CalendarSystem.GREGORIAN }
        val arithmetic = if (calendar == CalendarSystem.PERSIAN) PersianCalendarSystem else GregorianCalendarSystem
        return DateFormatter.format(arithmetic.fromJdn(day), day.weekday(), language, DateStyle.LONG)
    }

    private fun digits(
        value: Long,
        language: LanguageSpec,
    ): String = Numerals.localizeDigits(value.toString(), language.numerals)
}
