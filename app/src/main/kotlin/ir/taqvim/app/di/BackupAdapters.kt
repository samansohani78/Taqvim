/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.StoredRowCounts
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.backup.BackupError
import ir.taqvim.data.database.backup.BackupMetadata
import ir.taqvim.data.database.backup.BackupPreview
import ir.taqvim.data.database.backup.BackupProtection
import ir.taqvim.data.database.backup.BackupReadResult
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.database.backup.BackupTable
import ir.taqvim.data.database.backup.RestorableBackup
import ir.taqvim.data.database.backup.RestoreResult
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.RescheduleEvent
import ir.taqvim.data.scheduler.SchedulerEvents
import ir.taqvim.feature.backup.BackupExportResult
import ir.taqvim.feature.backup.BackupFailure
import ir.taqvim.feature.backup.BackupLanguageSource
import ir.taqvim.feature.backup.BackupOpenResult
import ir.taqvim.feature.backup.BackupOperations
import ir.taqvim.feature.backup.BackupRestoreResult
import ir.taqvim.feature.backup.BackupSummary
import ir.taqvim.feature.backup.BackupTableKind
import ir.taqvim.feature.backup.OpenedBackup
import ir.taqvim.feature.backup.PermissionKind
import ir.taqvim.feature.backup.PermissionStatus
import ir.taqvim.feature.backup.PermissionStatusSource
import ir.taqvim.feature.backup.PrivacyDataSource
import ir.taqvim.feature.backup.StoredData
import ir.taqvim.feature.backup.StoredDataKind
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Whole documents picked through the Storage Access Framework. */
internal interface DocumentBytes {
    /** The bytes at [uri], or `null` when it cannot be read or holds more than [limit] bytes. */
    suspend fun read(
        uri: String,
        limit: Int,
    ): ByteArray?

    /** Replaces the content at [uri] with [bytes]; returns whether it was written. */
    suspend fun write(
        uri: String,
        bytes: ByteArray,
    ): Boolean
}

/** [DocumentBytes] over the [resolver], off the main thread. */
internal class ContentResolverDocumentBytes(
    private val resolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DocumentBytes {
    override suspend fun read(
        uri: String,
        limit: Int,
    ): ByteArray? =
        withContext(dispatcher) {
            runCatching { resolver.openInputStream(uri.toUri())?.use { readBounded(it, limit) } }.getOrNull()
        }

    override suspend fun write(
        uri: String,
        bytes: ByteArray,
    ): Boolean =
        withContext(dispatcher) {
            runCatching { resolver.openOutputStream(uri.toUri(), "wt")?.use { it.write(bytes) } != null }
                .getOrDefault(false)
        }
}

private const val READ_BUFFER_BYTES = 64 * 1024

/** All bytes of [input], or `null` when it holds more than [limit] bytes. */
internal fun readBounded(
    input: InputStream,
    limit: Int,
): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(READ_BUFFER_BYTES)
    var read = input.read(buffer)
    while (read >= 0) {
        if (out.size() + read > limit) return null
        out.write(buffer, 0, read)
        read = input.read(buffer)
    }
    return out.toByteArray()
}

/**
 * [BackupOperations] (T-1503) over the T-605 [BackupService]. Passphrases are handed straight to the service and never
 * kept. After a restore, every kind of alarm is recomputed from the restored data.
 */
internal class ServiceBackupOperations(
    private val service: BackupService,
    private val documents: DocumentBytes,
    private val events: SchedulerEvents,
    private val clock: Clock,
    private val appVersion: () -> String,
) : BackupOperations {
    override suspend fun export(
        destinationUri: String,
        passphrase: CharArray?,
    ): BackupExportResult {
        val protection = passphrase?.let(BackupProtection::Passphrase) ?: BackupProtection.None
        val bytes = service.export(BackupMetadata(appVersion(), clock.now().toEpochMilliseconds()), protection)
        return if (documents.write(destinationUri, bytes)) {
            BackupExportResult.Exported(bytes.size.toLong())
        } else {
            BackupExportResult.Failed(BackupFailure.FILE_UNWRITABLE)
        }
    }

    override suspend fun open(
        sourceUri: String,
        passphrase: CharArray?,
    ): BackupOpenResult {
        val bytes =
            documents.read(sourceUri, MAX_BACKUP_BYTES) ?: return BackupOpenResult.Failed(BackupFailure.FILE_UNREADABLE)
        return when (val result = service.read(bytes, passphrase)) {
            is BackupReadResult.Ready -> BackupOpenResult.Ready(ServiceOpenedBackup(result.backup))
            is BackupReadResult.Failed -> BackupOpenResult.Failed(result.error.toFailure())
        }
    }

    override suspend fun restore(backup: OpenedBackup): BackupRestoreResult {
        if (backup !is ServiceOpenedBackup) return BackupRestoreResult.Failed(BackupFailure.RESTORE_FAILED)
        return when (val result = service.restore(backup.restorable)) {
            is RestoreResult.Restored -> {
                events.handle(RescheduleEvent.AlarmInputsChanged(AlarmKind.entries.toSet()))
                BackupRestoreResult.Restored(result.rowCounts.toTableKinds())
            }

            is RestoreResult.Failed -> {
                BackupRestoreResult.Failed(result.error.toFailure())
            }
        }
    }

    companion object {
        /** Larger files are not read: no backup of personal data comes near this size. */
        const val MAX_BACKUP_BYTES: Int = 64 * 1024 * 1024
    }
}

/** A backup read by the [BackupService], ready for [ServiceBackupOperations.restore]. */
internal class ServiceOpenedBackup(
    val restorable: RestorableBackup,
) : OpenedBackup {
    override val summary: BackupSummary = restorable.preview.toSummary()
}

