/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.ui.component.DateSelection
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AstronomyViewModelTest {
    private val tehran = AstronomyFixtures.tehran()
    private val start = AstronomyFixtures.at("2026-06-21T12:00", tehran)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A clock following the test scheduler's virtual time from [start]. */
    private fun TestScope.clockFrom(start: Instant): Clock =
        object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }

    private fun TestScope.viewModel(settings: MutableStateFlow<AstronomySettings?>): AstronomyViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return AstronomyViewModel({ settings }, clockFrom(start), dispatcher)
    }

    /** The next state satisfying [predicate], skipping minute ticks and intermediate states. */
    private suspend fun ReceiveTurbine<AstronomyUiState>.awaitUntil(
        predicate: (AstronomyUiState) -> Boolean,
    ): AstronomyUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    private fun AstronomyUiState.sky(): AstronomyContent.Sky = content.shouldBeInstanceOf<AstronomyContent.Sky>()

    @Test
    fun `follows the settings and the clock, then steps through days and years`(): Unit =
        runTest {
            val settings = MutableStateFlow<AstronomySettings?>(null)
            val viewModel = viewModel(settings)
            viewModel.uiState.test {
                awaitItem().content shouldBe AstronomyContent.Loading
                awaitUntil { it.content == AstronomyContent.NoLocation }
                viewModel.onStepDays(1)
                settings.value = tehran
                val now = awaitUntil { it.content is AstronomyContent.Sky }.sky()
                now.isNow shouldBe true
                now.picker.initial shouldBe DateSelection(1405, 3, 31)

                viewModel.onStepDays(1)
                awaitUntil { it.sky().picker.initial == DateSelection(1405, 4, 1) }.sky().isNow shouldBe false
                viewModel.onStepYears(-1)
                awaitUntil { it.sky().picker.initial == DateSelection(1404, 4, 1) }
                viewModel.onMinuteOfDay(9 * 60 + 30)
                awaitUntil { it.sky().timeText == "09:30" }
                viewModel.onNow()
                awaitUntil { it.sky().isNow }.sky().picker.initial shouldBe DateSelection(1405, 3, 31)
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `switches modes, picks dates and opens dialogs`(): Unit =
        runTest {
            val viewModel = viewModel(MutableStateFlow(tehran))
            viewModel.uiState.test {
                awaitUntil { it.content is AstronomyContent.Sky }
                viewModel.onMode(AstronomyMode.MOON)
                awaitUntil { it.mode == AstronomyMode.MOON }

                viewModel.onPickDate()
                awaitUntil { it.pickingDate }
                viewModel.onDatePicked(DateSelection(1405, 1, 1))
                awaitUntil { !it.pickingDate && it.sky().picker.initial == DateSelection(1405, 1, 1) }
                viewModel.onPickDate()
                awaitUntil { it.pickingDate }
                viewModel.onDismissPicker()
                awaitUntil { !it.pickingDate }

                viewModel.onNow()
                viewModel.onOpenDialog(AstronomyDialogKind.PLANETARY_HOURS)
                val open = awaitUntil { it.dialog != null }
                open.dialog.shouldBeInstanceOf<AstronomyDialog.PlanetaryHours>()
                open.sky().isNow shouldBe false
                viewModel.onDismissDialog()
                awaitUntil { it.dialog == null }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `the selection stays on the days the screen offers`(): Unit =
        runTest {
            val viewModel = viewModel(MutableStateFlow(tehran))
            val years = AstronomyDays.years(PersianCalendarSystem)
            val lastDay = PersianCalendarSystem.fromJdn(AstronomyDays.LAST)
            val firstDay = PersianCalendarSystem.fromJdn(AstronomyDays.FIRST)
            viewModel.uiState.test {
                awaitUntil { it.content is AstronomyContent.Sky }
                viewModel.onDatePicked(DateSelection(years.last, 12, 1))
                awaitUntil { it.sky().picker.initial == DateSelection(years.last, 12, 1) }
                viewModel.onStepYears(1)
                awaitUntil { it.sky().picker.initial == DateSelection(lastDay.year, lastDay.month, lastDay.day) }
                viewModel.onStepDays(1)
                testScheduler.runCurrent()
                viewModel.onDatePicked(DateSelection(years.first, 1, 1))
                awaitUntil { it.sky().picker.initial == DateSelection(years.first, 1, 1) }
                viewModel.onStepDays(-400)
                awaitUntil { it.sky().picker.initial == DateSelection(firstDay.year, firstDay.month, firstDay.day) }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `moves are ignored until the settings are known`(): Unit =
        runTest {
            val settings = MutableStateFlow<AstronomySettings?>(null)
            val viewModel = viewModel(settings)
            viewModel.uiState.test {
                awaitUntil { it.content == AstronomyContent.NoLocation }
                viewModel.onStepYears(1)
                viewModel.onMinuteOfDay(5)
                viewModel.onDatePicked(DateSelection(1400, 1, 1))
                viewModel.onPickDate()
                viewModel.onOpenDialog(AstronomyDialogKind.HOROSCOPE)
                testScheduler.runCurrent()
                val state = viewModel.uiState.value
                state.content shouldBe AstronomyContent.NoLocation
                state.pickingDate shouldBe false
                state.dialog shouldBe null
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
