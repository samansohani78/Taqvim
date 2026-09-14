/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1103 entry and T-1400 persistence: the converter opens with text, and board edits are kept. */
@OptIn(ExperimentalCoroutinesApi::class)
class ToolsEntryTest {
    private val kept = mutableListOf<List<String>>()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ReceiveTurbine<ToolsUiState>.awaitReady(
        predicate: (ToolsUiState, ToolsContent.Ready) -> Boolean,
    ): ToolsContent.Ready {
        while (true) {
            val state = awaitItem()
            val ready = state.content as? ToolsContent.Ready
            if (ready != null && predicate(state, ready)) return ready
        }
    }

    @Test
    fun `the converter opens with the linked text and board edits reach the store`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = MutableStateFlow(ToolsFixtures.settings())
            val viewModel =
                ToolsViewModel({ settings }, FakeClock(ToolsFixtures.NOW), { kept += it }, "1 Farvardin 1405")
            viewModel.uiState.test {
                val opened = awaitReady { state, _ -> state.inputs.converter == "1 Farvardin 1405" }
                opened.converter.shouldBeInstanceOf<ConverterResult.Converted>().run {
                    isToday shouldBe false
                    dates.first().iso shouldBe "1405-01-01"
                }

                viewModel.onRemoveZone("Asia/Kabul")
                awaitReady { _, ready -> ready.board.rows.size == 2 }
                viewModel.onAddZone("Europe/Paris")
                awaitReady { _, ready -> ready.board.rows.size == 3 }
                testScheduler.runCurrent()
                kept shouldBe listOf(listOf("America/Los_Angeles"), listOf("America/Los_Angeles", "Europe/Paris"))
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `a failing store keeps the edit for the session`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val failing = ToolsBoardStore { error("disk full") }
            val viewModel =
                ToolsViewModel({ MutableStateFlow(ToolsFixtures.settings()) }, FakeClock(ToolsFixtures.NOW), failing)
            viewModel.uiState.test {
                awaitReady { _, ready -> ready.board.rows.size == 3 }
                viewModel.onRemoveZone("Asia/Kabul")
                awaitReady { _, ready -> ready.board.rows.size == 2 }
                testScheduler.runCurrent()
                viewModel.uiState.value.inputs.converter shouldBe ""
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
