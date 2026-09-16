/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
     * [toJdn] and events with exceptions or overridden occurrences (T-1003), whose occurrences the caller expands. The
     * exception tables are named so that changing them emits again.
     */
    @Query(
        """
        SELECT * FROM personal_events
        WHERE (start_jdn <= :toJdn AND (end_jdn >= :fromJdn OR id IN (SELECT event_id FROM event_recurrences)))
            OR id IN (SELECT event_id FROM event_overrides)
            OR id IN (SELECT event_id FROM event_exceptions)
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

    /** Days on which event [eventId] does not occur (T-1003), ascending. */
    @Query("SELECT day_jdn FROM event_exceptions WHERE event_id = :eventId ORDER BY day_jdn")
    suspend fun exceptionDays(eventId: Long): List<Long>

    /** Adds exception days; days already stored are kept. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExceptions(exceptions: List<EventExceptionEntity>)

    @Query("DELETE FROM event_exceptions WHERE event_id = :eventId")
    suspend fun deleteExceptions(eventId: Long)

    /** Overridden occurrences of event [eventId] (T-1003), by original day. */
    @Query("SELECT * FROM event_overrides WHERE event_id = :eventId ORDER BY original_jdn")
    suspend fun overrides(eventId: Long): List<EventOverrideEntity>

    @Upsert
    suspend fun upsertOverrides(overrides: List<EventOverrideEntity>)

    @Query("DELETE FROM event_overrides WHERE event_id = :eventId")
    suspend fun deleteOverrides(eventId: Long)
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

    /** Records a failed delivery of alarm [id], which fires again at [retryAtEpochMillis] (ADR-0033). */
    @Query(
        "UPDATE scheduled_alarms SET attempts = :attempts, retry_at_epoch_millis = :retryAtEpochMillis WHERE id = :id",
    )
    suspend fun updateAttempts(
        id: Long,
        attempts: Int,
        retryAtEpochMillis: Long,
    )

    /** Deletes every alarm of [kind], e.g. before prayer alarms are rescheduled. */
    @Query("DELETE FROM scheduled_alarms WHERE kind = :kind")
    suspend fun deleteAlarms(kind: AlarmKind)
}

/** Reminders before official events (T-1002). */
@Dao
interface OfficialReminderDao {
    /** Adds a reminder, replacing the one with the same event and lead time. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: OfficialReminderEntity): Long

    @Query("DELETE FROM official_reminders WHERE event_id = :eventId AND days_before = :daysBefore")
    suspend fun delete(
        eventId: String,
        daysBefore: Int,
    )

    @Query("SELECT * FROM official_reminders ORDER BY event_id, days_before")
    suspend fun all(): List<OfficialReminderEntity>

    @Query("SELECT * FROM official_reminders ORDER BY event_id, days_before")
    fun observeAll(): Flow<List<OfficialReminderEntity>>
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
