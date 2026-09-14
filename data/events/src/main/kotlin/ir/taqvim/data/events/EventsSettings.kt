/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.calendar.HijriOffset
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The preferences that decide which events are shown and how days are dated (T-305). */
data class EventsSettings(
    val preferences: EventPreferences,
    val weekend: Set<Weekday>,
    /** The user's lunar Hijri correction, or `null` when none was set. */
    val hijriOffset: HijriOffset?,
)

/**
 * These preferences as [EventsSettings]; an out-of-range stored Hijri offset is ignored. The sources default to the
 * stored ones, which follow the language until the user chooses them (`AppSettings.defaultEventSources`, ADR-0007 §3).
 */
fun UserPreferences.toEventsSettings(
    enabledSources: Set<EventSource> = app.enabledEventSources,
    homeTimeZone: TimeZone = TimeZone.currentSystemDefault(),
): EventsSettings =
    EventsSettings(
        preferences =
            EventPreferences(
                enabledSources = enabledSources,
                homeTimeZone = homeTimeZone,
                islamicVariant = islamicVariant,
            ),
        weekend = weekend,
        hijriOffset = storedHijriOffset(),
    )

private fun UserPreferences.storedHijriOffset(): HijriOffset? {
    val setAt = hijriOffsetSetAtEpochMillis
    val usable = hijriOffsetDays != 0 && hijriOffsetDays in -HijriOffset.MAX_DAYS..HijriOffset.MAX_DAYS
    return if (setAt != null && usable) HijriOffset(hijriOffsetDays, Instant.fromEpochMilliseconds(setAt)) else null
}
