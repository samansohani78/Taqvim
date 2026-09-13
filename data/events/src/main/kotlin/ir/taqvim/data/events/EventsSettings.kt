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
) {
    companion object {
        /**
         * Sources shown while `UserPrefs` stores no choice of its own (settings arrive with T-1500): every source
         * except ancient Iranian festivals, which docs/PLAN.md §5.1 keeps off by default.
         */
        val DEFAULT_ENABLED_SOURCES: Set<EventSource> = EventSource.entries.toSet() - EventSource.ANCIENT_IRAN
    }
}

/** These preferences as [EventsSettings]; an out-of-range stored Hijri offset is ignored. */
fun UserPreferences.toEventsSettings(
    enabledSources: Set<EventSource> = EventsSettings.DEFAULT_ENABLED_SOURCES,
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
