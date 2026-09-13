/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompassViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.clockFrom(start: Instant): Clock =
        object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }

    private fun TestScope.viewModel(
        orientation: Flow<OrientationSample>,
        settings: Flow<CompassSettings> = flowOf(CompassFixtures.settings()),
    ): CompassViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return CompassViewModel(
            settingsSource = { settings },
            sensors = FakeSensors(orientation = orientation),
            declination = CompassFixtures.FIVE_EAST,
            clock = clockFrom(CompassFixtures.NOON),
        )
    }

    /** The next state whose content is a dial matching [predicate]. */
    private suspend fun ReceiveTurbine<CompassUiState>.awaitDial(
        predicate: (CompassUiState, CompassContent.Dial) -> Boolean = { _, _ -> true },
    ): Pair<CompassUiState, CompassContent.Dial> {
        while (true) {
            val state = awaitItem()
            val dial = state.content as? CompassContent.Dial
            if (dial != null && predicate(state, dial)) return state to dial
        }
    }

    @Test
    fun `filters the heading, applies true north and stops on request`(): Unit =
        runTest {
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val viewModel = viewModel(sensor)
            viewModel.uiState.test {
                awaitItem().content shouldBe CompassContent.Loading
                sensor.emit(CompassFixtures.reading(90.0))
                val (_, first) = awaitDial { _, dial -> dial.north == NorthReference.TRUE }
                first.headingDegrees shouldBe (95f plusOrMinus 1e-3f)
                first.qibla.shouldNotBeNull()

                sensor.emit(CompassFixtures.reading(100.0))
                val (_, filtered) = awaitDial { _, dial -> dial.headingDegrees > 95.5f }
                filtered.headingDegrees shouldBe (96.5f plusOrMinus 1e-3f)

                viewModel.onToggleFrozen()
                awaitItem().frozen shouldBe true
                sensor.emit(CompassFixtures.reading(180.0))
                runCurrent()
                expectNoEvents()

                viewModel.onToggleFrozen()
                val (resumed, moving) = awaitDial { state, dial -> !state.frozen && dial.headingDegrees > 97f }
                resumed.frozen shouldBe false
                (moving.headingDegrees < 185f) shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `accuracy, sun path toggle and magnetic north without a place`(): Unit =
        runTest {
            val viewModel =
                viewModel(
                    orientation = MutableStateFlow(CompassFixtures.reading(10.0, CompassAccuracy.LOW)),
                    settings = MutableStateFlow(CompassFixtures.settings(place = null)),
                )
            viewModel.uiState.test {
                val (_, dial) = awaitDial()
                dial.north shouldBe NorthReference.MAGNETIC
                dial.accuracy shouldBe CompassAccuracy.LOW
                dial.headingDegrees shouldBe (10f plusOrMinus 1e-3f)
                viewModel.onToggleSunPath()
                awaitDial { state, _ -> state.showSunPath }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `announcements keep their bucket until the heading clearly moves`(): Unit =
        runTest {
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val viewModel = viewModel(sensor, flowOf(CompassFixtures.settings(place = null)))
            viewModel.uiState.test {
                sensor.emit(CompassFixtures.reading(0.0))
                awaitDial().second.announcedDegrees shouldBe 0
                repeat(4) { sensor.emit(CompassFixtures.reading(9.0)) }
                awaitDial { _, dial -> dial.headingDegrees > 4f }.second.announcedDegrees shouldBe 0
                repeat(30) { sensor.emit(CompassFixtures.reading(40.0)) }
                awaitDial { _, dial -> dial.announcedDegrees == 30 || dial.announcedDegrees == 45 }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `devices without sensors`(): Unit =
        runTest {
            val viewModel = viewModel(flowOf(OrientationSample.Unavailable))
            viewModel.uiState.test {
                var state = awaitItem()
                while (state.content == CompassContent.Loading) state = awaitItem()
                state.content shouldBe CompassContent.SensorUnavailable
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
