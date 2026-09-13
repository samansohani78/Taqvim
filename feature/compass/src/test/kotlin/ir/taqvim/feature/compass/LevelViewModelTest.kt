/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.NumeralSystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
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
class LevelViewModelTest {
    private val g = 9.81
    private val tiltedTwoDegrees =
        GravitySample.Reading(Vector3(sin(2.0 * PI / 180) * g, 0.0, cos(2.0 * PI / 180) * g))

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        gravity: Flow<GravitySample>,
        store: FakeCalibrationStore,
        languageCode: String = "en",
    ): LevelViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return LevelViewModel({ flowOf(CompassFixtures.settings(languageCode)) }, FakeSensors(gravity = gravity), store)
    }

    private suspend fun ReceiveTurbine<LevelUiState>.awaitReading(
        predicate: (LevelContent.Reading) -> Boolean = { true },
    ): LevelContent.Reading {
        while (true) {
            val reading = awaitItem().content as? LevelContent.Reading
            if (reading != null && predicate(reading)) return reading
        }
    }

    @Test
    fun `calibration zeroes the current orientation and persists`(): Unit =
        runTest {
            val store = FakeCalibrationStore()
            val first = viewModel(MutableStateFlow(tiltedTwoDegrees), store)
            first.uiState.test {
                val tilted = awaitReading()
                tilted.orientation shouldBe DeviceOrientation.FLAT
                tilted.x shouldBe (2f plusOrMinus 1e-3f)
                tilted.isLevel shouldBe false
                first.onCalibrate()
                runCurrent()
                awaitReading { it.calibrated }.let {
                    it.isLevel shouldBe true
                    it.x shouldBe (0f plusOrMinus 1e-3f)
                }
                store.stored.value.offsets shouldContainKey DeviceOrientation.FLAT
                cancelAndIgnoreRemainingEvents()
            }
            first.viewModelScope.cancel()

            val second = viewModel(MutableStateFlow(tiltedTwoDegrees), store)
            second.uiState.test {
                awaitReading { it.calibrated }.isLevel shouldBe true
                second.onResetCalibration()
                runCurrent()
                awaitReading { !it.calibrated }.x shouldBe (2f plusOrMinus 1e-3f)
                store.stored.value.offsets
                    .isEmpty() shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
            second.viewModelScope.cancel()
        }

    @Test
    fun `tabs, units, digits and missing sensors`(): Unit =
        runTest {
            val viewModel = viewModel(flowOf(GravitySample.Unavailable), FakeCalibrationStore(), languageCode = "fa")
            viewModel.onCalibrate()
            viewModel.onResetCalibration()
            viewModel.uiState.test {
                var state = awaitItem()
                while (state.content != LevelContent.SensorUnavailable || state.numerals != NumeralSystem.PERSIAN) {
                    state = awaitItem()
                }
                viewModel.onSelectTab(LevelTab.RULER)
                awaitItem().tab shouldBe LevelTab.RULER
                viewModel.onSelectUnit(RulerUnit.INCHES)
                awaitItem().rulerUnit shouldBe RulerUnit.INCHES
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `free fall keeps the last orientation and portrait readings`(): Unit =
        runTest {
            val gravity = MutableStateFlow<GravitySample>(GravitySample.Reading(Vector3(0.0, 0.0, 0.0)))
            val viewModel = viewModel(gravity, FakeCalibrationStore())
            viewModel.uiState.test {
                var state = awaitItem()
                while (state.content != LevelContent.SensorUnavailable) state = awaitItem()
                gravity.value = GravitySample.Reading(Vector3(0.0, g, 0.0))
                awaitReading().orientation shouldBe DeviceOrientation.PORTRAIT
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
