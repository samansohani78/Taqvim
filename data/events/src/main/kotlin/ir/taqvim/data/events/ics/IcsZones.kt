/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** Environment lookups of the iCalendar bindings. */
internal object IcsZones {
    /** The device time zone at the time of each call. */
    val system: () -> TimeZone = { TimeZone.currentSystemDefault() }

    /** Calendars of personal events under the user's current Islamic variant, as the events repository uses them. */
    suspend fun personalCalendars(preferences: Flow<UserPreferences>): CalendarProvider =
        preferences
            .first()
            .let { IslamicCalendarSelection(it.islamicVariant, overrides = it.islamicOverride.table) }
            .providerFor(EventSource.USER)
}
