/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** T-805: calendar and year navigation, zoom, year selection, holidays and effects of the year view model. */
@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModelTest {
    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)
    private val calendars = YearCalendars(PERSIAN_FIRST)
    private val days = FakeYearDaysSource()
    private val settings = FakeYearSettingsSource(PERSIAN_FIRST)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(todaySource: FakeYearTodaySource = FakeYearTodaySource(today)): YearViewModel =
        YearViewModel(settings, todaySource, days)

    private suspend fun ReceiveTurbine<YearUiState>.awaitContent(predicate: (YearContent) -> Boolean): YearContent {
        while (true) {
            val content = awaitItem().content
            if (content != null && predicate(content)) return content
        }
    }

    @Test
    fun `today's year loads in the first calendar with the flags of its days`(): Unit =
        runTest {
            viewModel().uiState.test {
                val content = awaitContent { it.days != null }

                content.calendars shouldContainExactly PERSIAN_FIRST.calendars
                content.calendarIndex shouldBe 0
                content.year shouldBe 1405
                content.anchorDay shouldBe today
                content.columns shouldBe YearZoom.DEFAULT_COLUMNS
                content.isPickingYear shouldBe false
                content.languageCode shouldBe "fa"
                content.days?.map { it.jdn } shouldContainExactly calendars.yearDays(0, 1405).toList()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `next, previous, chosen and today's years move the shown year`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                viewModel.onAction(YearAction.ShowNextYear)
                awaitContent { it.year == 1406 }.anchorDay shouldBe calendars.yearStart(0, 1406)
                viewModel.onAction(YearAction.ShowPreviousYear)
                viewModel.onAction(YearAction.ShowPreviousYear)
                awaitContent { it.year == 1404 }
                viewModel.onAction(YearAction.OpenYearPicker)
                awaitContent { it.isPickingYear }
                viewModel.onAction(YearAction.ShowYear(1300))
                awaitContent { it.year == 1300 }.isPickingYear shouldBe false
                viewModel.onAction(YearAction.ShowYear(YearCalendars.MAX_YEAR + 100))
                awaitContent { it.year == YearCalendars.MAX_YEAR }
                viewModel.onAction(YearAction.OpenYearPicker)
                viewModel.onAction(YearAction.CloseYearPicker)
                viewModel.onAction(YearAction.GoToToday)
                val back = awaitContent { it.year == 1405 && !it.isPickingYear }
                back.anchorDay shouldBe today
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `switching calendars shows each calendar's year of the same days`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }
                viewModel.onAction(YearAction.ShowNextYear)
                val nowruz1406 = awaitContent { it.year == 1406 }.anchorDay

                viewModel.onAction(YearAction.SelectCalendar(1))
                awaitContent { it.calendarIndex == 1 }.year shouldBe calendars.yearOf(1, nowruz1406)
                viewModel.onAction(YearAction.SelectCalendar(9))
                val islamic = awaitContent { it.calendarIndex == 2 }
                islamic.year shouldBe calendars.yearOf(2, nowruz1406)
                islamic.calendars[islamic.calendarIndex] shouldBe CalendarSystem.ISLAMIC
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `zoom stays within its levels`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                repeat(YearZoom.MAX_COLUMNS) { viewModel.onAction(YearAction.ZoomIn) }
                awaitContent { it.columns == YearZoom.MIN_COLUMNS }
                repeat(YearZoom.MAX_COLUMNS + 2) { viewModel.onAction(YearAction.ZoomOut) }
                awaitContent { it.columns == YearZoom.MAX_COLUMNS }
                viewModel.onAction(YearAction.ZoomOut)
                viewModel.onAction(YearAction.ZoomIn)
                awaitContent { it.columns == YearZoom.MAX_COLUMNS - 1 }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `holiday changes re-emit and another year loads its own days`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { it.days != null }
                val holiday = gregorian(2026, 4, 1)

                days.holidays.value = setOf(holiday)
                awaitContent { content -> content.days.orEmpty().any { it.jdn == holiday && it.isHoliday } }
                viewModel.onAction(YearAction.ShowNextYear)
                val next = awaitContent { it.year == 1406 && it.days != null }
                next.days?.first()?.jdn shouldBe calendars.yearStart(0, 1406)
                days.requestedRanges.map { it.start } shouldContainExactly
                    listOf(calendars.yearStart(0, 1405), calendars.yearStart(0, 1406))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `settings changes re-resolve the calendars and the shown calendar`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }
                viewModel.onAction(YearAction.SelectCalendar(2))
                awaitContent { it.calendarIndex == 2 }

                settings.state.value =
                    PERSIAN_FIRST.copy(calendars = listOf(CalendarSystem.GREGORIAN), languageCode = "en")
                val gregorianOnly = awaitContent { it.calendars.size == 1 }
                gregorianOnly.calendarIndex shouldBe 0
                gregorianOnly.year shouldBe 2026
                gregorianOnly.languageCode shouldBe "en"
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `year moves before today is known are ignored and a month opens through an effect`(): Unit =
        runTest {
            val todaySource = FakeYearTodaySource(null)
            val viewModel = viewModel(todaySource)
            val firstDay = Jdn(2_461_121)

            viewModel.effects.test {
                viewModel.onAction(YearAction.ShowNextYear)
                viewModel.onAction(YearAction.OpenMonth(firstDay))
                awaitItem() shouldBe YearEffect.NavigateToMonth(firstDay)
            }
            viewModel.uiState.test {
                todaySource.state.value = today
                awaitContent { true }.year shouldBe 1405
                cancelAndIgnoreRemainingEvents()
            }
        }
}
