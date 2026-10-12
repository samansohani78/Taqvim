/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.events.PersonalEventDays
import ir.taqvim.data.events.PersonalEventRecord
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.DeviceTimeZone
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.search.SearchEvent
import ir.taqvim.feature.search.SearchEventKind
import ir.taqvim.feature.search.SearchEventSource
import ir.taqvim.feature.search.SearchMatcher
import ir.taqvim.feature.search.SearchSettings
import ir.taqvim.feature.search.SearchSettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * The search screen's preferences (T-804) from the stored user preferences (T-600), with the device zone from [zones]
 * so a zone change re-dates the results (review I06).
 */
internal class PreferencesSearchSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
) : SearchSettingsSource {
    override fun settings(): Flow<SearchSettings> =
        combine(preferences.preferences, zones) { stored, zone ->
            SearchSettings(stored.languageSpec().code, stored.calendars, zone.id)
        }.distinctUntilChanged()
}

/** The stored events the search reads, loaded when a query runs (T-601). */
internal class SearchEventStores(
    /** Every personal event with its recurrence, exception days and overridden occurrences. */
    val personal: suspend () -> List<PersonalEventRecord>,
)

/**
 * [SearchEventSource] (T-804) over every event source: official events through the T-304 index with their aliases,
 * titles in other languages and next occurrence, then personal events whose title contains the query. A personal
 * event's day is its next occurrence as the calendar shows it (ADR-0031), with recurring events expanded with the
 * user's personal [calendars] over [window] days, dated in the device [zone]. At most `limit` events come from each
 * source; the screen ranks them all.
 *
 * Every source here is local: the dataset compiled into the app and the user's own rows (ADR-0054).
 */
internal class CompositeSearchEventSource(
    private val official: OfficialEventSearchSource,
    private val stores: SearchEventStores,
    private val today: TodayProvider,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val window: Int = DEFAULT_WINDOW_DAYS,
    private val calendars: suspend () -> CalendarProvider = { CalendarProvider.DEFAULT },
    definitions: List<EventDefinition> = OfficialEvents.ALL,
) : SearchEventSource {
    private val byId by lazy { definitions.associateBy { it.id.value } }

    override suspend fun events(
        query: String,
        languageCode: String,
        limit: Int,
    ): List<SearchEvent> {
        val key = SearchMatcher.key(query)
        if (key.isEmpty() || limit <= 0) return emptyList()
        val from = today.today()
        val zone = zone()
        val matches: (String) -> Boolean = { SearchMatcher.key(it).contains(key) }
        val personal = stores.personal().filter { matches(it.event.title) }.take(limit)
        return officialEvents(query, languageCode, limit, from) + personal(personal, from, zone)
    }

    private suspend fun personal(
        records: List<PersonalEventRecord>,
        today: Jdn,
        zone: TimeZone,
    ): List<SearchEvent> {
        if (records.isEmpty()) return emptyList()
        val calendars = calendars()
        return records.map { record ->
            SearchEvent(
                id = record.event.id.toString(),
                kind = SearchEventKind.PERSONAL,
                title = record.event.title,
                nextDay = PersonalEventDays.nextShownDay(record, calendars, today, zone, window),
            )
        }
    }

    private suspend fun officialEvents(
        query: String,
        languageCode: String,
        limit: Int,
        today: Jdn,
    ): List<SearchEvent> =
        official.search(query, limit, today).map { result ->
            val definition = byId[result.eventId]
            val title = definition?.title?.forLanguage(languageCode) ?: result.title
            val otherTitles =
                definition
                    ?.title
                    ?.texts
                    ?.values
                    .orEmpty()
                    .filter { it != title }
            SearchEvent(
                id = result.eventId,
                kind = SearchEventKind.OFFICIAL,
                title = title,
                aliases = (definition?.aliases.orEmpty() + otherTitles).distinct(),
                nextDay = result.nextDay,
                isHoliday = result.isHoliday,
            )
        }

    companion object {
        /** Days ahead a recurring personal event is expanded over to find its next occurrence. */
        const val DEFAULT_WINDOW_DAYS: Int = 366
    }
}
