/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1103 entry: search opens with a linked query and searches it. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchEntryTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a linked query is searched when the screen opens`(): Unit =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val sources = FakeSearchSources(PERSIAN_FA)
            val recent = SessionRecentQueriesStore()
            val viewModel = SearchViewModel(sources, sources, sources, sources, recent, dispatcher, "نور")
            viewModel.uiState.test {
                var state = awaitItem()
                while (state.content !is SearchContent.Results) state = awaitItem()
                state.query shouldBe "نور"
                state.content
                    .shouldBeInstanceOf<SearchContent.Results>()
                    .sections
                    .first()
                    .results
                    .first()
                    .title shouldBe "عید نوروز"
                cancelAndIgnoreRemainingEvents()
            }
        }
}
