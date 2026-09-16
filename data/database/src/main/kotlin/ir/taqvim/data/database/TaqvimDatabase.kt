/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** The personal-data database (docs/PLAN.md §4.3, T-601). Schemas are exported to `data/database/schemas`. */
@Database(
    entities = [
        PersonalEventEntity::class,
        EventRecurrenceEntity::class,
        ReminderEntity::class,
        ScheduledAlarmEntity::class,
        ShiftRotationEntity::class,
        ShiftRotationRecordEntity::class,
        IcsSubscriptionEntity::class,
        IcsEventCacheEntity::class,
        DeviceEventCacheEntity::class,
        WorkdayProfileEntity::class,
        DiagnosticsLogEntity::class,
        OfficialReminderEntity::class,
        EventExceptionEntity::class,
        EventOverrideEntity::class,
    ],
    version = TaqvimMigrations.LATEST_VERSION,
    exportSchema = true,
)
@TypeConverters(CollectionConverters::class, CalendarConverters::class)
abstract class TaqvimDatabase : RoomDatabase() {
    abstract fun personalEventDao(): PersonalEventDao

    abstract fun reminderDao(): ReminderDao

    abstract fun officialReminderDao(): OfficialReminderDao

    abstract fun shiftRotationDao(): ShiftRotationDao

    abstract fun workdayProfileDao(): WorkdayProfileDao

    abstract fun icsSubscriptionDao(): IcsSubscriptionDao

    abstract fun deviceEventDao(): DeviceEventDao

    abstract fun diagnosticsDao(): DiagnosticsDao

    abstract fun backupDao(): BackupDao

    abstract fun privacyDao(): PrivacyDao

    companion object {
        /** Database file name. */
        const val NAME: String = "taqvim.db"

        /** The app database with every migration; create one per process. */
        fun build(context: Context): TaqvimDatabase =
            TaqvimMigrations.ALL
                .fold(Room.databaseBuilder(context, TaqvimDatabase::class.java, NAME)) { builder, migration ->
                    builder.addMigrations(migration)
                }.build()
    }
}

/**
 * Schema migrations. Every schema change bumps [LATEST_VERSION], commits the exported schema JSON and adds a
 * [Migration] from the previous version to [ALL]; `TaqvimDatabaseMigrationTest` migrates every exported version to the
 * latest.
 */
object TaqvimMigrations {
    /** Current schema version. */
    const val LATEST_VERSION: Int = 6

    /** 1 → 2 (T-1003): iCalendar UIDs of personal events; HTTP validators and check time of subscriptions. */
    private val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `personal_events` ADD COLUMN `ics_uid` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_personal_events_ics_uid` " +
                        "ON `personal_events` (`ics_uid`)",
                )
                db.execSQL("ALTER TABLE `ics_subscriptions` ADD COLUMN `last_modified` TEXT")
                db.execSQL("ALTER TABLE `ics_subscriptions` ADD COLUMN `last_checked_at_epoch_millis` INTEGER")
            }
        }

    /** 2 → 3 (T-1000): the source link of personal events. */
    private val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `personal_events` ADD COLUMN `source_link` TEXT")
            }
        }

    /** 3 → 4 (T-1002): reminders before official events. */
    private val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `official_reminders` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event_id` TEXT NOT NULL, " +
                        "`days_before` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_official_reminders_event_id_days_before` " +
                        "ON `official_reminders` (`event_id`, `days_before`)",
                )
            }
        }

    /** 4 → 5 (T-1003): exception days and overridden occurrences of recurring personal events. */
    private val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `event_exceptions` (`event_id` INTEGER NOT NULL, " +
                        "`day_jdn` INTEGER NOT NULL, PRIMARY KEY(`event_id`, `day_jdn`), FOREIGN KEY(`event_id`) " +
                        "REFERENCES `personal_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `event_overrides` (`event_id` INTEGER NOT NULL, " +
                        "`original_jdn` INTEGER NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                        "`start_jdn` INTEGER NOT NULL, `start_minute` INTEGER, `end_jdn` INTEGER NOT NULL, " +
                        "`end_minute` INTEGER, `color_argb` INTEGER, `cancelled` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`event_id`, `original_jdn`), FOREIGN KEY(`event_id`) " +
                        "REFERENCES `personal_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
            }
        }

    /** 5 → 6 (T-604, ADR-0033): delivery attempts, retry time and snoozed instant of scheduled alarms. */
    private val MIGRATION_5_6: Migration =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scheduled_alarms` ADD COLUMN `attempts` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scheduled_alarms` ADD COLUMN `retry_at_epoch_millis` INTEGER")
                db.execSQL("ALTER TABLE `scheduled_alarms` ADD COLUMN `snoozed_from_epoch_millis` INTEGER")
            }
        }

    /** Migrations between consecutive versions, oldest first. */
    val ALL: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
