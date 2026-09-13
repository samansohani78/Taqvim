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

/** A subscribed iCalendar feed (`ics_subscriptions`); [etag] enables conditional refresh. */
@Entity(tableName = "ics_subscriptions", indices = [Index(value = ["url"], unique = true)])
data class IcsSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Int? = null,
    val enabled: Boolean = true,
    @ColumnInfo(name = "refresh_interval_minutes") val refreshIntervalMinutes: Int,
    @ColumnInfo(name = "last_fetched_at_epoch_millis") val lastFetchedAtEpochMillis: Long? = null,
    val etag: String? = null,
)

/** One expanded occurrence of a subscribed feed (`ics_events_cache`), replaced on every refresh. */
@Entity(
    tableName = "ics_events_cache",
    primaryKeys = ["subscription_id", "uid", "start_epoch_millis"],
    foreignKeys = [
        ForeignKey(
            entity = IcsSubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscription_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("start_epoch_millis")],
)
data class IcsEventCacheEntity(
    @ColumnInfo(name = "subscription_id") val subscriptionId: Long,
    val uid: String,
    @ColumnInfo(name = "start_epoch_millis") val startEpochMillis: Long,
    @ColumnInfo(name = "end_epoch_millis") val endEpochMillis: Long,
    @ColumnInfo(name = "all_day") val allDay: Boolean,
    val summary: String,
    val location: String = "",
    val description: String = "",
)

/** One instance read from the device calendar provider (`device_events_cache`). */
@Entity(
    tableName = "device_events_cache",
    primaryKeys = ["event_id", "begin_epoch_millis"],
    indices = [Index("begin_epoch_millis")],
)
data class DeviceEventCacheEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
    @ColumnInfo(name = "begin_epoch_millis") val beginEpochMillis: Long,
    @ColumnInfo(name = "end_epoch_millis") val endEpochMillis: Long,
    @ColumnInfo(name = "all_day") val allDay: Boolean,
    val title: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Int? = null,
)

/** Severity of a diagnostics entry. */
enum class DiagnosticsLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** One local diagnostics entry (`diagnostics_log`), a ring of at most [DiagnosticsDao.MAX_ROWS] rows. */
@Entity(tableName = "diagnostics_log")
data class DiagnosticsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "at_epoch_millis") val atEpochMillis: Long,
    val level: DiagnosticsLevel,
    val tag: String,
    val message: String,
)
