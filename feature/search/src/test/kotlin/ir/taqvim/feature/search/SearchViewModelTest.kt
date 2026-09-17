/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        sources: FakeSearchSources,
        recent: RecentQueriesStore = SessionRecentQueriesStore(),
    ): SearchViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return SearchViewModel(sources, sources, sources, sources, recent, dispatcher)
    }

    private suspend fun ReceiveTurbine<SearchUiState>.awaitState(predicate: (SearchUiState) -> Boolean): SearchUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    /** The day label of the single result, if the state shows exactly one. */
    private fun SearchUiState.dayLabel(): String? =
        (content as? SearchContent.Results)
            ?.sections
            ?.singleOrNull()
            ?.results
            ?.singleOrNull()
            ?.dayLabel

    private suspend fun ReceiveTurbine<SearchUiState>.awaitResults(): SearchContent.Results =
        awaitState { it.content is SearchContent.Results }.content as SearchContent.Results

    @Test
    fun `typing is debounced into one search and Nowruz is found`(): Unit =
        runTest {
            val sources = FakeSearchSources(PERSIAN_FA)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                awaitItem().content shouldBe SearchContent.Idle
                listOf("ن", "نو", "نور").forEach {
                    viewModel.onAction(SearchAction.ChangeQuery(it))
                    advanceTimeBy(100)
                }
                awaitState { it.query == "نور" }.content shouldBe SearchContent.Searching
                val results = awaitResults()
                results.sections
                    .first()
                    .results
                    .first()
                    .title shouldBe "عید نوروز"
                results.date.shouldBeNull()
                sources.calls shouldContainExactly listOf("نور")
            }
        }

    @Test
    fun `filters hide other groups and the date, and nothing left means no results`(): Unit =
        runTest {
            val viewModel = viewModel(FakeSearchSources(PERSIAN_FA))
            viewModel.uiState.test {
                viewModel.onAction(SearchAction.ChangeQuery("۱ فروردین ۱۴۰۵"))
                awaitResults().date shouldBe DateSuggestion(NOWRUZ_1405, "شنبه ۱ فروردین ۱۴۰۵")
                viewModel.onAction(SearchAction.SelectFilter(SearchFilter.EVENTS))
                awaitState { it.filter == SearchFilter.EVENTS }.content shouldBe SearchContent.NoResults
                viewModel.onAction(SearchAction.ChangeQuery("نور"))
                val events = awaitResults()
                events.sections.map { it.group } shouldBe listOf(SearchGroup.EVENTS)
                events.date.shouldBeNull()
                viewModel.onAction(SearchAction.SelectFilter(SearchFilter.TOOLS))
                awaitState { it.filter == SearchFilter.TOOLS }.content shouldBe SearchContent.NoResults
            }
        }

    @Test
    fun `a failing event source still finds settings and tools`(): Unit =
        runTest {
            val sources = FakeSearchSources(PERSIAN_FA, failing = true)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                viewModel.onAction(SearchAction.ChangeQuery("نماز"))
                awaitResults().sections.map { it.group } shouldBe listOf(SearchGroup.SETTINGS)
                sources.calls shouldContainExactly listOf("نماز")
            }
        }

    @Test
    fun `opening results navigates and records the query`(): Unit =
        runTest {
            val viewModel = viewModel(FakeSearchSources(PERSIAN_FA))
            viewModel.uiState.test {
                viewModel.onAction(SearchAction.ChangeQuery(" نور "))
                val nowruz =
                    awaitResults()
                        .sections
                        .first()
                        .results
                        .first()
                viewModel.effects.test {
                    viewModel.onAction(SearchAction.OpenResult(nowruz))
                    awaitItem() shouldBe SearchEffect.Navigate(nowruz.target)
                    viewModel.onAction(SearchAction.OpenDate(DateSuggestion(TODAY, "")))
                    awaitItem() shouldBe SearchEffect.Navigate(SearchTarget.Day(TODAY))
                }
                awaitState { it.recent.isNotEmpty() }.recent shouldContainExactly listOf("نور")
                viewModel.onAction(SearchAction.ClearQuery)
                awaitState { it.query.isEmpty() }.content shouldBe SearchContent.Idle
                viewModel.onAction(SearchAction.UseRecent("نور"))
                awaitState { it.query == "نور" }
                viewModel.onAction(SearchAction.ClearRecent)
                awaitState { it.recent.isEmpty() }.recent shouldBe emptyList()
            }
        }

    @Test
    fun `a device zone change runs the search again`(): Unit =
        runTest {
            val sources = FakeSearchSources(PERSIAN_FA.copy(timeZoneId = "Asia/Tehran"))
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                viewModel.onAction(SearchAction.ChangeQuery("Nowruz"))
                awaitResults()
                sources.calls shouldContainExactly listOf("Nowruz")

                sources.settingsFlow.value = PERSIAN_FA.copy(timeZoneId = "Asia/Tokyo")
                // Same results, so the state may not change; the source is asked again.
                testScheduler.advanceUntilIdle()
                sources.calls shouldContainExactly listOf("Nowruz", "Nowruz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a language change rewrites the result days`(): Unit =
        runTest {
            val sources = FakeSearchSources(PERSIAN_FA)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                viewModel.onAction(SearchAction.ChangeQuery("Nowruz"))
                awaitResults()
                    .sections
                    .single()
                    .results
                    .single()
                    .dayLabel shouldBe "شنبه ۱ فروردین ۱۴۰۵"
                sources.settingsFlow.value = GREGORIAN_EN
                val english = awaitState { it.dayLabel() == "Saturday, March 21, 2026" }.content
                english.shouldBeInstanceOf<SearchContent.Results>()
            }
        }
}
