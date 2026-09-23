/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSearchIndex
import ir.taqvim.core.events.MatchKind
import ir.taqvim.core.events.SearchHit
import ir.taqvim.core.events.SearchQuery
import ir.taqvim.core.i18n.PersianText
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderDao
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.toEntity
import ir.taqvim.data.database.toRule
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import ir.taqvim.data.events.SkyAstronomicalEventSource
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.events.ics.TransactionRunner
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.events.EditorSettings
import ir.taqvim.feature.events.EditorSettingsSource
import ir.taqvim.feature.events.PersonalEvent
import ir.taqvim.feature.events.PersonalEventStore
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [PersonalEventStore] (T-1000) over the Room DAOs (T-601): an event, its recurrence and its reminders are written in
 * one [transactions] block. Dates are converted with [arithmetic], which carries the user's Islamic variant; an event
 * whose calendar [arithmetic] lacks loads as missing.
 */
internal class RoomPersonalEventStore(
    private val events: PersonalEventDao,
    private val reminders: ReminderDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
    private val arithmetic: suspend () -> Map<CalendarSystem, CalendarArithmetic>,
) : PersonalEventStore {
    private val occurrences = EventOccurrenceEdits(events)

    override suspend fun load(id: Long): PersonalEvent? {
        val entity = events.get(id) ?: return null
        val calendar = arithmetic()[entity.calendarSystem] ?: return null
        return PersonalEvent(
            id = entity.id,
            title = entity.title,
            notes = entity.notes,
            calendar = entity.calendarSystem,
            start = calendar.fromJdn(Jdn(entity.startJdn)),
            end = calendar.fromJdn(Jdn(entity.endJdn)),
            startMinute = entity.startMinute,
            endMinute = entity.endMinute,
            timeZoneId = entity.timeZoneId,
            colorArgb = entity.colorArgb,
            recurrence = events.getRecurrence(id)?.toRule(),
            reminderMinutes =
                reminders
                    .reminders(id)
                    .map { it.minutesBefore }
                    .distinct()
                    .sorted(),
            sourceLink = entity.sourceLink,
        )
    }

    override suspend fun save(event: PersonalEvent): Long {
        val calendar = requireNotNull(arithmetic()[event.calendar]) { "no arithmetic for ${event.calendar}" }
        val now = clock.now().toEpochMilliseconds()
        var storedId = 0L
        transactions.inTransaction {
            val existing = event.id?.let { events.get(it) }
            val entity = event.toEntity(calendar, existing, now)
            storedId = if (existing == null) events.insert(entity) else entity.id.also { events.update(entity) }
            val rule = event.recurrence
            if (rule == null) {
                // A one-off event has no occurrences to except or override (T-1003).
                events.deleteRecurrence(storedId)
                events.deleteExceptions(storedId)
                events.deleteOverrides(storedId)
            } else {
                events.upsertRecurrence(rule.toEntity(storedId, event.calendar))
                occurrences.prune(storedId, calendar, event, rule)
            }
            reminders.deleteReminders(storedId)
            event.reminderMinutes.distinct().forEach { minutes ->
                reminders.insertReminder(ReminderEntity(eventId = storedId, minutesBefore = minutes))
            }
        }
        return storedId
    }

    override suspend fun delete(id: Long) {
        events.delete(id)
    }

    override suspend fun loadOccurrence(
        id: Long,
        originalDay: Jdn,
    ): PersonalEvent? {
        val series = load(id) ?: return null
        return occurrences.load(series, calendarOf(series), originalDay)
    }

    override suspend fun saveOccurrence(
        id: Long,
        originalDay: Jdn,
        occurrence: PersonalEvent,
    ) {
        val series = requireNotNull(load(id)) { "no event $id" }
        require(occurrences.load(series, calendarOf(series), originalDay) != null) { "no occurrence on $originalDay" }
        occurrences.save(id, calendarOf(occurrence), originalDay, occurrence)
    }

    override suspend fun cancelOccurrence(
        id: Long,
        originalDay: Jdn,
    ) {
        val series = requireNotNull(load(id)) { "no event $id" }
        occurrences.cancel(series, calendarOf(series), originalDay)
    }

    private suspend fun calendarOf(event: PersonalEvent): CalendarArithmetic =
        requireNotNull(arithmetic()[event.calendar]) { "no arithmetic for ${event.calendar}" }
}

