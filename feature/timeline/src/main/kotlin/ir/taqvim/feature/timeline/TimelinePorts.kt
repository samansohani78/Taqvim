/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** The preferences the timeline reacts to (T-900). */
data class TimelineSettings(
    /** The user's calendars in order; the first available one titles the days. */
    val calendars: List<CalendarSystem>,
    val weekStart: Weekday,
    val islamicVariant: IslamicVariant,
    /** App language code (e.g. `fa`): month and weekday names, digits and spoken descriptions. */
    val languageCode: String,
)

/** Where an event on the timeline comes from. */
enum class TimelineEventKind {
    OFFICIAL,
    PERSONAL,
    DEVICE,
    SUBSCRIPTION,
}

/**
 * One event on one day, already titled in the app language. A timed event occupies [startMinute] until [endMinute]
 * (minutes of the civil day, end exclusive); events crossing midnight are split per day by the provider.
 */
data class TimelineEvent(
    /** Identifier within [kind]: the dataset id, or the stored or provider id as text. */
    val id: String,
    val kind: TimelineEventKind,
    val title: String,
    val isHoliday: Boolean,
    val isAllDay: Boolean,
    val startMinute: Int = 0,
    val endMinute: Int = TimelineGeometry.MINUTES_PER_DAY,
) {
    init {
        require(isAllDay || (startMinute in 0 until endMinute && endMinute <= TimelineGeometry.MINUTES_PER_DAY)) {
            "timed event '$id' needs 0 <= start < end <= 1440 (was $startMinute..$endMinute)"
        }
    }
}

/** Everything the timeline shows for the civil day [jdn]. */
data class TimelineDay(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    /** Events in display order. */
    val events: List<TimelineEvent>,
)

/** The chosen place, for the prayer lines. */
data class TimelinePlace(
    val coordinates: Coordinates,
    val timeZone: TimeZone,
    val prayer: PrayerSettings,
)

/** The current civil day and minute of the day on the device clock. */
data class TimelineNow(
    val day: Jdn,
    val minute: Int,
) {
    init {
        require(minute in 0 until TimelineGeometry.MINUTES_PER_DAY) { "minute of day must be in 0..1439 (was $minute)" }
    }
}

/** The user's calendar preferences; re-emits on every change. Implemented in `:app` over the data layer. */
fun interface TimelineSettingsSource {
    fun settings(): Flow<TimelineSettings>
}

/** The events of a range of days in day order; re-emits when they change. Implemented in `:app`. */
fun interface TimelineDaysSource {
    fun days(range: JdnRange): Flow<List<TimelineDay>>
}

/** The chosen place; emits `null` while none is chosen. Implemented in `:app`. */
fun interface TimelinePlaceSource {
    fun place(): Flow<TimelinePlace?>
}

/** The current day and minute, re-emitted at least every minute. Implemented in `:app`. */
fun interface TimelineClockSource {
    fun now(): Flow<TimelineNow>
}
