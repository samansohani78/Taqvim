/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ui.component.DateSelection
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

private const val HALF_YEAR_DAYS = 182
private const val NOWRUZ_YEAR = 1405

/** Longer than the ViewModel's `WhileSubscribed` timeout, so the state flow really stops between the two screens. */
private const val RESUBSCRIBE_DELAY_MILLIS = 10_000L

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
    fun `planning moves the Sun and Moon without touching the live heading`(): Unit =
        runTest {
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val viewModel = viewModel(sensor)
            viewModel.uiState.test {
                awaitItem().content shouldBe CompassContent.Loading
                sensor.emit(CompassFixtures.reading(90.0))
                val (live, liveDial) = awaitDial { _, dial -> dial.sun != null }

                // Live is the default and nothing about it changed.
                live.planner.planned.shouldBeNull()
                val liveSun = liveDial.sun.shouldNotBeNull().azimuthDegrees

                // Planning a day six months away must move the Sun; the solstice-to-solstice swing is large.
                val winter = CompassFixtures.NOON.toJdn(TimeZone.UTC) + HALF_YEAR_DAYS
                viewModel.onDatePicked(winter)
                // The planner state and the sky are separate flows, so wait for the state where both have caught up:
                // planned, and the Sun actually moved. If planning did not reach the sky this never arrives.
                val (planned, plannedDial) =
                    awaitDial { state, dial ->
                        state.planner.planned != null && dial.sun?.azimuthDegrees != liveSun
                    }
                val plannedSun = plannedDial.sun.shouldNotBeNull().azimuthDegrees
                plannedSun shouldNotBe liveSun
                planned.planner.planned
                    .shouldNotBeNull()
                    .timeText
                    .shouldNotBeEmpty()

                // The heading is still the sensors': a new sample moves the needle while planned.
                // The filter smooths, so one sample moves the needle by about a degree, not to the raw value — the
                // assertion is that it moved at all while planned, not that it jumped.
                val plannedHeading = plannedDial.headingDegrees
                sensor.emit(CompassFixtures.reading(120.0))
                val (stillPlanned, moved) =
                    awaitDial { _, dial -> dial.headingDegrees > plannedHeading + 0.5f }
                stillPlanned.planner.planned.shouldNotBeNull()
                moved.headingDegrees shouldBeGreaterThan plannedDial.headingDegrees

                // Now returns to live, and the Sun goes back to where the clock says it is.
                viewModel.onResumeLive()
                // Back to live the Sun leaves the planned instant. It is not compared with the Sun captured at the
                // start: live keeps moving with the clock, so that value is already stale by now — a mistake the
                // first draft of this test made.
                val (back, backDial) =
                    awaitDial { state, dial ->
                        state.planner.planned == null && dial.sun?.azimuthDegrees != plannedSun
                    }
                back.planner.planned.shouldBeNull()
                backDial.sun.shouldNotBeNull().azimuthDegrees shouldNotBe plannedSun
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a date picked in the user's calendar plans that day, not the Gregorian one`(): Unit =
        runTest {
            // The picker hands back a year, month and day of whatever calendar it was built from. Reading them as
            // Gregorian would plan 1 January 2026 for a Persian user who picked 1 Farvardin 1405.
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val persian = CompassFixtures.settings(languageCode = "fa", calendar = PersianCalendarSystem)
            val viewModel = viewModel(sensor, flowOf(persian))
            viewModel.uiState.test {
                awaitItem()
                sensor.emit(CompassFixtures.reading(90.0))
                awaitDial { _, dial -> dial.sun != null }

                viewModel.onDateSelected(DateSelection(NOWRUZ_YEAR, 1, 1))
                val planned = awaitDial { state, _ -> state.planner.planned != null }.first

                val nowruz = PersianCalendarSystem.toJdn(PersianCalendarSystem.date(NOWRUZ_YEAR, 1, 1))
                GregorianCalendarSystem.fromJdn(nowruz).month shouldBe 3
                planned.planner.planned
                    .shouldNotBeNull()
                    .dateText
                    .shouldContain(Numerals.format(NOWRUZ_YEAR.toLong(), persian.language.numerals))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the planned moment survives the screen going away and coming back`(): Unit =
        runTest {
            // The ViewModel outlives the composition, so a planned moment must still be there when the screen is
            // collected again - past the WhileSubscribed timeout, which is what stopping and restarting really means.
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val viewModel = viewModel(sensor)
            var chosen: PlannedMoment? = null
            viewModel.uiState.test {
                awaitItem()
                sensor.emit(CompassFixtures.reading(90.0))
                awaitDial { _, dial -> dial.sun != null }
                viewModel.onDatePicked(CompassFixtures.NOON.toJdn(TimeZone.UTC) + 3)
                val planned = awaitDial { state, _ -> state.planner.planned != null }.first
                chosen = planned.planner.planned.shouldNotBeNull()
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(RESUBSCRIBE_DELAY_MILLIS)
            viewModel.uiState.test {
                val resumed = awaitDial { state, _ -> state.planner.planned != null }.first
                val again = resumed.planner.planned.shouldNotBeNull()
                again shouldBe chosen.shouldNotBeNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the planned time survives the picker opening and closing`(): Unit =
        runTest {
            val sensor = MutableSharedFlow<OrientationSample>(replay = 1)
            val viewModel = viewModel(sensor)
            viewModel.uiState.test {
                awaitItem()
                sensor.emit(CompassFixtures.reading(90.0))
                awaitDial { _, dial -> dial.sun != null }

                val day = CompassFixtures.NOON.toJdn(TimeZone.UTC) + 3
                viewModel.onDatePicked(day)
                val planned = awaitDial { state, _ -> state.planner.planned != null }.first
                val chosen =
                    planned.planner.planned
                        .shouldNotBeNull()
                        .dateText

                // The state lives in the ViewModel, so reopening and dismissing the picker keeps the choice; this is
                // also what makes it survive a configuration change, which shares the same ViewModel instance.
                viewModel.onOpenDatePicker()
                awaitDial { state, _ -> state.planner.pickerOpen }
                viewModel.onDismissDatePicker()
                val after = awaitDial { state, _ -> !state.planner.pickerOpen }.first
                after.planner.planned
                    .shouldNotBeNull()
                    .dateText shouldBe chosen
                cancelAndIgnoreRemainingEvents()
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
