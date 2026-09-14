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
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.ui.component.DateSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1103 entry: the Astronomy screen opens a dialog for a day, once the place is known, and only once. */
@OptIn(ExperimentalCoroutinesApi::class)
class AstronomyEntryTest {
    private val tehran = AstronomyFixtures.tehran()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ReceiveTurbine<AstronomyUiState>.awaitUntil(
        predicate: (AstronomyUiState) -> Boolean,
    ): AstronomyUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    @Test
    fun `planetary hours of Nowruz open when the place arrives and stay closed after dismissal`(): Unit =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val settings = MutableStateFlow<AstronomySettings?>(null)
            val nowruz = LocalDate(2026, 3, 21).toJdn()
            val viewModel =
                AstronomyViewModel(
                    { settings },
                    FakeClock(AstronomyFixtures.at("2026-06-21T12:00", tehran)),
                    dispatcher,
                    AstronomyEntry(AstronomyDialogKind.PLANETARY_HOURS, nowruz),
                )
            viewModel.uiState.test {
                awaitUntil { it.content == AstronomyContent.NoLocation }.dialog shouldBe null
                settings.value = tehran
                val opened = awaitUntil { it.dialog != null }
                opened.dialog.shouldBeInstanceOf<AstronomyDialog.PlanetaryHours>()
                val sky = opened.content.shouldBeInstanceOf<AstronomyContent.Sky>()
                sky.picker.initial shouldBe DateSelection(1405, 1, 1)
                sky.isNow shouldBe false

                viewModel.onDismissDialog()
                awaitUntil { it.dialog == null }
                settings.value = AstronomyFixtures.tehran(languageCode = "fa")
                testScheduler.runCurrent()
                viewModel.uiState.value.dialog shouldBe null
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
