/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(sources: FakeAgendaSources): AgendaViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        return AgendaViewModel(sources, sources, sources, dispatcher)
    }

    /** The next loaded content matching [predicate], skipping intermediate states. */
    private suspend fun ReceiveTurbine<AgendaUiState>.awaitContent(
        predicate: (AgendaContent) -> Boolean = { !it.isLoading },
    ): AgendaContent {
        while (true) {
            val content = awaitItem().content
            if (content != null && predicate(content)) return content
        }
    }

    private fun AgendaContent.monthOffsets(): List<Int> = items.filterIsInstance<AgendaMonthHeader>().map { it.offset }

    @Test
    fun `loads the months around today with today in the list`(): Unit =
        runTest {
            val sources = FakeAgendaSources(GREGORIAN_EN)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                awaitItem().content shouldBe null
                val content = awaitContent()
                content.monthOffsets() shouldContainExactly listOf(-1, 0, 1, 2)
                (content.items[content.todayIndex] as AgendaDayRow).jdn shouldBe TODAY
                content.mode shouldBe AgendaMode.AGENDA
                content.canLoadEarlier.shouldBeTrue()
                content.canLoadLater.shouldBeTrue()
                content.isRightToLeft.shouldBeFalse()
                content.localeTag shouldBe "en-US"
                sources.requested.single().start shouldBe gregorian(2026, 8, 1)
                sources.requested.single().endInclusive shouldBe gregorian(2026, 11, 30)
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `loads later months once per load and ignores requests while loading`(): Unit =
        runTest {
            val sources = FakeAgendaSources(GREGORIAN_EN)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(AgendaAction.LoadLater)
                viewModel.onAction(AgendaAction.LoadLater)
                awaitContent { it.monthOffsets().last() == 5 && !it.isLoading }.monthOffsets().first() shouldBe -1
                sources.requested shouldHaveSize 2
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `earlier months slide the window and going to today brings today back`(): Unit =
        runTest {
            val sources = FakeAgendaSources(PERSIAN_FA)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                var far = awaitContent()
                repeat(10) { step ->
                    viewModel.onAction(AgendaAction.LoadEarlier)
                    far = awaitContent { !it.isLoading && it.monthOffsets().first() == -1 - 3 * (step + 1) }
                }
                far.monthOffsets().first() shouldBe -31
                far.monthOffsets().last() shouldBe -8
                far.todayIndex shouldBe -1
                far.scrollToTodayRequest shouldBe 0

                viewModel.onAction(AgendaAction.GoToToday)
                val back = awaitContent { !it.isLoading && it.todayIndex >= 0 }
                back.monthOffsets() shouldContainExactly listOf(-1, 0, 1, 2)
                back.scrollToTodayRequest shouldBe 1
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `switching to all days shows every day and scrolls to today`(): Unit =
        runTest {
            val sources = FakeAgendaSources(GREGORIAN_EN)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                awaitContent()
                viewModel.onAction(AgendaAction.SelectMode(AgendaMode.MONTH_LIST))
                val all = awaitContent { it.mode == AgendaMode.MONTH_LIST }
                all.items.filterIsInstance<AgendaDayRow>() shouldHaveSize 31 + 30 + 31 + 30
                all.scrollToTodayRequest shouldBe 1
                viewModel.onAction(AgendaAction.SelectMode(AgendaMode.MONTH_LIST))
                viewModel.onAction(AgendaAction.GoToToday)
                awaitContent { it.scrollToTodayRequest == 2 }.mode shouldBe AgendaMode.MONTH_LIST
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `event and settings changes update the list`(): Unit =
        runTest {
            val sources = FakeAgendaSources(GREGORIAN_EN)
            val viewModel = viewModel(sources)
            viewModel.uiState.test {
                awaitContent()
                sources.eventsState.value = emptyMap()
                awaitContent { content -> content.items.filterIsInstance<AgendaDayRow>().size == 1 }
                sources.settingsState.value = PERSIAN_FA
                val persian = awaitContent { it.isRightToLeft }
                (persian.items[persian.todayIndex] as AgendaDayRow).dayNumber shouldBe "۲۲"
                sources.todayState.value = TODAY + 1
                awaitContent { (it.items[it.todayIndex] as AgendaDayRow).jdn == TODAY + 1 }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `opening a day or an event navigates`(): Unit =
        runTest {
            val viewModel = viewModel(FakeAgendaSources())
            val birthday = SAMPLE_EVENTS.getValue(TODAY + 2).last()
            viewModel.effects.test {
                viewModel.onAction(AgendaAction.OpenDay(TODAY))
                awaitItem() shouldBe AgendaEffect.NavigateToDay(TODAY)
                viewModel.onAction(AgendaAction.OpenEvent(birthday))
                awaitItem() shouldBe AgendaEffect.NavigateToEvent(birthday)
            }
            viewModel.viewModelScope.cancel()
        }
}