internal fun BackupPreview.toSummary(): BackupSummary =
    BackupSummary(
        formatVersion = formatVersion,
        appVersion = appVersion,
        createdAtEpochMillis = createdAtEpochMillis,
        encrypted = encrypted,
        rowCounts = rowCounts.toTableKinds(),
        languageCode = preferences.languageCode,
        placeName = preferences.place?.name,
    )

/** The counts of the tables the backup screen knows; tables it does not list (official reminders) are left out. */
internal fun Map<BackupTable, Int>.toTableKinds(): Map<BackupTableKind, Int> =
    entries
        .mapNotNull { (table, count) -> BackupTableKind.entries.firstOrNull { it.name == table.name }?.to(count) }
        .toMap()

internal fun BackupError.toFailure(): BackupFailure =
    when (this) {
        BackupError.NotABackup -> BackupFailure.NOT_A_BACKUP
        BackupError.Truncated -> BackupFailure.TRUNCATED
        is BackupError.UnsupportedFormat -> BackupFailure.UNSUPPORTED_FORMAT
        BackupError.PassphraseRequired -> BackupFailure.PASSPHRASE_REQUIRED
        BackupError.WrongPassphrase -> BackupFailure.WRONG_PASSPHRASE
        BackupError.Corrupted -> BackupFailure.CORRUPTED
        is BackupError.InvalidContent -> BackupFailure.INVALID_CONTENT
        is BackupError.RestoreFailed -> BackupFailure.RESTORE_FAILED
    }

/** The installed version name, or "unknown" when the package manager does not report it. */
internal fun Context.appVersionName(): String =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    }.getOrNull() ?: "unknown"

/** [BackupLanguageSource] (T-1503) from the stored app language. */
internal class PreferencesBackupLanguageSource(
    private val preferences: UserPreferencesRepository,
) : BackupLanguageSource {
    override fun language(): Flow<LanguageSpec> =
        preferences.preferences.map { it.languageSpec() }.distinctUntilChanged()
}

/** [PrivacyDataSource] (T-1503) over the database row counts and the stored place and search history. */
internal class RoomPrivacyDataSource(
    private val database: TaqvimDatabase,
    private val preferences: UserPreferencesRepository,
) : PrivacyDataSource {
    override fun storedData(): Flow<List<StoredData>> =
        combine(database.privacyDao().observeCounts(), preferences.preferences) { rows, current ->
            storedData(rows, hasPlace = current.place != null, recentSearches = current.app.recentSearches.size)
        }

    override suspend fun clear(kind: StoredDataKind): Boolean {
        if (!kind.clearable) return false
        return runCatching {
            when (kind) {
                StoredDataKind.LOCATION -> {
                    preferences.update { it.copy(place = null) }
                }

                StoredDataKind.DEVICE_CALENDAR_CACHE -> {
                    database.privacyDao().clearDeviceEvents()
                }

                StoredDataKind.DIAGNOSTICS -> {
                    database.diagnosticsDao().clear()
                }

                else -> {
                    preferences.update { it.copy(app = it.app.copy(recentSearches = emptyList())) }
                }
            }
        }.onFailure { if (it is CancellationException) throw it }.isSuccess
    }
}

/** The dashboard rows for [rows], a chosen place ([hasPlace]) and [recentSearches] stored searches. */
internal fun storedData(
    rows: StoredRowCounts,
    hasPlace: Boolean,
    recentSearches: Int,
): List<StoredData> =
    listOf(
        StoredData(StoredDataKind.PERSONAL_EVENTS, rows.personalEvents),
        StoredData(StoredDataKind.REMINDERS, rows.reminders),
        StoredData(StoredDataKind.SUBSCRIPTIONS, rows.subscriptions),
        StoredData(StoredDataKind.LOCATION, if (hasPlace) 1 else 0),
        StoredData(StoredDataKind.DEVICE_CALENDAR_CACHE, rows.deviceEvents),
        StoredData(StoredDataKind.DIAGNOSTICS, rows.diagnostics),
        StoredData(StoredDataKind.RECENT_SEARCHES, recentSearches),
    )

/** [PermissionStatusSource] (T-1503) over the platform permission checks; [refresh] reads them again. */
internal class PlatformPermissionStatusSource(
    private val context: Context,
) : PermissionStatusSource {
    private val state = MutableStateFlow(read())

    override fun statuses(): Flow<List<PermissionStatus>> = state

    override fun refresh() {
        state.value = read()
    }

    private fun read(): List<PermissionStatus> = PermissionKind.entries.map { PermissionStatus(it, granted(it)) }

    private fun granted(kind: PermissionKind): Boolean =
        when (kind) {
            PermissionKind.LOCATION -> {
                has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            PermissionKind.CALENDAR -> {
                has(Manifest.permission.READ_CALENDAR)
            }

            PermissionKind.NOTIFICATIONS -> {
                notificationsAllowed()
            }

            PermissionKind.EXACT_ALARMS -> {
                exactAlarmsAllowed()
            }

            PermissionKind.DO_NOT_DISTURB -> {
                context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true
            }
        }

    private fun notificationsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            has(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    private fun exactAlarmsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/** Backup and restore (T-605, T-1503) and the privacy dashboard over the database, preferences and platform. */
val backupPortsModule =
    module {
        single<DocumentBytes> { ContentResolverDocumentBytes(get()) }
        single<BackupOperations> {
            val context = androidContext()
            ServiceBackupOperations(get(), get(), get(), get()) { context.appVersionName() }
        }
        single<BackupLanguageSource> { PreferencesBackupLanguageSource(get()) }
        single<PrivacyDataSource> { RoomPrivacyDataSource(get(), get()) }
        single<PermissionStatusSource> { PlatformPermissionStatusSource(androidContext()) }
    }
