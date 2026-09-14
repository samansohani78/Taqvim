/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.DeviceEventCacheEntity
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.search.SearchEvent
import ir.taqvim.feature.search.SearchEventKind
import ir.taqvim.feature.search.SearchEventSource
import ir.taqvim.feature.search.SearchMatcher
import ir.taqvim.feature.search.SearchSettings
import ir.taqvim.feature.search.SearchSettingsSource
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The search screen's preferences (T-804) from the stored user preferences (T-600). */
internal class PreferencesSearchSettingsSource(
    private val preferences: UserPreferencesRepository,
) : SearchSettingsSource {
    override fun settings(): Flow<SearchSettings> =
        preferences.preferences
            .map { SearchSettings(it.languageSpec().code, it.calendars) }
            .distinctUntilChanged()
}

/** The stored and cached events the search reads, each loaded when a query runs (T-601, T-602, T-1003). */
internal class SearchEventStores(
    /** Every personal event. */
    val personal: suspend () -> List<PersonalEventEntity>,
    /** Cached device-calendar instances overlapping `[from, to)` epoch milliseconds. */
    val device: suspend (from: Long, to: Long) -> List<DeviceEventCacheEntity>,
    /** Cached occurrences of enabled subscriptions overlapping `[from, to)` epoch milliseconds. */
    val subscriptions: suspend (from: Long, to: Long) -> List<IcsEventCacheEntity>,
)

/**
 * [SearchEventSource] (T-804) over every event source: official events through the T-304 index with their aliases,
 * titles in other languages and next occurrence, then personal, device and subscription events whose title contains
 * the query. Device and subscription events are searched from today on, over [window] days, in the device [zone]. At
 * most `limit` events come from each source; the screen ranks them all.
 */
internal class CompositeSearchEventSource(
    private val official: OfficialEventSearchSource,
    private val stores: SearchEventStores,
    private val today: TodayProvider,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val window: Int = DEFAULT_WINDOW_DAYS,
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
        val fromMillis = from.startMillis(zone)
        val toMillis = (from + window).startMillis(zone)
        val matches: (String) -> Boolean = { SearchMatcher.key(it).contains(key) }
        val personal = stores.personal().filter { matches(it.title) }.take(limit)
        val device = stores.device(fromMillis, toMillis).filter { matches(it.title) }.take(limit)
        val subscriptions = stores.subscriptions(fromMillis, toMillis).filter { matches(it.summary) }.take(limit)
        return officialEvents(query, languageCode, limit, from) +
            personal.map { it.toSearchEvent(from) } +
            device.map { it.toSearchEvent(zone) } +
            subscriptions.map { it.toSearchEvent(zone) }
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
        /** Days ahead searched in the device and subscription caches. */
        const val DEFAULT_WINDOW_DAYS: Int = 366
    }
}

private fun PersonalEventEntity.toSearchEvent(today: Jdn): SearchEvent =
    SearchEvent(
        id = id.toString(),
        kind = SearchEventKind.PERSONAL,
        title = title,
        nextDay = Jdn(startJdn).takeIf { it >= today },
    )

private fun DeviceEventCacheEntity.toSearchEvent(zone: TimeZone): SearchEvent =
    SearchEvent(eventId.toString(), SearchEventKind.DEVICE, title, nextDay = dayOf(beginEpochMillis, zone))

private fun IcsEventCacheEntity.toSearchEvent(zone: TimeZone): SearchEvent =
    SearchEvent("$subscriptionId:$uid", SearchEventKind.SUBSCRIPTION, summary, nextDay = dayOf(startEpochMillis, zone))

/** The civil day of [epochMillis] in [zone]. */
private fun dayOf(
    epochMillis: Long,
    zone: TimeZone,
): Jdn =
    Instant
        .fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(zone)
        .date
        .toJdn()
