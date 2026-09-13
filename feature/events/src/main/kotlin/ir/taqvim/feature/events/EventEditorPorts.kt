/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.core.nlp.ParseContext
import kotlinx.coroutines.flow.Flow

/**
 * A personal event as the editor reads and writes it (T-1000, F-01). Dates are in [calendar]; [startMinute] and
 * [endMinute] are minutes of the day (`0..1439`) in [timeZoneId], both `null` for an all-day event.
 */
data class PersonalEvent(
    val id: Long? = null,
    val title: String,
    val notes: String = "",
    val calendar: CalendarSystem,
    val start: CalendarDate,
    val end: CalendarDate,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val timeZoneId: String,
    val colorArgb: Int? = null,
    /** Repetition counted in [calendar] (ADR-0011), or `null` for a one-off event. */
    val recurrence: RecurrenceRule? = null,
    /** Minutes before each occurrence, ascending and distinct. */
    val reminderMinutes: List<Int> = emptyList(),
    /** The web page the event was taken from, if any. */
    val sourceLink: String? = null,
)

/** Storage of personal events; implemented in `:app` over the Room DAOs (T-601). */
interface PersonalEventStore {
    /** The event with [id], or `null` when it does not exist. */
    suspend fun load(id: Long): PersonalEvent?

    /** Inserts [event] when its id is `null`, otherwise replaces it; returns the stored id. */
    suspend fun save(event: PersonalEvent): Long

    /** Deletes the event with [id] together with its recurrence and reminders. */
    suspend fun delete(id: Long)
}

/** What the editor needs from the preferences; bound in `:app`. */
data class EditorSettings(
    /** App language: names, digits and date phrases. */
    val language: LanguageSpec,
    /** Calendars offered for events, in order; the first one is the calendar of new events. */
    val calendars: List<CalendarSystem>,
    /** Civil time zone of new events (IANA id). */
    val timeZoneId: String,
    /** Arithmetic of every supported calendar, with the user's Islamic variant. */
    val arithmetic: Map<CalendarSystem, CalendarArithmetic> = ParseContext.DEFAULT_CALENDARS,
    /** Finds named events for phrases such as "3 days before Nowruz"; `null` disables them. */
    val anchors: AnchorLookup? = null,
) {
    init {
        require(calendars.isNotEmpty()) { "at least one calendar is needed" }
        require(calendars.all { it in arithmetic }) { "no arithmetic for some of $calendars" }
    }

    /** The arithmetic of [system]. */
    fun arithmeticOf(system: CalendarSystem): CalendarArithmetic =
        requireNotNull(arithmetic[system]) { "no arithmetic for $system" }
}

/** The current [EditorSettings]; re-emits on every change. */
fun interface EditorSettingsSource {
    fun settings(): Flow<EditorSettings>
}
