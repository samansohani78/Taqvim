/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T-601: every exported schema (1…latest) migrates to the latest schema and validates against the entities. */
@RunWith(AndroidJUnit4::class)
class TaqvimDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TaqvimDatabase::class.java)

    @Test
    fun everySchemaMigratesToLatest(): Unit =
        (1..TaqvimMigrations.LATEST_VERSION).forEach { version ->
            val name = "migration-from-$version.db"
            helper.createDatabase(name, version).close()
            helper
                .runMigrationsAndValidate(
                    name,
                    TaqvimMigrations.LATEST_VERSION,
                    true,
                    *TaqvimMigrations.ALL.toTypedArray(),
                ).close()
        }

    @Test
    fun eventsSurviveMigrationFromFourAndGainExceptionsAndOverrides() {
        val name = "migration-4-5.db"
        helper.createDatabase(name, 4).use { db ->
            db.execSQL(
                "INSERT INTO personal_events (id, title, notes, calendar_system, start_jdn, end_jdn, time_zone_id, " +
                    "created_at_epoch_millis, updated_at_epoch_millis) " +
                    "VALUES (1, 't', '', 'PERSIAN', 10, 10, 'UTC', 0, 0)",
            )
        }
        helper.runMigrationsAndValidate(name, 5, true, *TaqvimMigrations.ALL.toTypedArray()).use { db ->
            db.execSQL("INSERT INTO event_exceptions (event_id, day_jdn) VALUES (1, 17)")
            db.execSQL(
                "INSERT INTO event_overrides (event_id, original_jdn, title, notes, start_jdn, end_jdn, cancelled) " +
                    "VALUES (1, 24, 'moved', '', 25, 25, 0)",
            )
            db.query("SELECT COUNT(*) FROM personal_events").use { it.moveToFirst() } shouldBe true
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM personal_events WHERE id = 1")
            db.query("SELECT (SELECT COUNT(*) FROM event_exceptions) + (SELECT COUNT(*) FROM event_overrides)").use {
                it.moveToFirst()
                it.getInt(0)
            } shouldBe 0
        }
    }

    @Test
    fun scheduledAlarmsSurviveMigrationFromFiveAsPendingWithoutRetry() {
        val name = "migration-5-6.db"
        helper.createDatabase(name, 5).use { db ->
            db.execSQL(
                "INSERT INTO scheduled_alarms (id, kind, source_id, trigger_at_epoch_millis) " +
                    "VALUES (7, 'REMINDER', 3, 99)",
            )
        }
        helper.runMigrationsAndValidate(name, 6, true, *TaqvimMigrations.ALL.toTypedArray()).use { db ->
            db
                .query(
                    "SELECT kind, source_id, trigger_at_epoch_millis, attempts, retry_at_epoch_millis, " +
                        "snoozed_from_epoch_millis FROM scheduled_alarms WHERE id = 7",
                ).use {
                    it.moveToFirst()
                    listOf(it.getString(0), it.getLong(1), it.getLong(2), it.getInt(3), it.isNull(4), it.isNull(5))
                } shouldBe listOf("REMINDER", 3L, 99L, 0, true, true)
        }
    }

    @Test
    fun subscriptionsSurviveMigrationFromSixWithoutFailureOrProblems() {
        val name = "migration-6-7.db"
        helper.createDatabase(name, 6).use { db ->
            db.execSQL(
                "INSERT INTO ics_subscriptions (id, url, display_name, enabled, refresh_interval_minutes, " +
                    "last_fetched_at_epoch_millis) VALUES (4, 'https://example.org/a.ics', 'A', 1, 60, 1234)",
            )
        }
        helper.runMigrationsAndValidate(name, 7, true, *TaqvimMigrations.ALL.toTypedArray()).use { db ->
            db
                .query(
                    "SELECT url, last_fetched_at_epoch_millis, last_error, last_error_at_epoch_millis, problem_count " +
                        "FROM ics_subscriptions WHERE id = 4",
                ).use {
                    it.moveToFirst()
                    listOf(it.getString(0), it.getLong(1), it.isNull(2), it.isNull(3), it.getInt(4))
                } shouldBe listOf("https://example.org/a.ics", 1234L, true, true, 0)
        }
    }

    @Test
    fun appDatabaseOpensTheOldestSchema() {
        helper.createDatabase(TaqvimDatabase.NAME, 1).close()
        val db = TaqvimDatabase.build(ApplicationProvider.getApplicationContext<Context>())

        runBlocking { db.diagnosticsDao().count() } shouldBe 0
        db.close()
    }
}
