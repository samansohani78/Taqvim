/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** T-1103 entry: the calendar opens on a given day (links, year view, agenda, search). */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarInitialDayTest {
    /** 27 Esfand 1404. */
    private val today = gregorian(2026, 3, 18)

    /** 4 Dey 1405. */
    private val linked = gregorian(2026, 12, 25)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(initialDay: Jdn?): CalendarViewModel {
        val days = FakeDaySource()
        return CalendarViewModel(
            FakeSettingsSource(PERSIAN_FIRST),
            FakeTodaySource(today),
            days,
            days,
            SearchEventsUseCase(FakeSearchSource(emptyMap()), UnconfinedTestDispatcher(testScheduler)),
            FakePlaceSource(null),
            FakeNowSource(TEST_NOW),
            FakeDisplayStore(),
            UnconfinedTestDispatcher(testScheduler),
            initialDay,
        )
    }

    private suspend fun ReceiveTurbine<CalendarUiState>.awaitContent(
        predicate: (CalendarContent) -> Boolean,
    ): CalendarContent {
        while (true) {
            val content = awaitItem().content
            if (content != null && predicate(content)) return content
        }
    }

    @Test
    fun `the initial day is selected with its month shown, and today is one action away`(): Unit =
        runTest {
            val viewModel = viewModel(linked)
            viewModel.uiState.test {
                val opened = awaitContent { true }
                opened.selectedDay shouldBe linked
                opened.today shouldBe today
                opened.visibleMonth.year shouldBe 1405
                opened.visibleMonth.month shouldBe 10

                viewModel.onAction(CalendarAction.GoToToday)
                awaitContent { it.selectedDay == today }.visibleMonth.month shouldBe 12
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `without an initial day the calendar opens on today`(): Unit =
        runTest {
            viewModel(null).uiState.test {
                awaitContent { true }.selectedDay shouldBe today
                cancelAndIgnoreRemainingEvents()
            }
        }
}
