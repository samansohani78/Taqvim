/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/**
 * A user-created event (`personal_events`, docs/PLAN.md §4.3). Days are Julian day numbers; times are minutes of the
 * day (`0..1439`) in [timeZoneId], and `null` times mark an all-day event. [icsUid] is the iCalendar UID of an imported
 * or exported event (T-1003), unique when present.
 */
@Entity(
    tableName = "personal_events",
    indices = [Index("start_jdn"), Index(value = ["ics_uid"], unique = true)],
)
data class PersonalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    /** Calendar the event was entered in; recurrence months and years count in it. */
    @ColumnInfo(name = "calendar_system") val calendarSystem: CalendarSystem,
    @ColumnInfo(name = "start_jdn") val startJdn: Long,
    @ColumnInfo(name = "start_minute") val startMinute: Int? = null,
    @ColumnInfo(name = "end_jdn") val endJdn: Long,
    @ColumnInfo(name = "end_minute") val endMinute: Int? = null,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Int? = null,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "ics_uid") val icsUid: String? = null,
    /** The web page the event was taken from (T-1000), if any. */
    @ColumnInfo(name = "source_link") val sourceLink: String? = null,
)

/** The RRULE-lite rule of one personal event (`event_recurrences`); see [RecurrenceRule] (T-503). */
@Entity(
    tableName = "event_recurrences",
    foreignKeys = [
        ForeignKey(
            entity = PersonalEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EventRecurrenceEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: Long,
    val frequency: Frequency,
    val interval: Int,
    val count: Int?,
    @ColumnInfo(name = "until_jdn") val untilJdn: Long?,
    @ColumnInfo(name = "by_day") val byDay: List<WeekdayNum>,
    @ColumnInfo(name = "by_month_day") val byMonthDay: List<Int>,
    @ColumnInfo(name = "calendar_system") val calendarSystem: CalendarSystem,
    @ColumnInfo(name = "invalid_dates") val invalidDates: InvalidDatePolicy,
    @ColumnInfo(name = "week_start") val weekStart: Weekday,
)

/** The stored rule as a [RecurrenceRule]. */
fun EventRecurrenceEntity.toRule(): RecurrenceRule =
    RecurrenceRule(
        frequency = frequency,
        interval = interval,
        count = count,
        until = untilJdn?.let(::Jdn),
        byDay = byDay,
        byMonthDay = byMonthDay,
        invalidDates = invalidDates,
        weekStart = weekStart,
    )

/** This rule as the recurrence of event [eventId], counted in [calendarSystem]. */
fun RecurrenceRule.toEntity(
    eventId: Long,
    calendarSystem: CalendarSystem,
): EventRecurrenceEntity =
    EventRecurrenceEntity(
        eventId = eventId,
        frequency = frequency,
        interval = interval,
        count = count,
        untilJdn = until?.value,
        byDay = byDay,
        byMonthDay = byMonthDay,
        calendarSystem = calendarSystem,
        invalidDates = invalidDates,
        weekStart = weekStart,
    )

/** A reminder [minutesBefore] each occurrence of an event (`reminders`). */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PersonalEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("event_id")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "minutes_before") val minutesBefore: Int,
    val enabled: Boolean = true,
)

/** What a scheduled alarm belongs to. */
enum class AlarmKind {
    REMINDER,
    PRAYER,
    SHIFT,
}

/**
 * An alarm registered with the system (`scheduled_alarms`), kept so alarms can be restored after reboot. [sourceId] is
 * the row id of the reminder or shift rotation, or `null` for prayer alarms; [id] doubles as the request code.
 */
@Entity(tableName = "scheduled_alarms", indices = [Index("trigger_at_epoch_millis")])
data class ScheduledAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: AlarmKind,
    @ColumnInfo(name = "source_id") val sourceId: Long?,
    @ColumnInfo(name = "trigger_at_epoch_millis") val triggerAtEpochMillis: Long,
)
