/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Personal events and their recurrence rules. */
@Dao
interface PersonalEventDao {
    @Insert
    suspend fun insert(event: PersonalEventEntity): Long

    @Update
    suspend fun update(event: PersonalEventEntity)

    /** Deletes the event with its recurrence and reminders. */
    @Query("DELETE FROM personal_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM personal_events WHERE id = :id")
    suspend fun get(id: Long): PersonalEventEntity?

    /** The event imported or exported with iCalendar UID [uid] (T-1003). */
    @Query("SELECT * FROM personal_events WHERE ics_uid = :uid")
    suspend fun getByIcsUid(uid: String): PersonalEventEntity?

    /** Every personal event, for export. */
    @Query("SELECT * FROM personal_events ORDER BY start_jdn, start_minute, id")
    suspend fun all(): List<PersonalEventEntity>

    /**
     * Events overlapping the days [fromJdn]..[toJdn] (inclusive), plus recurring events that start on or before
     * [toJdn], whose occurrences the caller expands.
     */
    @Query(
        """
        SELECT * FROM personal_events
        WHERE start_jdn <= :toJdn AND (end_jdn >= :fromJdn OR id IN (SELECT event_id FROM event_recurrences))
        ORDER BY start_jdn, start_minute, id
        """,
    )
    fun observeInRange(
        fromJdn: Long,
        toJdn: Long,
    ): Flow<List<PersonalEventEntity>>

    @Upsert
    suspend fun upsertRecurrence(recurrence: EventRecurrenceEntity)

    @Query("DELETE FROM event_recurrences WHERE event_id = :eventId")
    suspend fun deleteRecurrence(eventId: Long)

    @Query("SELECT * FROM event_recurrences WHERE event_id = :eventId")
    suspend fun getRecurrence(eventId: Long): EventRecurrenceEntity?
}

/** Event reminders and the alarms scheduled for reminders, prayers and shifts. */
@Dao
interface ReminderDao {
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("SELECT * FROM reminders WHERE event_id = :eventId ORDER BY minutes_before, id")
    fun observeReminders(eventId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE event_id = :eventId ORDER BY minutes_before, id")
    suspend fun reminders(eventId: Long): List<ReminderEntity>

    /** Deletes the reminders of [eventId], e.g. before an imported event's alarms are replaced. */
    @Query("DELETE FROM reminders WHERE event_id = :eventId")
    suspend fun deleteReminders(eventId: Long)

    @Insert
    suspend fun insertAlarm(alarm: ScheduledAlarmEntity): Long

    @Query("SELECT * FROM scheduled_alarms ORDER BY trigger_at_epoch_millis, id")
    suspend fun alarms(): List<ScheduledAlarmEntity>

    @Query("DELETE FROM scheduled_alarms WHERE id = :id")
    suspend fun deleteAlarm(id: Long)

    /** Deletes every alarm of [kind], e.g. before prayer alarms are rescheduled. */
    @Query("DELETE FROM scheduled_alarms WHERE kind = :kind")
    suspend fun deleteAlarms(kind: AlarmKind)
}

/** Shift rotations and their per-day records. */
@Dao
interface ShiftRotationDao {
    @Insert
    suspend fun insertRotation(rotation: ShiftRotationEntity): Long

    @Update
    suspend fun updateRotation(rotation: ShiftRotationEntity)

    @Query("DELETE FROM shift_rotations WHERE id = :id")
    suspend fun deleteRotation(id: Long)

    @Query("SELECT * FROM shift_rotations ORDER BY name, id")
    fun observeRotations(): Flow<List<ShiftRotationEntity>>

    @Upsert
    suspend fun upsertRecord(record: ShiftRotationRecordEntity)

    @Query("DELETE FROM shift_rotation_records WHERE rotation_id = :rotationId AND jdn = :jdn")
    suspend fun deleteRecord(
        rotationId: Long,
        jdn: Long,
    )

    @Query(
        """
        SELECT * FROM shift_rotation_records
        WHERE rotation_id = :rotationId AND jdn BETWEEN :fromJdn AND :toJdn
        ORDER BY jdn
        """,
    )
    fun observeRecords(
        rotationId: Long,
        fromJdn: Long,
        toJdn: Long,
    ): Flow<List<ShiftRotationRecordEntity>>
}

/** Named workday profiles. */
@Dao
interface WorkdayProfileDao {
    @Insert
    suspend fun insert(profile: WorkdayProfileEntity): Long

    @Update
    suspend fun update(profile: WorkdayProfileEntity)

    @Query("DELETE FROM workday_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    /** Profiles with the default first. */
    @Query("SELECT * FROM workday_profiles ORDER BY is_default DESC, name, id")
    fun observeAll(): Flow<List<WorkdayProfileEntity>>

    /** Makes profile [id] the only default. */
    @Query("UPDATE workday_profiles SET is_default = (id = :id)")
    suspend fun makeDefault(id: Long)
}
