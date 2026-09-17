/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** The preferences search reacts to (T-804). */
data class SearchSettings(
    /** App language code (e.g. `fa`): result dates, digits and how typed dates are read. */
    val languageCode: String,
    /** The user's calendars in order; the first available one writes result dates and reads typed numbers. */
    val calendars: List<CalendarSystem>,
    /**
     * The device time zone id: personal, device and subscription results are dated in it, so a change re-runs the
     * search (review I06). Empty when the source does not track it.
     */
    val timeZoneId: String = "",
)

/** Where a found event comes from. */
enum class SearchEventKind {
    OFFICIAL,
    PERSONAL,
    DEVICE,
    SUBSCRIPTION,
}

/** An event offered to the search, already titled in the app language. */
data class SearchEvent(
    /** Identifier within [kind]: the dataset id, or the stored or provider id as text. */
    val id: String,
    val kind: SearchEventKind,
    val title: String,
    /** Other names the event is known by (aliases, titles in other languages). */
    val aliases: List<String> = emptyList(),
    /** The event's next day from today, when it has one. */
    val nextDay: Jdn? = null,
    val isHoliday: Boolean = false,
)

/** The preferences search reads; re-emits on change. Implemented in `:app` over the data layer. */
fun interface SearchSettingsSource {
    fun settings(): Flow<SearchSettings>
}

/**
 * Events that may match [query]: official events (e.g. through the T-304 index), personal, device and subscription
 * events. The source may return extra candidates; the screen ranks them all the same way. Implemented in `:app`.
 */
fun interface SearchEventSource {
    suspend fun events(
        query: String,
        languageCode: String,
        limit: Int,
    ): List<SearchEvent>
}

/** The searchable settings and tools, titled in the app language. */
fun interface SearchCatalogSource {
    fun entries(): List<SearchEntry>
}

/** The current civil day; emits again when the day changes. */
fun interface SearchTodaySource {
    fun today(): Flow<Jdn>
}

/** Queries the user opened results for, newest first. Implemented in `:app` (a session store is provided here). */
interface RecentQueriesStore {
    fun queries(): Flow<List<String>>

    suspend fun add(query: String)

    suspend fun clear()
}

/** [SearchTodaySource] reading [provider] every [interval], so a new day shows within [interval]. */
class TickingSearchTodaySource(
    private val provider: TodayProvider,
    private val interval: Duration = 1.minutes,
) : SearchTodaySource {
    init {
        require(interval.isPositive()) { "interval must be positive (was $interval)" }
    }

    override fun today(): Flow<Jdn> =
        flow {
            while (true) {
                emit(provider.today())
                delay(interval)
            }
        }.distinctUntilChanged()
}

/**
 * [RecentQueriesStore] kept for the life of the instance: at most [capacity] queries, newest first, a repeated query
 * (compared by [SearchMatcher.key]) moves to the front, blank queries are ignored.
 */
class SessionRecentQueriesStore(
    private val capacity: Int = DEFAULT_CAPACITY,
) : RecentQueriesStore {
    private val state = MutableStateFlow<List<String>>(emptyList())

    init {
        require(capacity > 0) { "capacity must be positive (was $capacity)" }
    }

    override fun queries(): Flow<List<String>> = state.asStateFlow()

    override suspend fun add(query: String) {
        val text = query.trim()
        val key = SearchMatcher.key(text)
        if (key.isNotEmpty()) {
            state.update { current ->
                (listOf(text) + current.filterNot { SearchMatcher.key(it) == key }).take(capacity)
            }
        }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }

    companion object {
        /** Queries kept when no capacity is given. */
        const val DEFAULT_CAPACITY: Int = 8
    }
}
