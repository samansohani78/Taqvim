/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
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

/** T-803: the toolbar menu, its dialogs, going to a date, stored display choices and the entries that leave. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarMenuTest {
    /** 27 Esfand 1404. */
    private val today = gregorian(2026, 3, 18)
    private val settings = FakeSettingsSource(PERSIAN_FIRST)
    private val days = FakeDaySource()
    private val store = FakeDisplayStore(settings)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): CalendarViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return CalendarViewModel(
            settings,
            FakeTodaySource(today),
            days,
            days,
            SearchEventsUseCase(FakeSearchSource(emptyMap()), dispatcher),
            FakePlaceSource(null),
            FakeNowSource(TEST_NOW),
            store,
            dispatcher,
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

    private fun persian(
        year: Int,
        month: Int,
        day: Int,
    ) = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day))

    @Test
    fun `the menu opens, closes and hands over to its dialogs`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent().menu shouldBe CalendarMenu()
                viewModel.onAction(CalendarAction.OpenMenu)
                awaitContent { it.menu.isOpen }
                viewModel.onAction(CalendarAction.DismissMenu)
                awaitContent { !it.menu.isOpen }

                viewModel.onAction(CalendarAction.OpenMenu)
                awaitContent { it.menu.isOpen }
                viewModel.onAction(CalendarAction.OpenDatePicker)
                awaitContent { it.menu == CalendarMenu(dialog = CalendarDialog.DATE_PICKER) }
                viewModel.onAction(CalendarAction.DismissDialog)
                awaitContent { it.menu == CalendarMenu() }

                viewModel.onAction(CalendarAction.OpenSecondaryCalendarChooser)
                val chooser = awaitContent { it.menu.dialog == CalendarDialog.SECONDARY_CALENDAR }
                chooser.secondaryChoices shouldBe
                    listOf(CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN, CalendarSystem.NEPALI)

                viewModel.onAction(CalendarAction.OpenMenu)
                awaitContent { it.menu.isOpen }
                viewModel.onAction(CalendarAction.OpenShiftWork)
                awaitContent { !it.menu.isOpen }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `going to a date selects it in the primary calendar with the month and day clamped`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(CalendarAction.OpenDatePicker)
                viewModel.onAction(CalendarAction.PickDate(1405, 12, 31))

                val lastEsfand = persian(1405, 12, PersianCalendarSystem.monthLength(1405, 12))
                val content = awaitContent { it.selectedDay == lastEsfand }
                content.visibleMonth shouldBe CalendarDate(CalendarSystem.PERSIAN, 1405, 12, 1)
                content.monthOffset shouldBe 12
                content.menu shouldBe CalendarMenu()

                viewModel.onAction(CalendarAction.PickDate(1406, 13, 0))
                awaitContent { it.selectedDay == persian(1406, 12, 1) }.monthOffset shouldBe 24
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `display choices are stored and the screen follows the preferences`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent().showWeekNumbers shouldBe false
                viewModel.onAction(CalendarAction.OpenMenu)
                viewModel.onAction(CalendarAction.ShowWeekNumbers(true))
                awaitContent { it.showWeekNumbers }.menu shouldBe CalendarMenu()
                store.weekNumbers shouldBe listOf(true)

                viewModel.onAction(CalendarAction.ChooseSecondaryCalendar(CalendarSystem.ISLAMIC))
                val reordered = awaitContent { it.calendars.getOrNull(1) == CalendarSystem.ISLAMIC }
                reordered.calendars.first() shouldBe CalendarSystem.PERSIAN
                reordered.secondaryChoices shouldBe
                    listOf(CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN, CalendarSystem.NEPALI)
                store.secondaries shouldBe listOf(CalendarSystem.ISLAMIC)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a display choice that cannot be stored is reported`(): Unit =
        runTest {
            store.failure = IllegalStateException("storage unavailable")
            val viewModel = viewModel()
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.ShowWeekNumbers(true))
                awaitItem() shouldBe CalendarEffect.ShowSnackbar(CalendarMessage.SETTING_NOT_SAVED)
                viewModel.onAction(CalendarAction.ChooseSecondaryCalendar(CalendarSystem.GREGORIAN))
                awaitItem() shouldBe CalendarEffect.ShowSnackbar(CalendarMessage.SETTING_NOT_SAVED)
            }
        }

    @Test
    fun `toolbar and menu entries leave the screen`(): Unit =
        runTest {
            val viewModel = viewModel()
            val otherDay = gregorian(2026, 3, 25)
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.OpenSearchScreen)
                awaitItem() shouldBe CalendarEffect.NavigateToSearch
                viewModel.onAction(CalendarAction.OpenShiftWork)
                awaitItem() shouldBe CalendarEffect.NavigateToShiftWork
                viewModel.onAction(CalendarAction.OpenPlanetaryHours)
                awaitItem() shouldBe CalendarEffect.NavigateToPlanetaryHours(today)
                viewModel.onAction(CalendarAction.SelectDay(otherDay))
                viewModel.onAction(CalendarAction.OpenPlanetaryHours)
                awaitItem() shouldBe CalendarEffect.NavigateToPlanetaryHours(otherDay)
                viewModel.onAction(CalendarAction.PrintMonth)
                awaitItem() shouldBe CalendarEffect.PrintMonth
            }
        }
}
