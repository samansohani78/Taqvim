/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * A day on which a recurring personal event does not occur (`event_exceptions`, T-1003): the iCalendar EXDATE of the
 * occurrence that would start on [dayJdn].
 */
@Entity(
    tableName = "event_exceptions",
    primaryKeys = ["event_id", "day_jdn"],
    foreignKeys = [
        ForeignKey(
            entity = PersonalEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EventExceptionEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "day_jdn") val dayJdn: Long,
)

/**
 * The changed occurrence of a recurring personal event that would start on [originalJdn] (`event_overrides`, T-1003),
 * the iCalendar RECURRENCE-ID instance. It replaces that occurrence with its own title, notes and days and times (in
 * the event's time zone; `null` times mark an all-day instance); a `null` [colorArgb] keeps the event's color. A
 * [cancelled] override removes the occurrence.
 */
@Entity(
    tableName = "event_overrides",
    primaryKeys = ["event_id", "original_jdn"],
    foreignKeys = [
        ForeignKey(
            entity = PersonalEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EventOverrideEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "original_jdn") val originalJdn: Long,
    val title: String,
    val notes: String = "",
    @ColumnInfo(name = "start_jdn") val startJdn: Long,
    @ColumnInfo(name = "start_minute") val startMinute: Int? = null,
    @ColumnInfo(name = "end_jdn") val endJdn: Long,
    @ColumnInfo(name = "end_minute") val endMinute: Int? = null,
    @ColumnInfo(name = "color_argb") val colorArgb: Int? = null,
    val cancelled: Boolean = false,
)
