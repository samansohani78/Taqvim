/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Jdn
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The unified search (T-804, F-12): events, tools and settings matched as the user types. The query is debounced by
 * [DEBOUNCE_MILLIS]; a newer query cancels the running search. Matching runs on [dispatcher]; a failing event source
 * leaves the tools and settings results. Opening a result records the query and leaves through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    settingsSource: SearchSettingsSource,
    todaySource: SearchTodaySource,
    private val eventSource: SearchEventSource,
    private val catalogSource: SearchCatalogSource,
    private val recentStore: RecentQueriesStore,
    private val dispatcher: CoroutineDispatcher,
    /** Text searched when the screen opens (a T-1103 link); `null` opens it empty. */
    initialQuery: String? = null,
) : ViewModel() {
    private val query = MutableStateFlow(initialQuery.orEmpty())
    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val effectChannel = Channel<SearchEffect>(Channel.BUFFERED)

    private val environment: Flow<Environment> =
        combine(
            settingsSource.settings().distinctUntilChanged(),
            todaySource.today().distinctUntilChanged(),
        ) { settings, today -> Environment(SearchDates(settings), today) }

    private val found: Flow<Found?> =
        combine(
            query
                .debounce { if (it.isBlank()) 0L else DEBOUNCE_MILLIS }
                .map { it.trim() }
                .distinctUntilChanged(),
            environment,
        ) { text, environment -> text to environment }
            .mapLatest { (text, environment) -> search(text, environment) }
            .onStart<Found?> { emit(null) }

    /** One-shot navigation effects. */
    val effects: Flow<SearchEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<SearchUiState> =
        combine(query, filter, recentStore.queries(), found) { text, filter, recent, found ->
            SearchUiState(text, filter, recent.toImmutableList(), content(text, filter, found))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SearchUiState())

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.ChangeQuery -> query.value = action.text
            SearchAction.ClearQuery -> query.value = ""
            is SearchAction.SelectFilter -> filter.value = action.filter
            is SearchAction.UseRecent -> query.value = action.query
            SearchAction.ClearRecent -> viewModelScope.launch { recentStore.clear() }
            is SearchAction.OpenResult -> open(action.result.target)
            is SearchAction.OpenDate -> open(SearchTarget.Day(action.date.jdn))
        }
    }

    private fun open(target: SearchTarget) {
        viewModelScope.launch {
            recentStore.add(query.value)
            effectChannel.send(SearchEffect.Navigate(target))
        }
    }

    private suspend fun search(
        text: String,
        environment: Environment,
    ): Found {
        if (text.isEmpty()) return Found(text, emptyList(), null)
        return withContext(dispatcher) {
            val dates = environment.dates
            val events =
                runCatching { eventSource.events(text, dates.language.code, EVENT_CANDIDATES) }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrDefault(emptyList())
            val sections = SearchRanker.rank(text, events, catalogSource.entries(), dates::label)
            Found(text, sections, dates.suggest(text, environment.today))
        }
    }

    private fun content(
        text: String,
        filter: SearchFilter,
        found: Found?,
    ): SearchContent {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> SearchContent.Idle
            found == null || found.query != trimmed -> SearchContent.Searching
            else -> found.content(filter)
        }
    }

    private class Environment(
        val dates: SearchDates,
        val today: Jdn,
    )

    private class Found(
        val query: String,
        val sections: List<SearchSection>,
        val date: DateSuggestion?,
    ) {
        fun content(filter: SearchFilter): SearchContent {
            val shown = sections.filter { filter.group == null || it.group == filter.group }
            val shownDate = date.takeIf { filter == SearchFilter.ALL }
            return if (shown.isEmpty() && shownDate == null) {
                SearchContent.NoResults
            } else {
                SearchContent.Results(shown.toImmutableList(), shownDate)
            }
        }
    }

    companion object {
        /** Quiet time after the last keystroke before searching. */
        const val DEBOUNCE_MILLIS: Long = 250L

        /** Events requested from the source per query. */
        const val EVENT_CANDIDATES: Int = 60

        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
