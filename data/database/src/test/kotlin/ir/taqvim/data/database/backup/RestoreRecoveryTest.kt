/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.backup.BackupFixtures.data
import ir.taqvim.data.database.backup.BackupFixtures.metadata
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.proto.UserPrefs
import ir.taqvim.data.preferences.toProto
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B09 (R): a restore spans Room and the preferences, so failures and process deaths between the two stores are
 * finished or undone from the restore journal. A "process recreation" is a new [BackupService] over the same stores
 * and journal directory.
 */
@RunWith(AndroidJUnit4::class)
class RestoreRecoveryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val original = UserPreferences.defaultsFor("en")
    private val store = FlakyPreferences(original.toProto())
    private val preferences = UserPreferencesRepository(store)
    private val codec = BackupCodec(SecureRandom(), iterations = 1_000)
    private val journalDirectory = File(context.cacheDir, "restore-journal-${UUID.randomUUID()}")

    @After
    fun close() {
        db.close()
        journalDirectory.deleteRecursively()
    }

    private fun service() = BackupService(db, preferences, journalDirectory, codec)

    /** A backup of the fixture data and preferences, with the stores then reset to empty data and [original]. */
    private suspend fun backup(): RestorableBackup {
        db.backupDao().replaceAll(data)
        preferences.update { BackupFixtures.preferences }
        val file = service().export(metadata, BackupProtection.None)
        db.backupDao().replaceAll(PersonalData())
        preferences.update { original }
        return service().read(file).shouldBeInstanceOf<BackupReadResult.Ready>().backup
    }

    private fun RestoreResult.failure(): BackupError = shouldBeInstanceOf<RestoreResult.Failed>().error

    private suspend fun shouldHoldOriginal() {
        db.backupDao().snapshot() shouldBe PersonalData()
        preferences.preferences.first() shouldBe original
    }

    private suspend fun shouldHoldBackup() {
        db.backupDao().snapshot() shouldBe data
        preferences.preferences.first() shouldBe BackupFixtures.preferences
    }

    @Test
    fun aFailingPreferenceWriteAfterTheDatabasePutsTheOldDataBack(): Unit =
        runBlocking {
            val backup = backup()
            store.failNextUpdates(1)

            service().restore(backup).failure().shouldBeInstanceOf<BackupError.RestoreFailed>()

            shouldHoldOriginal()
            service().recover() shouldBe RecoveryResult.NothingPending
        }

    @Test
    fun aRestoreThatCannotBeUndoneStaysPendingUntilTheNextStart(): Unit =
        runBlocking {
            val backup = backup()
            store.failNextUpdates(Int.MAX_VALUE)
            val first = service()

            first.restore(backup).failure().shouldBeInstanceOf<BackupError.RestorePending>()
            first.restore(backup).failure().shouldBeInstanceOf<BackupError.RestorePending>()
            first.recover() shouldBe RecoveryResult.StillPending

            store.failNextUpdates(0)
            val restarted = service()
            restarted.recover() shouldBe RecoveryResult.RolledBack
            shouldHoldOriginal()
            restarted.recover() shouldBe RecoveryResult.NothingPending
            restarted.restore(backup) shouldBe RestoreResult.Restored(backup.preview.rowCounts)
            shouldHoldBackup()
        }

    @Test
    fun aProcessStoppedBeforeAnythingWasAppliedFinishesTheRestore(): Unit =
        runBlocking {
            val backup = backup()
            RestoreJournal(journalDirectory, codec).begin(
                RestoreContents(backup.data, backup.preferences),
                RestoreContents(PersonalData(), original),
            )

            service().recover() shouldBe RecoveryResult.Completed(backup.preview.rowCounts)

            shouldHoldBackup()
        }

    @Test
    fun aProcessStoppedBetweenTheTwoStoresFinishesTheRestore(): Unit =
        runBlocking {
            val backup = backup()
            RestoreJournal(journalDirectory, codec).begin(
                RestoreContents(backup.data, backup.preferences),
                RestoreContents(PersonalData(), original),
            )
            db.backupDao().replaceAll(backup.data)

            service().recover() shouldBe RecoveryResult.Completed(backup.preview.rowCounts)

            shouldHoldBackup()
        }

    @Test
    fun aProcessStoppedWhileUndoingFinishesTheUndo(): Unit =
        runBlocking {
            val backup = backup()
            val journal = RestoreJournal(journalDirectory, codec)
            journal.begin(RestoreContents(backup.data, backup.preferences), RestoreContents(PersonalData(), original))
            db.backupDao().replaceAll(backup.data)
            journal.rollBack()

            service().recover() shouldBe RecoveryResult.RolledBack

            shouldHoldOriginal()
        }

    @Test
    fun anIncompleteOrUnreadableJournalIsIgnored(): Unit =
        runBlocking {
            val backup = backup()
            val journal = RestoreJournal(journalDirectory, codec)
            journal.begin(RestoreContents(backup.data, backup.preferences), RestoreContents(PersonalData(), original))
            File(journalDirectory, "direction").delete()

            service().recover() shouldBe RecoveryResult.NothingPending

            journal.begin(RestoreContents(backup.data, backup.preferences), RestoreContents(PersonalData(), original))
            File(journalDirectory, "target.bin").writeBytes(byteArrayOf(1, 2, 3))
            service().recover() shouldBe RecoveryResult.NothingPending
            shouldHoldOriginal()
        }

    @Test
    fun aJournalThatCannotBeWrittenLeavesEverythingUnchanged(): Unit =
        runBlocking {
            val backup = backup()
            journalDirectory.parentFile?.mkdirs()
            journalDirectory.writeText("not a directory")

            service().restore(backup).failure().shouldBeInstanceOf<BackupError.RestoreFailed>()

            shouldHoldOriginal()
            journalDirectory.delete()
        }
}

/** Preferences in memory whose next updates can be made to fail, as a full disk or I/O error would. */
private class FlakyPreferences(
    initial: UserPrefs,
) : DataStore<UserPrefs> {
    private val state = MutableStateFlow(initial)
    private var failures = 0

    override val data: Flow<UserPrefs> = state

    fun failNextUpdates(count: Int) {
        failures = count
    }

    override suspend fun updateData(transform: suspend (t: UserPrefs) -> UserPrefs): UserPrefs {
        if (failures > 0) {
            failures--
            throw IOException("preferences are not writable")
        }
        return transform(state.value).also { state.value = it }
    }
}
