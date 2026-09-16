/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.timeline.TimelineClockSource
import ir.taqvim.feature.timeline.TimelineDay
import ir.taqvim.feature.timeline.TimelineDaysSource
import ir.taqvim.feature.timeline.TimelineEvent
import ir.taqvim.feature.timeline.TimelineEventKind
import ir.taqvim.feature.timeline.TimelineNow
import ir.taqvim.feature.timeline.TimelinePlace
import ir.taqvim.feature.timeline.TimelinePlaceSource
import ir.taqvim.feature.timeline.TimelineSettings
import ir.taqvim.feature.timeline.TimelineSettingsSource
import ir.taqvim.feature.times.TimesSettingsSource
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 1_440
private const val MILLIS_PER_MINUTE = 60_000L

/** The timeline's preferences (T-900) from the stored user preferences (T-600). */
internal class PreferencesTimelineSettingsSource(
    private val preferences: UserPreferencesRepository,
) : TimelineSettingsSource {
    override fun settings(): Flow<TimelineSettings> =
        preferences.preferences
            .map { TimelineSettings(it.calendars, it.weekStart, it.islamicVariant, it.languageCode) }
            .distinctUntilChanged()
}

/**
 * Timeline days (T-900) from the events repository (T-305), titled in [language]. Dataset events are all-day; timed
 * personal, device and subscription events become minute spans of each civil day in the device [zone].
 */
internal class RepositoryTimelineDaysSource(
    private val days: (JdnRange) -> Flow<List<DayEvents>>,
    private val language: Flow<String>,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : TimelineDaysSource {
    override fun days(range: JdnRange): Flow<List<TimelineDay>> =
        combine(days.invoke(range), language.distinctUntilChanged()) { days, language ->
            val zone = zone()
            days.map { it.toTimelineDay(language, zone) }
        }.distinctUntilChanged()
}

/** This day for the timeline: dataset events first, then personal, device and subscription events. */
internal fun DayEvents.toTimelineDay(
    language: String,
    zone: TimeZone,
): TimelineDay {
    val official =
        official.map { occurrence ->
            val definition = occurrence.definition
            TimelineEvent(
                id = definition.id.value,
                kind = TimelineEventKind.OFFICIAL,
                title = definition.title.forLanguage(language),
                isHoliday = occurrence.isHoliday,
                isAllDay = true,
            )
        }
    val personal = personal.mapNotNull { it.toTimelineEvent(jdn, zone) }
    val device =
        device.mapNotNull {
            event(it.eventId.toString(), TimelineEventKind.DEVICE, it.title, it.allDay, jdn, it.begin, it.end, zone)
        }
    val subscriptions =
        ics.mapNotNull {
            val id = "${it.subscriptionId}:${it.uid}"
            event(id, TimelineEventKind.SUBSCRIPTION, it.summary, it.allDay, jdn, it.begin, it.end, zone)
        }
    return TimelineDay(jdn, isHoliday, isWeekend, official + personal + device + subscriptions)
}

/** A personal occurrence on [day]: all-day without times, otherwise its span from its own time zone to [zone]. */
private fun PersonalOccurrence.toTimelineEvent(
    day: Jdn,
    zone: TimeZone,
): TimelineEvent? {
    val start = startMinute
    val end = endMinute
    if (start == null || end == null) {
        return TimelineEvent(itemId, TimelineEventKind.PERSONAL, title, isHoliday = false, isAllDay = true)
    }
    val eventZone = runCatching { TimeZone.of(timeZoneId) }.getOrDefault(zone)
    val begin = days.start.at(start, eventZone)
    val finish = days.endInclusive.at(end, eventZone)
    return event(itemId, TimelineEventKind.PERSONAL, title, false, day, begin, finish, zone)
}

/** An event on [day]: all-day, or the part of `[begin, end)` inside that civil day of [zone]; `null` when none is. */
@Suppress("LongParameterList") // One event's fields, shared by every source.
private fun event(
    id: String,
    kind: TimelineEventKind,
    title: String,
    allDay: Boolean,
    day: Jdn,
    begin: Instant,
    end: Instant,
    zone: TimeZone,
): TimelineEvent? {
    if (allDay) return TimelineEvent(id, kind, title, isHoliday = false, isAllDay = true)
    val span = minuteSpanOn(day, begin, end, zone) ?: return null
    return TimelineEvent(id, kind, title, false, false, span.first, span.last + 1)
}

/**
 * The minutes of civil [day] in [zone] that `[begin, end)` covers, as `start..endExclusive - 1`, or `null` when the
 * interval does not reach into that day. An event ending exactly at midnight ends at minute 1440; a zero-length event
 * keeps one minute so it stays visible.
 */
internal fun minuteSpanOn(
    day: Jdn,
    begin: Instant,
    end: Instant,
    zone: TimeZone,
): IntRange? {
    val dayStart = day.startOfDay(zone)
    val nextStart = (day + 1).startOfDay(zone)
    val endedBefore = begin < dayStart && end <= dayStart
    if (begin >= nextStart || endedBefore) return null
    val from = maxOf(begin, dayStart)
    val until = minOf(maxOf(end, begin), nextStart)
    val startMinute = if (from == dayStart) 0 else from.minuteOfDay(zone)
    val endMinute = if (until == nextStart) MINUTES_PER_DAY else until.minuteOfDay(zone)
    return startMinute until maxOf(endMinute, startMinute + 1).coerceAtMost(MINUTES_PER_DAY)
}

/** The instant this day begins in [zone]. */
internal fun Jdn.startOfDay(zone: TimeZone): Instant = toLocalDate().atStartOfDayIn(zone)

/** The epoch milliseconds at which this day begins in [zone]. */
internal fun Jdn.startMillis(zone: TimeZone): Long = startOfDay(zone).toEpochMilliseconds()

/** The instant at [minute] of this civil day in [zone]; minute 1440 is the start of the next day. */
private fun Jdn.at(
    minute: Int,
    zone: TimeZone,
): Instant =
    if (minute >= MINUTES_PER_DAY) {
        (this + 1).startOfDay(zone)
    } else {
        toLocalDate().atTime(minute / MINUTES_PER_HOUR, minute % MINUTES_PER_HOUR).toInstant(zone)
    }

private fun Instant.minuteOfDay(zone: TimeZone): Int =
    toLocalDateTime(zone).let { it.hour * MINUTES_PER_HOUR + it.minute }

/** The timeline's place (T-900) from the Times tab's settings (T-1100): the chosen place, zone and prayer method. */
internal class TimesTimelinePlaceSource(
    private val times: TimesSettingsSource,
) : TimelinePlaceSource {
    override fun place(): Flow<TimelinePlace?> =
        times
            .settings()
            .map { settings -> settings?.let { TimelinePlace(it.place, it.timeZone, it.prayer) } }
            .distinctUntilChanged()
}

/** [TimelineClockSource] (T-900): the device-zone day and minute, emitted again at every minute boundary. */
internal class DeviceTimelineClockSource(
    private val clock: Clock,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : TimelineClockSource {
    override fun now(): Flow<TimelineNow> =
        flow {
            while (true) {
                val now = clock.now()
                val local = now.toLocalDateTime(zone())
                emit(TimelineNow(local.date.toJdn(), local.hour * MINUTES_PER_HOUR + local.minute))
                delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
            }
        }.distinctUntilChanged()
}
