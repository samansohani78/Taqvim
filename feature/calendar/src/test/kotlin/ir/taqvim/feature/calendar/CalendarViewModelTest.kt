/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** T-800: selection, month paging, today, preferences, day events, search and effects of the calendar screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    /** 27 Esfand 1404. */
    private val today = gregorian(2026, 3, 18)

    /** 5 Farvardin 1405: next Persian month, same Gregorian month. */
    private val afterNowruz = gregorian(2026, 3, 25)

    private val nowruz =
        EventSearchResult("ir.holiday.nowruz-1", "Nowruz", isHoliday = true, nextDay = gregorian(2026, 3, 21))
    private val undated = EventSearchResult("undated", "Undated", isHoliday = false, nextDay = null)

    private val settings = FakeSettingsSource(PERSIAN_FIRST)
    private val todaySource = FakeTodaySource(today)
    private val days = FakeDaySource()
    private val searchSource = FakeSearchSource(mapOf("nowruz" to listOf(nowruz)))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): CalendarViewModel {
        val search = SearchEventsUseCase(searchSource, UnconfinedTestDispatcher(testScheduler))
        return CalendarViewModel(
            settings,
            todaySource,
            days,
            days,
            search,
            FakePlaceSource(null),
            FakeNowSource(TEST_NOW),
            FakeDisplayStore(),
            UnconfinedTestDispatcher(testScheduler),
        )
    }

    private suspend fun ReceiveTurbine<CalendarUiState>.awaitContent(
        predicate: (CalendarContent) -> Boolean = { true },
    ): CalendarContent {
        while (true) {
            val content = awaitItem().content
            if (content != null && predicate(content)) return content
        }
    }

    @Test
    fun `loads today in every calendar with its month and events`(): Unit =
        runTest {
            viewModel().uiState.test {
                val content = awaitContent { it.dayDetails != null }

                content.today shouldBe today
                content.selectedDay shouldBe today
                content.calendars shouldBe PERSIAN_FIRST.calendars
                content.selectedDates.take(2) shouldBe
                    listOf(
                        CalendarDate(CalendarSystem.PERSIAN, 1404, 12, 27),
                        CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 18),
                    )
                content.selectedDates[2].system shouldBe CalendarSystem.ISLAMIC
                content.monthOffset shouldBe 0
                content.visibleMonth shouldBe CalendarDate(CalendarSystem.PERSIAN, 1404, 12, 1)
                content.weekStart shouldBe Weekday.SATURDAY
                content.selectedTab shouldBe DayDetailsTab.CALENDARS
                content.dayDetails?.events shouldBe listOf(FakeDaySource.eventOn(today))
                content.search shouldBe CalendarSearch()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selecting a day shows its month and loads its events`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.SelectDay(afterNowruz))

                val content = awaitContent { it.selectedDay == afterNowruz && it.dayDetails != null }
                content.monthOffset shouldBe 1
                content.visibleMonth shouldBe CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)
                content.selectedDates.first() shouldBe CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 5)
                content.dayDetails?.jdn shouldBe afterNowruz
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `paging months keeps the selection`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.ShowNextMonth)
                viewModel.onAction(CalendarAction.ShowNextMonth)
                val twoAhead = awaitContent { it.monthOffset == 2 }
                twoAhead.visibleMonth shouldBe CalendarDate(CalendarSystem.PERSIAN, 1405, 2, 1)
                twoAhead.selectedDay shouldBe today

                viewModel.onAction(CalendarAction.ShowPreviousMonth)
                awaitContent { it.monthOffset == 1 }.visibleMonth shouldBe
                    CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)

                viewModel.onAction(CalendarAction.ShowMonth(-12))
                awaitContent { it.monthOffset == -12 }.visibleMonth shouldBe
                    CalendarDate(CalendarSystem.PERSIAN, 1403, 12, 1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `today resets selection and month and then follows the day change`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.SelectDay(afterNowruz))
                viewModel.onAction(CalendarAction.ShowNextMonth)
                awaitContent { it.monthOffset == 2 }

                viewModel.onAction(CalendarAction.GoToToday)
                val back = awaitContent { it.selectedDay == today }
                back.monthOffset shouldBe 0

                todaySource.state.value = today + 1
                awaitContent { it.today == today + 1 }.selectedDay shouldBe today + 1
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an explicit selection stays when the day changes`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.SelectDay(afterNowruz))
                awaitContent { it.monthOffset == 1 }

                todaySource.state.value = gregorian(2026, 3, 21)
                val content = awaitContent { it.today == gregorian(2026, 3, 21) }
                content.selectedDay shouldBe afterNowruz
                content.monthOffset shouldBe 0
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `preferences change the primary calendar and the pager position`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.SelectDay(afterNowruz))
                awaitContent { it.monthOffset == 1 }

                settings.state.value =
                    PERSIAN_FIRST.copy(
                        calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
                        weekStart = Weekday.MONDAY,
                    )
                val content = awaitContent { it.calendars.first() == CalendarSystem.GREGORIAN }
                content.calendars shouldBe listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN)
                content.monthOffset shouldBe 0
                content.visibleMonth shouldBe CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 1)
                content.weekStart shouldBe Weekday.MONDAY
                content.selectedDay shouldBe afterNowruz
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `day details follow changes of the day's events and tabs switch`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { it.dayDetails != null }
                days.holidays.value = setOf(today)
                awaitContent { it.dayDetails?.isHoliday == true }.dayDetails?.events shouldBe
                    listOf(FakeDaySource.eventOn(today, holiday = true))

                viewModel.onAction(CalendarAction.SelectTab(DayDetailsTab.TIMES))
                awaitContent { it.selectedTab == DayDetailsTab.TIMES }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search runs once for the latest query after the debounce`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.OpenSearch)
                awaitContent { it.search.isOpen }

                viewModel.onAction(CalendarAction.ChangeSearchQuery("now"))
                viewModel.onAction(CalendarAction.ChangeSearchQuery("nowruz"))
                awaitContent { it.search.query == "nowruz" && it.search.isSearching }
                val done = awaitContent { it.search.query == "nowruz" && !it.search.isSearching }

                done.search.results shouldBe listOf(nowruz)
                searchSource.queries shouldBe listOf("nowruz" to SearchEventsUseCase.LIMIT)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `blank queries and closing the search do not search`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.ChangeSearchQuery("   "))
                awaitContent { it.search.isOpen && !it.search.isSearching }

                viewModel.onAction(CalendarAction.ChangeSearchQuery("nowruz"))
                viewModel.onAction(CalendarAction.CloseSearch)
                awaitContent { it.search == CalendarSearch() }
                advanceTimeBy(1.seconds)

                searchSource.queries.shouldBeEmpty()
                viewModel.uiState.value.content
                    .shouldNotBeNull()
                    .search shouldBe CalendarSearch()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `opening a result jumps to its next day on the events tab`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.ChangeSearchQuery("nowruz"))
                awaitContent { it.search.results.isNotEmpty() }

                viewModel.onAction(CalendarAction.OpenSearchResult(nowruz))
                val content = awaitContent { it.selectedDay == nowruz.nextDay }
                content.search shouldBe CalendarSearch()
                content.selectedTab shouldBe DayDetailsTab.EVENTS
                content.visibleMonth shouldBe CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `effects navigate and report results without a date`(): Unit =
        runTest {
            val viewModel = viewModel()
            val event = FakeDaySource.eventOn(today)
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.OpenSearchResult(undated))
                awaitItem() shouldBe CalendarEffect.ShowSnackbar(CalendarMessage.NO_UPCOMING_OCCURRENCE)

                viewModel.onAction(CalendarAction.CreateEvent(afterNowruz))
                awaitItem() shouldBe CalendarEffect.NavigateToEventEditor(afterNowruz)

                viewModel.onAction(CalendarAction.OpenEvent(event))
                awaitItem() shouldBe CalendarEffect.NavigateToEvent(event)
            }
        }

    @Test
    fun `month paging is ignored until today is known`(): Unit =
        runTest {
            todaySource.state.value = null
            val viewModel = viewModel()
            viewModel.onAction(CalendarAction.ShowNextMonth)
            viewModel.uiState.test {
                todaySource.state.value = today
                awaitContent().monthOffset shouldBe 0
                cancelAndIgnoreRemainingEvents()
            }
        }
}
