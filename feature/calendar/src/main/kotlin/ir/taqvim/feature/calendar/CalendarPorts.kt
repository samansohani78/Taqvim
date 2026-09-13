/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlinx.coroutines.flow.Flow

/** The preferences the calendar screen reacts to (T-800). */
data class CalendarSettings(
    /** The user's calendars in order; the first available one is the primary calendar. */
    val calendars: List<CalendarSystem>,
    val weekStart: Weekday,
    val islamicVariant: IslamicVariant,
)

/** Where an event shown on a day comes from. */
enum class DayEventKind {
    OFFICIAL,
    PERSONAL,
    DEVICE,
    SUBSCRIPTION,
}

/** One event on a day, already titled in the app language. */
data class DayEventItem(
    /** Identifier within [kind]: the dataset id, or the stored or provider id as text. */
    val id: String,
    val kind: DayEventKind,
    val title: String,
    val isHoliday: Boolean,
)

/** Everything the calendar screen shows for the civil day [jdn]. */
data class CalendarDay(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    /** Events in display order (holidays and official events first). */
    val events: List<DayEventItem>,
)

/** One event found by the calendar search. */
data class EventSearchResult(
    val eventId: String,
    val title: String,
    val isHoliday: Boolean,
    /** The first day on or after today on which the event occurs, or `null` when none is known. */
    val nextDay: Jdn?,
)

/** The user's calendar preferences; re-emits on every change. Implemented in `:app` over the data layer. */
fun interface CalendarSettingsSource {
    fun settings(): Flow<CalendarSettings>
}

/** The events of one day; re-emits when they change. Implemented in `:app` over the events repository. */
fun interface CalendarDaySource {
    fun day(jdn: Jdn): Flow<CalendarDay>
}

/** Event search in the app language. Implemented in `:app` over the event search index. */
interface EventSearchSource {
    /** At most [limit] results for the non-blank [text], best first. */
    suspend fun search(
        text: String,
        limit: Int,
    ): List<EventSearchResult>
}

/** The current civil day; emits again when the day changes. */
fun interface TodaySource {
    fun today(): Flow<Jdn>
}