/** This event as a row: creation time and iCalendar UID are kept from [existing], the update time is [now]. */
private fun PersonalEvent.toEntity(
    arithmetic: CalendarArithmetic,
    existing: PersonalEventEntity?,
    now: Long,
): PersonalEventEntity =
    PersonalEventEntity(
        id = existing?.id ?: id ?: 0,
        title = title,
        notes = notes,
        calendarSystem = calendar,
        startJdn = arithmetic.toJdn(start).value,
        startMinute = startMinute,
        endJdn = arithmetic.toJdn(end).value,
        endMinute = endMinute,
        timeZoneId = timeZoneId,
        colorArgb = colorArgb,
        createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
        updatedAtEpochMillis = now,
        icsUid = existing?.icsUid,
        sourceLink = sourceLink,
    )

/**
 * [EditorSettingsSource] (T-1000) from the stored preferences, with the device zone from [zones] (review I06). The zone
 * only sets the time zone of a form created after it and the editor's display (today, recurrence preview): an open
 * form keeps the zone it was created with, so a device zone change never moves a draft's times silently.
 */
internal class PreferencesEditorSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val anchors: AnchorLookup?,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
) : EditorSettingsSource {
    override fun settings(): Flow<EditorSettings> =
        combine(preferences.preferences, zones) { stored, zone -> editorSettings(stored, zone.id, anchors) }
            .distinctUntilChanged()
}

/**
 * Editor settings under [preferences]: the app language, the user's calendars in order (Persian when there are none),
 * new events in [timeZoneId], and every calendar's arithmetic with the user's Islamic variant.
 */
internal fun editorSettings(
    preferences: UserPreferences,
    timeZoneId: String,
    anchors: AnchorLookup?,
): EditorSettings {
    val arithmetic = preferences.availableArithmetic()
    val calendars = preferences.calendars.distinct().ifEmpty { listOf(CalendarSystem.PERSIAN) }
    return EditorSettings(preferences.languageSpec(), calendars, timeZoneId, arithmetic, anchors)
}

/**
 * [AnchorLookup] over the official dataset (T-304) for phrases such as "3 days before Nowruz": the event whose title
 * or alias equals the query, starts with it, or contains it as whole words ("نوروز" in "عید نوروز", review R02), on
 * its occurrence closest to the reference day (previous, same or next year of its own calendar). Fragments of a word
 * and typo matches are ignored so ordinary words never become anchors.
 */
internal class OfficialAnchorLookup(
    definitions: List<EventDefinition> = OfficialEvents.ALL,
) : AnchorLookup {
    private val index by lazy { EventSearchIndex(definitions) }
    private val lookup by lazy { EventLookup(definitions, astronomy = SkyAstronomicalEventSource) }

    override fun find(
        query: String,
        reference: Jdn,
    ): Jdn? {
        val hits =
            index
                .search(SearchQuery(query, limit = CANDIDATES))
                .filter { it.kind == MatchKind.EXACT || it.kind == MatchKind.PREFIX || containsWords(it, query) }
        val best = hits.firstOrNull()?.kind ?: return null
        // Several days can share a name ("آغاز نوروز", "عید نوروز" …); the phrase means the first of them.
        return hits
            .filter { it.kind == best }
            .mapNotNull { nearest(it.definition, reference) }
            .minByOrNull { it.value }
    }

    /** The occurrence of [definition] closest to [reference], in the previous, same or next year of its calendar. */
    private fun nearest(
        definition: EventDefinition,
        reference: Jdn,
    ): Jdn? {
        val calendar = CalendarProvider.DEFAULT.calendarFor(definition.calendar) ?: return null
        val year = calendar.fromJdn(reference).year
        return (year - 1..year + 1)
            .flatMap { lookup.occurrencesIn(definition.calendar, it, setOf(definition.source)) }
            .filter { it.definition.id == definition.id }
            .minByOrNull { abs(it.jdn.value - reference.value) }
            ?.jdn
    }

    /**
     * Whether [hit]'s text holds [query] as a run of whole words. Both are compared as search keys, which ignore
     * spaces, so "نو روز" matches the word "نوروز" as the index does; only the title's word boundaries count.
     */
    private fun containsWords(
        hit: SearchHit,
        query: String,
    ): Boolean {
        if (hit.kind != MatchKind.SUBSTRING) return false
        val wanted = PersianText.searchKey(query)
        val words = PersianText.normalize(hit.matchedText).split(' ').map(PersianText::searchKey)
        val runs = words.indices.flatMap { first -> (first until words.size).map { words.subList(first, it + 1) } }
        return wanted.isNotEmpty() && runs.any { it.joinToString("") == wanted }
    }

    private companion object {
        /** Hits examined for a whole-word match; exact and prefix hits rank first, so a few are enough. */
        const val CANDIDATES = 8
    }
}
