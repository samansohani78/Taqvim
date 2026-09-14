/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.DiagnosticsLevel
import ir.taqvim.data.database.DiagnosticsLogEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.StoredRowCounts
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.backup.BackupError
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.RescheduleEvent
import ir.taqvim.data.scheduler.SchedulerEvents
import ir.taqvim.feature.backup.BackupExportResult
import ir.taqvim.feature.backup.BackupFailure
import ir.taqvim.feature.backup.BackupOpenResult
import ir.taqvim.feature.backup.BackupRestoreResult
import ir.taqvim.feature.backup.BackupSummary
import ir.taqvim.feature.backup.BackupTableKind
import ir.taqvim.feature.backup.OpenedBackup
import ir.taqvim.feature.backup.PermissionKind
import ir.taqvim.feature.backup.StoredData
import ir.taqvim.feature.backup.StoredDataKind
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/** T-1503 wiring: backup files through the T-605 service, the privacy dashboard's data and the permission checks. */
@RunWith(AndroidJUnit4::class)
class BackupAdaptersTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val tehran = ChosenPlace(PlaceSource.COORDINATES, null, "خانه", Coordinates(35.7, 51.4), "Asia/Tehran")
    private val preferences = repositoryOf(UserPreferences.defaultsFor("fa").copy(place = tehran))
    private val documents = MemoryDocuments()
    private val handled = mutableListOf<RescheduleEvent>()
    private val events =
        object : SchedulerEvents {
            override suspend fun handle(event: RescheduleEvent) {
                handled += event
            }

            override suspend fun onAlarmFired(id: Long) = Unit
        }
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-14T08:00:00Z")
        }
    private val operations =
        ServiceBackupOperations(BackupService(database, preferences), documents, events, clock) { "1.2.3" }

    @After
    fun closeDatabase() {
        database.close()
        // Robolectric starts TaqvimApplication, and so Koin, for every test.
        stopKoin()
    }

    @Test
    fun encryptedBackupsExportPreviewAndRestoreTheData(): Unit =
        runBlocking {
            val eventId = database.personalEventDao().insert(event("تولد"))

            val exported = operations.export(FILE, "رمز".toCharArray())
            exported shouldBe BackupExportResult.Exported(requireNotNull(documents.files[FILE]).size.toLong())

            operations.open(FILE, null) shouldBe failed(BackupFailure.PASSPHRASE_REQUIRED)
            operations.open(FILE, "غلط".toCharArray()) shouldBe failed(BackupFailure.WRONG_PASSPHRASE)
            val opened =
                operations.open(FILE, "رمز".toCharArray()).shouldBeInstanceOf<BackupOpenResult.Ready>().backup
            opened.summary shouldBe
                BackupSummary(
                    formatVersion = 1,
                    appVersion = "1.2.3",
                    createdAtEpochMillis = clock.now().toEpochMilliseconds(),
                    encrypted = true,
                    rowCounts =
                        BackupTableKind.entries.associateWith { if (it == BackupTableKind.PERSONAL_EVENTS) 1 else 0 },
                    languageCode = "fa",
                    placeName = "خانه",
                )

            database.personalEventDao().delete(eventId)
            operations.restore(opened).shouldBeInstanceOf<BackupRestoreResult.Restored>()

            database.personalEventDao().all().map { it.title } shouldBe listOf("تولد")
            handled shouldBe listOf(RescheduleEvent.AlarmInputsChanged(AlarmKind.entries.toSet()))
        }

    @Test
    fun unreadableUnwritableAndForeignFilesAreTypedFailures(): Unit =
        runBlocking {
            documents.writable = false
            operations.export(FILE, null) shouldBe BackupExportResult.Failed(BackupFailure.FILE_UNWRITABLE)
            operations.open("content://missing", null) shouldBe BackupOpenResult.Failed(BackupFailure.FILE_UNREADABLE)

            documents.files[FILE] = "not a backup".encodeToByteArray()
            operations.open(FILE, null) shouldBe BackupOpenResult.Failed(BackupFailure.NOT_A_BACKUP)

            val foreign =
                object : OpenedBackup {
                    override val summary: BackupSummary =
                        BackupSummary(1, "0", 0, encrypted = false, emptyMap(), "fa", placeName = null)
                }
            operations.restore(foreign) shouldBe BackupRestoreResult.Failed(BackupFailure.RESTORE_FAILED)
            handled shouldBe emptyList()
        }

    @Test
    fun everyBackupErrorHasAFailure() {
        mapOf(
            BackupError.NotABackup to BackupFailure.NOT_A_BACKUP,
            BackupError.Truncated to BackupFailure.TRUNCATED,
            BackupError.UnsupportedFormat(9) to BackupFailure.UNSUPPORTED_FORMAT,
            BackupError.PassphraseRequired to BackupFailure.PASSPHRASE_REQUIRED,
            BackupError.WrongPassphrase to BackupFailure.WRONG_PASSPHRASE,
            BackupError.Corrupted to BackupFailure.CORRUPTED,
            BackupError.InvalidContent("x") to BackupFailure.INVALID_CONTENT,
            BackupError.RestoreFailed("x") to BackupFailure.RESTORE_FAILED,
        ).forEach { (error, failure) -> error.toFailure() shouldBe failure }
    }

    @Test
    fun documentsAreReadWithinTheLimitAndWrittenThroughTheContentResolver(): Unit =
        runBlocking {
            readBounded(ByteArrayInputStream(ByteArray(10)), limit = 10)?.size shouldBe 10
            readBounded(ByteArrayInputStream(ByteArray(11)), limit = 10).shouldBeNull()

            val file = File.createTempFile("backup", ".taqvim").apply { deleteOnExit() }
            val uri = Uri.fromFile(file).toString()
            val resolver = ContentResolverDocumentBytes(context.contentResolver)
            resolver.write(uri, byteArrayOf(1, 2, 3)) shouldBe true
            resolver.read(uri, limit = 3)?.toList() shouldBe listOf<Byte>(1, 2, 3)
            resolver.read(uri, limit = 2).shouldBeNull()
            resolver.read(Uri.fromFile(File(file.parentFile, "missing.taqvim")).toString(), limit = 3).shouldBeNull()
        }

    @Test
    fun privacyRowsFollowTheDataAndClearableKindsAreCleared(): Unit =
        runBlocking {
            database.personalEventDao().insert(event("a"))
            database.diagnosticsDao().insert(
                DiagnosticsLogEntity(atEpochMillis = 1, level = DiagnosticsLevel.WARN, tag = "t", message = "m"),
            )
            preferences.update { it.copy(app = it.app.copy(recentSearches = listOf("نوروز", "یلدا"))) }
            val privacy = RoomPrivacyDataSource(database, preferences)

            privacy.storedData().first() shouldBe
                storedData(StoredRowCounts(1, 0, 0, 0, 1), hasPlace = true, recentSearches = 2)

            StoredDataKind.entries.forEach { kind -> privacy.clear(kind) shouldBe kind.clearable }

            privacy.storedData().first() shouldBe
                listOf(
                    StoredData(StoredDataKind.PERSONAL_EVENTS, 1),
                    StoredData(StoredDataKind.REMINDERS, 0),
                    StoredData(StoredDataKind.SUBSCRIPTIONS, 0),
                    StoredData(StoredDataKind.LOCATION, 0),
                    StoredData(StoredDataKind.DEVICE_CALENDAR_CACHE, 0),
                    StoredData(StoredDataKind.DIAGNOSTICS, 0),
                    StoredData(StoredDataKind.RECENT_SEARCHES, 0),
                )
            PreferencesBackupLanguageSource(preferences).language().first().code shouldBe "fa"
        }

    @Test
    fun permissionStatusesAreReadAgainOnRefresh(): Unit =
        runBlocking {
            val source = PlatformPermissionStatusSource(context)
            val before = source.statuses().first().associate { it.kind to it.granted }
            before[PermissionKind.LOCATION] shouldBe false
            before[PermissionKind.CALENDAR] shouldBe false
            before.keys shouldBe PermissionKind.entries.toSet()

            shadowOf(context as Application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
            source
                .statuses()
                .first()
                .first { it.kind == PermissionKind.LOCATION }
                .granted shouldBe false
            source.refresh()

            source
                .statuses()
                .first()
                .first { it.kind == PermissionKind.LOCATION }
                .granted shouldBe true
            context.appVersionName().isNotBlank() shouldBe true
        }

    private fun failed(failure: BackupFailure): BackupOpenResult = BackupOpenResult.Failed(failure)

    private fun event(title: String) =
        PersonalEventEntity(
            title = title,
            calendarSystem = CalendarSystem.PERSIAN,
            startJdn = 2_461_120,
            endJdn = 2_461_120,
            timeZoneId = "Asia/Tehran",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )

    /** Documents in memory; writes fail while [writable] is false. */
    private class MemoryDocuments : DocumentBytes {
        val files = mutableMapOf<String, ByteArray>()
        var writable = true

        override suspend fun read(
            uri: String,
            limit: Int,
        ): ByteArray? = files[uri]?.takeIf { it.size <= limit }

        override suspend fun write(
            uri: String,
            bytes: ByteArray,
        ): Boolean = writable.also { if (it) files[uri] = bytes }
    }

    private companion object {
        const val FILE = "content://documents/backup.taqvim"
    }
}
