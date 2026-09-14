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
import ir.taqvim.core.events.SearchQuery
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderDao
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.toEntity
import ir.taqvim.data.database.toRule
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [PersonalEventStore] (T-1000) over the Room DAOs (T-601): an event, its recurrence and its reminders are written in
 * one [transactions] block. Dates are converted with [arithmetic], which carries the user's Islamic variant; events in
 * a calendar without arithmetic (Nepali, T-105) load as missing.
 */
internal class RoomPersonalEventStore(
    private val events: PersonalEventDao,
    private val reminders: ReminderDao,
    private val transactions: TransactionRunner,
    private val clock: Clock,
    private val arithmetic: suspend () -> Map<CalendarSystem, CalendarArithmetic>,
) : PersonalEventStore {
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
                events.deleteRecurrence(storedId)
            } else {
                events.upsertRecurrence(rule.toEntity(storedId, event.calendar))
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

/** [EditorSettingsSource] (T-1000) from the stored preferences, with new events in the device's time zone. */
internal class PreferencesEditorSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val anchors: AnchorLookup?,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : EditorSettingsSource {
    override fun settings(): Flow<EditorSettings> =
        preferences.preferences.map { editorSettings(it, zone().id, anchors) }.distinctUntilChanged()
}

/**
 * Editor settings under [preferences]: the app language, the user's computable calendars in order (Persian when none
 * is), new events in [timeZoneId], and every computable calendar with the user's Islamic variant.
 */
internal fun editorSettings(
    preferences: UserPreferences,
    timeZoneId: String,
    anchors: AnchorLookup?,
): EditorSettings {
    val arithmetic = preferences.availableArithmetic()
    val calendars =
        preferences.calendars
            .distinct()
            .filter { it in arithmetic }
            .ifEmpty { listOf(CalendarSystem.PERSIAN) }
    return EditorSettings(preferences.languageSpec(), calendars, timeZoneId, arithmetic, anchors)
}

/**
 * [AnchorLookup] over the official dataset (T-304) for phrases such as "3 days before Nowruz": the event whose title
 * or alias equals or starts with the query, on its occurrence closest to the reference day (previous, same or next
 * year of its own calendar). Looser matches are ignored so ordinary words never become anchors.
 */
internal class OfficialAnchorLookup(
    definitions: List<EventDefinition> = OfficialEvents.ALL,
) : AnchorLookup {
    private val index by lazy { EventSearchIndex(definitions) }
    private val lookup by lazy { EventLookup(definitions) }

    override fun find(
        query: String,
        reference: Jdn,
    ): Jdn? {
        val definition =
            index
                .search(SearchQuery(query, limit = 1))
                .firstOrNull { it.kind == MatchKind.EXACT || it.kind == MatchKind.PREFIX }
                ?.definition ?: return null
        val calendar = CalendarProvider.DEFAULT.calendarFor(definition.calendar) ?: return null
        val year = calendar.fromJdn(reference).year
        return (year - 1..year + 1)
            .flatMap { lookup.occurrencesIn(definition.calendar, it, setOf(definition.source)) }
            .filter { it.definition.id == definition.id }
            .minByOrNull { abs(it.jdn.value - reference.value) }
            ?.jdn
    }
}
