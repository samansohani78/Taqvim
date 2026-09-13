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
    ],
    version = TaqvimMigrations.LATEST_VERSION,
    exportSchema = true,
)
@TypeConverters(CollectionConverters::class, CalendarConverters::class)
abstract class TaqvimDatabase : RoomDatabase() {
    abstract fun personalEventDao(): PersonalEventDao

    abstract fun reminderDao(): ReminderDao

    abstract fun shiftRotationDao(): ShiftRotationDao

    abstract fun workdayProfileDao(): WorkdayProfileDao

    abstract fun icsSubscriptionDao(): IcsSubscriptionDao

    abstract fun deviceEventDao(): DeviceEventDao

    abstract fun diagnosticsDao(): DiagnosticsDao

    abstract fun backupDao(): BackupDao

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
    const val LATEST_VERSION: Int = 2

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

    /** Migrations between consecutive versions, oldest first. */
    val ALL: List<Migration> = listOf(MIGRATION_1_2)
}
