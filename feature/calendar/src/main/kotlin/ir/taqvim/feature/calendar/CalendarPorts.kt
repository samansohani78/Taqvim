/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone

/** The preferences the calendar screen reacts to (T-800, T-801). */
data class CalendarSettings(
    /** The user's calendars in order; the first available one is the primary calendar. */
    val calendars: List<CalendarSystem>,
    val weekStart: Weekday,
    val islamicVariant: IslamicVariant,
    /** App language code (e.g. `fa`): month names, digits and spoken dates of the month pager. */
    val languageCode: String,
    /** Whether the month pager shows a week-number column (T-801). */
    val showWeekNumbers: Boolean = false,
    /** Optional official Iranian month starts the user switched on or imported (ADR-0037); `null`: computed. */
    val islamicOverrides: IslamicMonthTable? = null,
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
    /** The dataset source of an [DayEventKind.OFFICIAL] event (T-802 source tooltip); `null` for other kinds. */
    val source: EventSource? = null,
    /** Primary-source citations of an official event, shown in its source tooltip. */
    val citations: List<Citation> = emptyList(),
    /** Where the date of an official event in the Islamic calendar comes from; `null` for other events. */
    val dateOrigin: DateOrigin? = null,
)

/** The chosen place for the Times tab and the Moon of the day details (T-802). */
data class CalendarPlace(
    /** Display name, already localized. */
    val name: String,
    val coordinates: Coordinates,
    val timeZone: TimeZone,
    val prayer: PrayerSettings,
)

/** The chosen place; emits `null` while none is chosen. Implemented in `:app` over the preferences and cities. */
fun interface CalendarPlaceSource {
    fun place(): Flow<CalendarPlace?>
}

/** The current instant, re-emitted at least every minute (next prayer time and the Sun's progress). */
fun interface NowSource {
    fun now(): Flow<Instant>
}

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

/** The events of a range of days in day order; re-emits when they change. Implemented in `:app` (T-801). */
fun interface CalendarMonthSource {
    fun days(range: JdnRange): Flow<List<CalendarDay>>
}

/** Event search in the app language. Implemented in `:app` over the event search index. */
interface EventSearchSource {
    /** At most [limit] results for the non-blank [text], best first. */
    suspend fun search(
        text: String,
        limit: Int,
    ): List<EventSearchResult>
}

/** Stores the display choices of the calendar screen's menu (T-803). Implemented in `:app` over the preferences. */
interface CalendarDisplayStore {
    /** Shows or hides the week-number column of the month pager ([CalendarSettings.showWeekNumbers]). */
    suspend fun setShowWeekNumbers(show: Boolean)

    /** Makes [system] the secondary calendar: second in [CalendarSettings.calendars], added when missing. */
    suspend fun setSecondaryCalendar(system: CalendarSystem)
}

/**
 * Reminders before official events (T-1002): lead times in whole days, at most one per event and lead time, sounding
 * at the all-day reminder time. Implemented in `:app` over the `official_reminders` table.
 */
interface OfficialReminderStore {
    /** Enabled lead times, in days before each occurrence, of the official event with dataset id [eventId]. */
    fun daysBefore(eventId: String): Flow<Set<Int>>

    /** Turns the reminder [daysBefore] (0‥30) days before the official event [eventId] on or off. */
    suspend fun setReminder(
        eventId: String,
        daysBefore: Int,
        enabled: Boolean,
    )

    companion object {
        /** No reminders and nothing stored: the calendar without reminder storage. */
        val NONE: OfficialReminderStore =
            object : OfficialReminderStore {
                override fun daysBefore(eventId: String): Flow<Set<Int>> = flowOf(emptySet())

                override suspend fun setReminder(
                    eventId: String,
                    daysBefore: Int,
                    enabled: Boolean,
                ) = Unit
            }
    }
}

/** The current civil day; emits again when the day changes. */
fun interface TodaySource {
    fun today(): Flow<Jdn>
}
