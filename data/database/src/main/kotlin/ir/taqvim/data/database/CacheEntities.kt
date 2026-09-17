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

/**
 * A subscribed iCalendar feed (`ics_subscriptions`). [lastFetchedAtEpochMillis] is the last full download (whose
 * validators [etag] and [lastModified], RFC 9110 §8.8, enable conditional requests) and [lastCheckedAtEpochMillis] the
 * last answered request, full or "not modified" (T-1003).
 */
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
    @ColumnInfo(name = "last_modified") val lastModified: String? = null,
    @ColumnInfo(name = "last_checked_at_epoch_millis") val lastCheckedAtEpochMillis: Long? = null,
    /** Why the last refresh failed ([SubscriptionErrorCodes]), cleared by the next answered request. */
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "last_error_at_epoch_millis") val lastErrorAtEpochMillis: Long? = null,
    /** How many problems the reader reported in the last downloaded feed (content ignored or approximated). */
    @ColumnInfo(name = "problem_count", defaultValue = "0") val problemCount: Int = 0,
)

/** Cached occurrences of one subscription: how many, and the earliest start and latest end. */
data class IcsCacheSummary(
    @ColumnInfo(name = "subscription_id") val subscriptionId: Long,
    @ColumnInfo(name = "event_count") val eventCount: Int,
    @ColumnInfo(name = "first_start_epoch_millis") val firstStartEpochMillis: Long?,
    @ColumnInfo(name = "last_end_epoch_millis") val lastEndEpochMillis: Long?,
)

/**
 * Stable codes stored in `ics_subscriptions.last_error` (F03): the refresh failure kinds, with an HTTP status as
 * `HTTP_<code>`.
 */
object SubscriptionErrorCodes {
    const val NETWORK: String = "NETWORK"
    const val TIMEOUT: String = "TIMEOUT"
    const val TOO_LARGE: String = "TOO_LARGE"
    const val INSECURE: String = "INSECURE"
    const val INVALID_ADDRESS: String = "INVALID_ADDRESS"
    const val UNREADABLE: String = "UNREADABLE"
    const val HTTP_PREFIX: String = "HTTP_"

    fun http(status: Int): String = HTTP_PREFIX + status

    /** The HTTP status in [code], or `null` when [code] is not an HTTP failure. */
    fun httpStatus(code: String): Int? =
        if (code.startsWith(HTTP_PREFIX)) code.removePrefix(HTTP_PREFIX).toIntOrNull() else null
}

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
