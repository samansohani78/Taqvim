/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A clock following the test scheduler's virtual time from [start]. */
    private fun TestScope.clockFrom(start: Instant): Clock =
        object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }

    private fun TestScope.viewModel(
        settings: MutableStateFlow<ToolsSettings> = MutableStateFlow(ToolsFixtures.settings()),
        start: Instant = ToolsFixtures.NOW,
    ): ToolsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ToolsViewModel(
            { settings },
            clockFrom(start),
            computeDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    /** The next state that is ready and satisfies [predicate]. */
    private suspend fun ReceiveTurbine<ToolsUiState>.awaitReady(
        predicate: (ToolsUiState, ToolsContent.Ready) -> Boolean = { _, _ -> true },
    ): ToolsContent.Ready {
        var state = awaitItem()
        var ready = state.content as? ToolsContent.Ready
        while (ready == null || !predicate(state, ready)) {
            state = awaitItem()
            ready = state.content as? ToolsContent.Ready
        }
        return ready
    }

    @Test
    fun `loads today, follows typing in each tool and switches tabs`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitItem().content shouldBe ToolsContent.Loading
                awaitReady().converter.shouldBeInstanceOf<ConverterResult.Converted>().isToday shouldBe true
                viewModel.qrToShare().shouldBeNull()

                viewModel.onInputsChange(ToolsInputs(converter = "1405/6/22", duration = "1h 30m", qr = "hello"))
                val typed =
                    awaitReady { _, ready -> (ready.converter as? ConverterResult.Converted)?.isToday == false }
                typed.converter
                    .shouldBeInstanceOf<ConverterResult.Converted>()
                    .dates[2]
                    .iso shouldBe "2026-09-13"
                typed.duration shouldBe DurationState.Empty
                typed.qr shouldBe QrState.Empty

                viewModel.onSelectTab(ToolsTab.DURATION)
                val calculated =
                    awaitReady { state, ready ->
                        state.tab == ToolsTab.DURATION &&
                            ready.duration != DurationState.Empty
                    }
                calculated.duration.shouldBeInstanceOf<DurationState.Value>().totalMinutes shouldBe "90"

                viewModel.onSelectTab(ToolsTab.QR)
                awaitReady { state, ready -> state.tab == ToolsTab.QR && ready.qr is QrState.Code }
                viewModel.qrToShare().shouldNotBeNull().first shouldBe "hello"

                viewModel.onSelectTab(ToolsTab.DISTANCE)
                viewModel.onInputsChange(ToolsInputs(distanceFrom = "1405/1/1", distanceTo = "1405/1/8"))
                val measured = awaitReady { _, ready -> ready.distance.result != null }
                measured.distance.result
                    .shouldNotBeNull()
                    .days shouldBe "7"
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `zones can be searched, added and removed during the session`(): Unit =
        runTest {
            val settings = MutableStateFlow(ToolsFixtures.settings())
            val viewModel = viewModel(settings)
            viewModel.onSelectTab(ToolsTab.TIME_ZONES)
            viewModel.uiState.test {
                awaitReady { state, _ -> state.tab == ToolsTab.TIME_ZONES }.board.rows.map { it.id } shouldBe
                    listOf("Asia/Tehran", "Asia/Kabul", "America/Los_Angeles")
                viewModel.onRemoveZone("America/Los_Angeles")
                awaitReady { _, ready -> ready.board.rows.size == 2 }

                viewModel.onInputsChange(ToolsInputs(zoneQuery = "new_york"))
                val searched = awaitReady { _, ready -> ready.board.suggestions.isNotEmpty() }
                searched.board.suggestions shouldContain "America/New_York"
                viewModel.onAddZone("America/New_York")
                val added =
                    awaitReady { state, ready -> state.inputs.zoneQuery.isEmpty() && ready.board.rows.size == 3 }
                added.board.rows.map { it.id } shouldBe listOf("Asia/Tehran", "Asia/Kabul", "America/New_York")
                added.board.suggestions shouldBe emptyList()

                settings.value = ToolsFixtures.settings(boardZones = listOf("Europe/Paris"))
                val kept = awaitReady { _, ready -> ready.board.rows.size == 3 }
                kept.board.rows.map { it.id } shouldContain "America/New_York"
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `today changes at midnight`(): Unit =
        runTest {
            val viewModel = viewModel(start = Instant.parse("2026-06-21T20:29:30Z"))
            viewModel.uiState.test {
                awaitReady()
                    .converter
                    .shouldBeInstanceOf<ConverterResult.Converted>()
                    .dates[0]
                    .iso shouldBe "1405-03-31"
                awaitReady { _, ready ->
                    (ready.converter as? ConverterResult.Converted)?.dates?.first()?.iso == "1405-04-01"
                }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `the board follows the clock minute by minute`(): Unit =
        runTest {
            val board = viewModel(start = Instant.parse("2026-06-21T20:29:30Z"))
            board.onSelectTab(ToolsTab.TIME_ZONES)
            board.uiState.test {
                awaitReady { state, _ -> state.tab == ToolsTab.TIME_ZONES }.board.rows[0].time shouldBe "23:59"
                val after = awaitReady { _, ready -> ready.board.rows[0].time == "00:00" }
                after.board.rows[0].dayShift shouldBe 0
                cancelAndIgnoreRemainingEvents()
            }
            board.viewModelScope.cancel()
        }

    @Test
    fun `tools compute on the injected dispatcher and only for the selected tab`(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val compute = StandardTestDispatcher(TestCoroutineScheduler())
            val viewModel =
                ToolsViewModel(
                    { MutableStateFlow(ToolsFixtures.settings()) },
                    clockFrom(ToolsFixtures.NOW),
                    computeDispatcher = compute,
                )

            /** Lets both the main and the computation dispatcher run what is due, without moving the clock. */
            fun settle(computing: Boolean) =
                repeat(SETTLE_ROUNDS) {
                    testScheduler.runCurrent()
                    if (computing) compute.scheduler.runCurrent()
                }
            viewModel.uiState.test {
                awaitItem().content shouldBe ToolsContent.Loading
                settle(computing = false)
                // Nothing is computed while the computation dispatcher does not run.
                expectNoEvents()
                settle(computing = true)
                awaitReady().qr shouldBe QrState.Empty

                viewModel.onInputsChange(ToolsInputs(qr = "hello"))
                settle(computing = true)
                // The QR tab is not selected, so its code is not encoded.
                awaitReady { state, _ -> state.inputs.qr == "hello" }.qr shouldBe QrState.Empty

                viewModel.onSelectTab(ToolsTab.QR)
                settle(computing = true)
                awaitReady { state, _ -> state.tab == ToolsTab.QR }.qr.shouldBeInstanceOf<QrState.Code>()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    private companion object {
        const val SETTLE_ROUNDS = 4
    }
}
