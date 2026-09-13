/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.DeviceEventCacheEntity
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** One instance of a device calendar event (T-602). */
data class DeviceEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    /** Opaque ARGB display color, or `null` when the provider reports none. */
    val colorArgb: Int?,
    /** Civil days the instance covers (see [DeviceEventMapping.days]). */
    val days: JdnRange,
)

/** One row of `CalendarContract.Instances` as read from the provider, before filtering. */
data class InstanceRow(
    val eventId: Long,
    val calendarId: Long,
    val title: String?,
    val beginEpochMillis: Long,
    val endEpochMillis: Long,
    val allDay: Boolean,
    /** `DISPLAY_COLOR`: the event color, or the calendar color when the event has none. */
    val displayColor: Int?,
    /** The calendar's `VISIBLE` flag. */
    val visible: Boolean,
    /** The event's `DELETED` flag (deleted locally, not yet removed by the sync adapter). */
    val deleted: Boolean,
)

/** A provider query window `[fromEpochMillis, toEpochMillis)` in UTC milliseconds. */
data class InstantWindow(
    val fromEpochMillis: Long,
    val toEpochMillis: Long,
)

/** Pure mapping between provider rows, cache rows and civil days; no Android types (T-602). */
object DeviceEventMapping {
    /** Alpha channel 0xFF000000; the provider does not define an alpha convention, so colors are shown opaque. */
    private const val OPAQUE_ALPHA: Int = -0x01000000

    /**
     * Civil days covered by an instance whose end is exclusive. All-day instances are UTC midnight boundaries
     * (`CalendarContract.Events`: all-day events use the UTC time zone), so their days are the UTC dates in every
     * device zone; timed instances use their dates in [zone]. A zero-length instance covers the day it starts on.
     */
    fun days(
        beginEpochMillis: Long,
        endEpochMillis: Long,
        allDay: Boolean,
        zone: TimeZone,
    ): JdnRange {
        val dayZone = if (allDay) TimeZone.UTC else zone
        val lastMillis = maxOf(beginEpochMillis, endEpochMillis - 1)
        val first = Instant.fromEpochMilliseconds(beginEpochMillis).toJdn(dayZone)
        return first..Instant.fromEpochMilliseconds(lastMillis).toJdn(dayZone)
    }

    /**
     * The provider window for [days] in [zone], widened by one day on each side so every all-day instance of those
     * days is included whatever the zone offset; results are narrowed again with [overlaps].
     */
    fun window(
        days: JdnRange,
        zone: TimeZone,
    ): InstantWindow {
        require(!days.isEmpty()) { "days must not be empty" }
        val from = (days.start - 1).toLocalDate().atStartOfDayIn(zone)
        val to = (days.endInclusive + 2).toLocalDate().atStartOfDayIn(zone)
        return InstantWindow(from.toEpochMilliseconds(), to.toEpochMilliseconds())
    }

    /** Cache row for a provider row, or `null` for instances of invisible calendars and deleted events. */
    fun toCacheEntity(row: InstanceRow): DeviceEventCacheEntity? =
        if (row.visible && !row.deleted) {
            DeviceEventCacheEntity(
                eventId = row.eventId,
                calendarId = row.calendarId,
                beginEpochMillis = row.beginEpochMillis,
                endEpochMillis = row.endEpochMillis,
                allDay = row.allDay,
                title = row.title.orEmpty(),
                colorArgb = row.displayColor?.let(::opaque),
            )
        } else {
            null
        }

    /** [color] with a fully opaque alpha channel. */
    fun opaque(color: Int): Int = color or OPAQUE_ALPHA

    /** The event of a cache row, dated in [zone]. */
    fun toDeviceEvent(
        entity: DeviceEventCacheEntity,
        zone: TimeZone,
    ): DeviceEvent =
        DeviceEvent(
            eventId = entity.eventId,
            calendarId = entity.calendarId,
            title = entity.title,
            begin = Instant.fromEpochMilliseconds(entity.beginEpochMillis),
            end = Instant.fromEpochMilliseconds(entity.endEpochMillis),
            allDay = entity.allDay,
            colorArgb = entity.colorArgb,
            days = days(entity.beginEpochMillis, entity.endEpochMillis, entity.allDay, zone),
        )

    /** Whether two non-empty day ranges share at least one day. */
    fun overlaps(
        a: JdnRange,
        b: JdnRange,
    ): Boolean = a.start <= b.endInclusive && b.start <= a.endInclusive
}
