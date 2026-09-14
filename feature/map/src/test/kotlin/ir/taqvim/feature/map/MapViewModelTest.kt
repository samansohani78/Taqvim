/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ir.taqvim.core.model.Coordinates
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val view = ViewSize(1_000f, 800f)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.clockFrom(start: Instant): Clock =
        object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }

    private fun TestScope.viewModel(
        settings: Flow<MapSettings> = flowOf(MapFixtures.settings()),
        outline: WorldOutlineSource = WorldOutlineSource { MapFixtures.OUTLINE },
    ): MapViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return MapViewModel(
            settingsSource = { settings },
            outlineSource = outline,
            magnetic = MapFixtures.MAGNETIC,
            clock = clockFrom(MapFixtures.NOON),
            crescentObserver = MapFixtures.CRESCENT,
            computeDispatcher = dispatcher,
        ).also { model -> backgroundScope.launch { model.uiState.collect {} } }
    }

    @Test
    fun `loads the outline and computes the default layers for now`(): Unit =
        runTest {
            val model = viewModel()
            runCurrent()
            val state = model.uiState.value
            (state.outline as OutlineState.Ready).outline shouldBe MapFixtures.OUTLINE
            val time = state.time.shouldNotBeNull()
            time.timeText shouldBe "12:00"
            time.dateText shouldContain "2026"
            time.live shouldBe true
            state.overlays.illumination.shouldNotBeNull()
            state.overlays.moon.shouldBeNull()
            state.overlays.qibla.shouldNotBeEmpty()
            state.overlays.place shouldBe Equirectangular.project(MapFixtures.TEHRAN)
            state.hasPlace shouldBe true
            model.viewModelScope.cancel()
        }

    @Test
    fun `layers switch their overlays on and off`(): Unit =
        runTest {
            val model = viewModel()
            runCurrent()
            MapLayer.entries.forEach(model::onToggleLayer)
            runCurrent()
            val overlays = model.uiState.value.overlays
            overlays.illumination.shouldBeNull()
            overlays.qibla.shouldBeEmpty()
            overlays.moon.shouldNotBeNull()
            overlays.crescent.shouldNotBeNull()
            overlays.declination.shouldNotBeNull()
            overlays.directPath.shouldBeEmpty()
            model.uiState.value.layers shouldBe MapLayer.entries.toSet() - MapUiState.DEFAULT_LAYERS
            model.viewModelScope.cancel()
        }

    @Test
    fun `picking emits the location and draws the direct path`(): Unit =
        runTest {
            val model = viewModel()
            runCurrent()
            model.onToggleLayer(MapLayer.DIRECT_PATH)
            model.effects.test {
                model.onPick(ScreenPoint(500f, 400f), view)
                awaitItem() shouldBe MapEffect.LocationPicked(Coordinates(0.0, 0.0))
                model.onPickCenter()
                awaitItem() shouldBe MapEffect.LocationPicked(Coordinates(0.0, 0.0))
            }
            model.onPick(ScreenPoint(1f, 1f), ViewSize(0f, 0f))
            runCurrent()
            val state = model.uiState.value
            state.picked.shouldNotBeNull().text shouldBe "0.00°, 0.00°"
            state.overlays.directPath.shouldNotBeEmpty()
            model.viewModelScope.cancel()
        }

    @Test
    fun `zoom and pan stay within the map`(): Unit =
        runTest {
            val model = viewModel()
            model.onResize(view)
            model.onZoom(100.0, ScreenPoint(0f, 0f), view)
            model.onPan(-1_000_000f, 1_000_000f, view)
            runCurrent()
            val viewport = model.uiState.value.viewport
            viewport.zoom shouldBe MapViewport.MAX_ZOOM
            viewport.centerX shouldBe (1 - 1 / 16.0)
            viewport.centerY shouldBe view.height / (view.width * MapViewport.MAX_ZOOM)
            model.viewModelScope.cancel()
        }

    @Test
    fun `the time can be moved and follows the clock again`(): Unit =
        runTest {
            val model = viewModel(settings = flowOf(MapFixtures.settings("fa")))
            runCurrent()
            model.uiState.value.time
                ?.timeText shouldBe "۱۲:۰۰"
            model.onSelectMinute(90)
            runCurrent()
            model.uiState.value.time?.let {
                it.timeText shouldBe "۰۱:۳۰"
                it.minuteOfDay shouldBe 90
                it.live shouldBe false
            }
            val date =
                model.uiState.value.time
                    ?.dateText
            model.onStepDay(1)
            runCurrent()
            (
                model.uiState.value.time
                    ?.dateText == date
            ) shouldBe false
            model.onNow()
            runCurrent()
            model.uiState.value.time
                ?.live shouldBe true
            model.onPickCenter()
            runCurrent()
            model.uiState.value.picked
                ?.text shouldBe "۰٫۰۰°، ۰٫۰۰°"
            model.viewModelScope.cancel()
        }

    @Test
    fun `no place and a missing outline are reported`(): Unit =
        runTest {
            val model =
                viewModel(
                    settings = flowOf(MapFixtures.settings(place = null)),
                    outline = WorldOutlineSource { error("asset missing") },
                )
            runCurrent()
            val state = model.uiState.value
            state.outline shouldBe OutlineState.Unavailable
            state.hasPlace shouldBe false
            state.overlays.qibla.shouldBeEmpty()
            state.overlays.place.shouldBeNull()
            model.viewModelScope.cancel()
        }
}
