/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Weekday
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

/** T-801: the months the pager needs (shown month ±1) and the week-number effect of the calendar view model. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarMonthsTest {
    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)
    private val days = FakeDaySource()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(settings: CalendarSettings = PERSIAN_FIRST): CalendarViewModel {
        val search = SearchEventsUseCase(FakeSearchSource(emptyMap()), UnconfinedTestDispatcher(testScheduler))
        return CalendarViewModel(FakeSettingsSource(settings), FakeTodaySource(today), days, days, search)
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
    fun `the shown month and its neighbours load as six weeks of days`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                val content = awaitContent { it.months.size == 3 }

                content.months.map { it.offset } shouldContainExactly listOf(-1, 0, 1)
                content.months.forEach { month ->
                    month.days.size shouldBe MonthLayout.CELLS
                    month.days
                        .first()
                        .jdn
                        .weekday() shouldBe Weekday.SATURDAY
                }
                content.months[1]
                    .days
                    .first()
                    .jdn shouldBe gregorian(2026, 3, 21)
                content.languageCode shouldBe "fa"
                content.showWeekNumbers shouldBe false

                viewModel.onAction(CalendarAction.ShowNextMonth)
                val next = awaitContent { shown -> shown.months.map { it.offset } == listOf(0, 1, 2) }
                // 1 Khordad 1405 is 2026-05-22.
                next.months[2]
                    .days
                    .first()
                    .jdn shouldBe MonthLayout.gridStart(gregorian(2026, 5, 22), Weekday.SATURDAY)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selecting a day of the shown month keeps its months, and their events update`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { it.months.size == 3 }
                val requests = days.requestedRanges.size
                val firstOfApril = gregorian(2026, 4, 1)

                viewModel.onAction(CalendarAction.SelectDay(firstOfApril))
                awaitContent { it.selectedDay == firstOfApril }.months.size shouldBe 3
                days.requestedRanges.size shouldBe requests

                days.holidays.value = setOf(firstOfApril)
                awaitContent { shown -> shown.months[1].days.any { it.jdn == firstOfApril && it.isHoliday } }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `settings choose the language and the week-number column`(): Unit =
        runTest {
            val viewModel = viewModel(PERSIAN_FIRST.copy(languageCode = "en", showWeekNumbers = true))
            viewModel.uiState.test {
                val content = awaitContent { true }
                content.languageCode shouldBe "en"
                content.showWeekNumbers shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a week number opens the timeline at its first day`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.OpenWeek(gregorian(2026, 3, 21)))
                awaitItem() shouldBe CalendarEffect.NavigateToTimeline(gregorian(2026, 3, 21))
            }
        }
}
