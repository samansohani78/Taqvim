/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.database.backup.BackupFixtures.data
import ir.taqvim.data.database.backup.BackupFixtures.metadata
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.UserPrefsSerializer
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-605 (R): export and restore against Room and the preferences DataStore on a Robolectric device. */
@RunWith(AndroidJUnit4::class)
class BackupServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val job = Job()
    private val prefsFile = File(context.filesDir, "backup-test-${UUID.randomUUID()}.pb")
    private val preferences =
        UserPreferencesRepository(
            DataStoreFactory.create(
                serializer = UserPrefsSerializer,
                scope = CoroutineScope(Dispatchers.IO + job),
                produceFile = { prefsFile },
            ),
        )
    private val service = BackupService(db, preferences, BackupCodec(SecureRandom(), iterations = 1_000))

    @After
    fun close() {
        runBlocking { job.cancelAndJoin() }
        db.close()
        prefsFile.delete()
    }

    private suspend fun seed() {
        db.backupDao().replaceAll(data)
        preferences.update { BackupFixtures.preferences }
    }

    @Test
    fun exportThenRestoreBringsBackEveryTableAndThePreferences(): Unit =
        runBlocking {
            seed()
            val file = service.export(metadata, BackupProtection.Passphrase("secret".toCharArray()))

            db.personalEventDao().delete(3)
            db.workdayProfileDao().insert(
                WorkdayProfileEntity(
                    name = "Other",
                    weekend = emptySet(),
                    holidaySources = emptySet(),
                    halfDays = HalfDayPolicy.HALF,
                    personalLeave = emptyList(),
                ),
            )
            preferences.update { UserPreferences.defaultsFor("en") }

            val backup = service.read(file, "secret".toCharArray()).shouldBeInstanceOf<BackupReadResult.Ready>().backup
            backup.preview.rowCounts.getValue(BackupTable.PERSONAL_EVENTS) shouldBe 2

            service.restore(backup) shouldBe RestoreResult.Restored(backup.preview.rowCounts)
            db.backupDao().snapshot() shouldBe data
            db.personalEventDao().getRecurrence(3)?.byMonthDay shouldBe listOf(1, -1)
            preferences.preferences.first() shouldBe BackupFixtures.preferences
        }

    @Test
    fun plaintextExportIsReadableWithoutAPassphrase(): Unit =
        runBlocking {
            seed()
            val file = service.export(metadata, BackupProtection.None)

            val preview =
                service
                    .read(file)
                    .shouldBeInstanceOf<BackupReadResult.Ready>()
                    .backup.preview

            preview.encrypted shouldBe false
            preview.appVersion shouldBe "0.1.0"
            preview.preferences shouldBe BackupFixtures.preferences
            service.read(file.copyOf(10)).shouldBeInstanceOf<BackupReadResult.Failed>()
        }

    @Test
    fun aFailingRestoreRollsBackTheWholeTransaction(): Unit =
        runBlocking {
            seed()
            val valid =
                service
                    .read(service.export(metadata, BackupProtection.None))
                    .shouldBeInstanceOf<BackupReadResult.Ready>()
                    .backup
            // Bypasses the reader's validation: the reminder's event does not exist, so the foreign key fails mid-way.
            val broken =
                RestorableBackup(
                    valid.preview,
                    PersonalData(events = data.events, reminders = listOf(ReminderEntity(1, 404, 10))),
                    UserPreferences.defaultsFor("en"),
                )

            service
                .restore(
                    broken,
                ).shouldBeInstanceOf<RestoreResult.Failed>()
                .error
                .shouldBeInstanceOf<BackupError.RestoreFailed>()
            db.backupDao().snapshot() shouldBe data
            preferences.preferences.first() shouldBe BackupFixtures.preferences
        }
}
