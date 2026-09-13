/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * The user's own rows that a backup carries (T-605). Caches (`ics_events_cache`, `device_events_cache`), the
 * diagnostics log and `scheduled_alarms` are derived or device-specific and are not part of it.
 */
data class PersonalData(
    val events: List<PersonalEventEntity> = emptyList(),
    val recurrences: List<EventRecurrenceEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val shiftRotations: List<ShiftRotationEntity> = emptyList(),
    val shiftRecords: List<ShiftRotationRecordEntity> = emptyList(),
    val icsSubscriptions: List<IcsSubscriptionEntity> = emptyList(),
    val workdayProfiles: List<WorkdayProfileEntity> = emptyList(),
)

/** Whole-table reads and the atomic replacement used by backup and restore (T-605). */
@Dao
abstract class BackupDao {
    /** Every personal row, read in one transaction so the copy is consistent. */
    @Transaction
    open suspend fun snapshot(): PersonalData =
        PersonalData(
            events = events(),
            recurrences = recurrences(),
            reminders = reminders(),
            shiftRotations = shiftRotations(),
            shiftRecords = shiftRecords(),
            icsSubscriptions = icsSubscriptions(),
            workdayProfiles = workdayProfiles(),
        )

    /**
     * Replaces every personal row with [data] (ids kept) in a single transaction: if any insert fails, for example on a
     * foreign key, the database is left exactly as it was.
     */
    @Transaction
    open suspend fun replaceAll(data: PersonalData) {
        // Deleting parents cascades to recurrences, reminders, shift records and cached ICS occurrences.
        deleteEvents()
        deleteShiftRotations()
        deleteIcsSubscriptions()
        deleteWorkdayProfiles()
        insertEvents(data.events)
        insertRecurrences(data.recurrences)
        insertReminders(data.reminders)
        insertShiftRotations(data.shiftRotations)
        insertShiftRecords(data.shiftRecords)
        insertIcsSubscriptions(data.icsSubscriptions)
        insertWorkdayProfiles(data.workdayProfiles)
    }

    @Query("SELECT * FROM personal_events ORDER BY id")
    protected abstract suspend fun events(): List<PersonalEventEntity>

    @Query("SELECT * FROM event_recurrences ORDER BY event_id")
    protected abstract suspend fun recurrences(): List<EventRecurrenceEntity>

    @Query("SELECT * FROM reminders ORDER BY id")
    protected abstract suspend fun reminders(): List<ReminderEntity>

    @Query("SELECT * FROM shift_rotations ORDER BY id")
    protected abstract suspend fun shiftRotations(): List<ShiftRotationEntity>

    @Query("SELECT * FROM shift_rotation_records ORDER BY rotation_id, jdn")
    protected abstract suspend fun shiftRecords(): List<ShiftRotationRecordEntity>

    @Query("SELECT * FROM ics_subscriptions ORDER BY id")
    protected abstract suspend fun icsSubscriptions(): List<IcsSubscriptionEntity>

    @Query("SELECT * FROM workday_profiles ORDER BY id")
    protected abstract suspend fun workdayProfiles(): List<WorkdayProfileEntity>

    @Query("DELETE FROM personal_events")
    protected abstract suspend fun deleteEvents()

    @Query("DELETE FROM shift_rotations")
    protected abstract suspend fun deleteShiftRotations()

    @Query("DELETE FROM ics_subscriptions")
    protected abstract suspend fun deleteIcsSubscriptions()

    @Query("DELETE FROM workday_profiles")
    protected abstract suspend fun deleteWorkdayProfiles()

    @Insert
    protected abstract suspend fun insertEvents(rows: List<PersonalEventEntity>)

    @Insert
    protected abstract suspend fun insertRecurrences(rows: List<EventRecurrenceEntity>)

    @Insert
    protected abstract suspend fun insertReminders(rows: List<ReminderEntity>)

    @Insert
    protected abstract suspend fun insertShiftRotations(rows: List<ShiftRotationEntity>)

    @Insert
    protected abstract suspend fun insertShiftRecords(rows: List<ShiftRotationRecordEntity>)

    @Insert
    protected abstract suspend fun insertIcsSubscriptions(rows: List<IcsSubscriptionEntity>)

    @Insert
    protected abstract suspend fun insertWorkdayProfiles(rows: List<WorkdayProfileEntity>)
}
