/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.runtime.Composable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone

/** Synthetic backups and fakes of the backup and privacy ports. */
internal object BackupFixtures {
    const val SOURCE_URI: String = "content://documents/backup/7"
    const val DESTINATION_URI: String = "content://documents/new/8"
    const val PASSPHRASE: String = "kooh-e Damavand 5671"

    val english: LanguageSpec = requireNotNull(LanguageTable.forCode("en"))
    val persian: LanguageSpec = requireNotNull(LanguageTable.forCode("fa"))
    val tehran: TimeZone = TimeZone.of("Asia/Tehran")

    /** Made at noon in Tehran on 13 September 2026 (22 Shahrivar 1405), a Sunday. */
    val summary: BackupSummary =
        BackupSummary(
            formatVersion = 1,
            appVersion = "1.0.0",
            createdAtEpochMillis = Instant.parse("2026-09-13T08:30:00Z").toEpochMilliseconds(),
            encrypted = true,
            rowCounts =
                mapOf(
                    BackupTableKind.PERSONAL_EVENTS to 12,
                    BackupTableKind.EVENT_RECURRENCES to 3,
                    BackupTableKind.REMINDERS to 20,
                    BackupTableKind.ICS_SUBSCRIPTIONS to 2,
                ),
            languageCode = "fa",
            placeName = "Tehran",
        )

    val plainSummary: BackupSummary = summary.copy(encrypted = false)

    val stored: List<StoredData> =
        listOf(
            StoredData(StoredDataKind.PERSONAL_EVENTS, 12),
            StoredData(StoredDataKind.REMINDERS, 20),
            StoredData(StoredDataKind.LOCATION, 1),
            StoredData(StoredDataKind.DEVICE_CALENDAR_CACHE, 140),
            StoredData(StoredDataKind.RECENT_SEARCHES, 4),
        )

    val granted: List<PermissionStatus> =
        listOf(
            PermissionStatus(PermissionKind.LOCATION, true),
            PermissionStatus(PermissionKind.NOTIFICATIONS, true),
        )

    /** The privacy dashboard as its view model shows [stored] and [granted] in [language]. */
    fun privacyState(language: LanguageSpec): PrivacyUiState {
        val digits = language.numerals
        return PrivacyUiState(
            loading = false,
            data =
                StoredDataKind.entries
                    .map { kind ->
                        val count = stored.firstOrNull { it.kind == kind }?.count ?: 0
                        StoredDataRow(
                            kind,
                            ir.taqvim.core.i18n.Numerals
                                .localizeDigits(count.toString(), digits),
                            count == 0,
                            kind.clearable && count > 0,
                        )
                    }.toImmutableList(),
            permissions =
                PermissionKind.entries
                    .map { kind -> granted.firstOrNull { it.kind == kind } ?: PermissionStatus(kind, false) }
                    .toImmutableList(),
        )
    }
}

internal data class FakeBackup(
    override val summary: BackupSummary,
) : OpenedBackup

internal class FakeLanguages(
    language: LanguageSpec,
) : BackupLanguageSource {
    val state = MutableStateFlow(language)

    override fun language(): Flow<LanguageSpec> = state
}

/**
 * Records every call with the passphrase text it carried at that moment, and keeps the passphrase arrays themselves so
 * tests can check they were wiped afterwards.
 */
internal class FakeOperations : BackupOperations {
    var exportResult: BackupExportResult = BackupExportResult.Exported(12_345)
    var openResult: (String?) -> BackupOpenResult = { BackupOpenResult.Ready(FakeBackup(BackupFixtures.plainSummary)) }
    var restoreResult: BackupRestoreResult =
        BackupRestoreResult.Restored(BackupFixtures.summary.rowCounts)

    /** When set, exports wait for it to complete. */
    var exportGate: CompletableDeferred<Unit>? = null

    val exports = mutableListOf<Pair<String, String?>>()
    val opens = mutableListOf<Pair<String, String?>>()
    val passphraseArrays = mutableListOf<CharArray>()
    var restores: Int = 0
        private set

    override suspend fun export(
        destinationUri: String,
        passphrase: CharArray?,
    ): BackupExportResult {
        exports += destinationUri to passphrase?.concatToString()
        passphrase?.let(passphraseArrays::add)
        exportGate?.await()
        return exportResult
    }

    override suspend fun open(
        sourceUri: String,
        passphrase: CharArray?,
    ): BackupOpenResult {
        val text = passphrase?.concatToString()
        opens += sourceUri to text
        passphrase?.let(passphraseArrays::add)
        return openResult(text)
    }

    override suspend fun restore(backup: OpenedBackup): BackupRestoreResult {
        restores++
        return restoreResult
    }

    /** Encrypted backups: no passphrase asks for one, [BackupFixtures.PASSPHRASE] opens it, anything else is wrong. */
    fun encrypted() {
        openResult = { text ->
            when (text) {
                null -> BackupOpenResult.Failed(BackupFailure.PASSPHRASE_REQUIRED)
                BackupFixtures.PASSPHRASE -> BackupOpenResult.Ready(FakeBackup(BackupFixtures.summary))
                else -> BackupOpenResult.Failed(BackupFailure.WRONG_PASSPHRASE)
            }
        }
    }
}

internal class FakePrivacyData(
    stored: List<StoredData> = BackupFixtures.stored,
) : PrivacyDataSource {
    val state = MutableStateFlow(stored)
    var clearSucceeds: Boolean = true
    val cleared = mutableListOf<StoredDataKind>()

    override fun storedData(): Flow<List<StoredData>> = state

    override suspend fun clear(kind: StoredDataKind): Boolean {
        cleared += kind
        if (clearSucceeds) state.update { list -> list.filterNot { it.kind == kind } }
        return clearSucceeds
    }
}

internal class FakePermissions(
    granted: List<PermissionStatus> = BackupFixtures.granted,
) : PermissionStatusSource {
    val state = MutableStateFlow(granted)
    var refreshes: Int = 0
        private set

    override fun statuses(): Flow<List<PermissionStatus>> = state

    override fun refresh() {
        refreshes++
    }
}

@Composable
internal fun BackupTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
