/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** T-900: shown days, modes, zoom, the drawn box, events, prayer lines and effects of the timeline view model. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {
    private val settings = FakeTimelineSettingsSource(PERSIAN_SETTINGS)
    private val clock = FakeTimelineClockSource(TimelineNow(TODAY, 9 * 60))
    private val place = FakeTimelinePlaceSource(null)
    private val days = FakeTimelineDaysSource()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): TimelineViewModel = TimelineViewModel(settings, clock, place, days)

    private suspend fun ReceiveTurbine<TimelineUiState>.awaitContent(
        predicate: (TimelineContent) -> Boolean,
    ): TimelineContent {
        while (true) {
            val content = awaitItem().content
            if (content != null && predicate(content)) return content
        }
    }

    @Test
    fun `today's week loads from the week start with its placed events`(): Unit =
        runTest {
            val holiday = allDay("h", "Holiday", isHoliday = true)
            days.events.value = mapOf(TODAY to listOf(timed("a", 540, 600), timed("b", 570, 660), holiday))

            viewModel().uiState.test {
                val content = awaitContent { it.isLoaded }

                content.mode shouldBe TimelineMode.WEEK
                content.columns.map { it.jdn } shouldContainExactly
                    (gregorian(2026, 4, 4)..gregorian(2026, 4, 10)).toList()
                content.calendar shouldBe CalendarSystem.PERSIAN
                content.zoom shouldBe 1f
                content.draft shouldBe null
                content.now shouldBe TimelineNow(TODAY, 540)
                val today = content.columns.single { it.jdn == TODAY }
                today.timed.map { Triple(it.event.id, it.column, it.columns) } shouldContainExactly
                    listOf(Triple("a", 0, 2), Triple("b", 1, 2))
                today.allDay shouldContainExactly listOf(holiday)
                days.requestedRanges.first().start shouldBe gregorian(2026, 4, 4)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `next, previous, day mode and today move the shown days`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                viewModel.onAction(TimelineAction.ShowNext)
                awaitContent { it.columns.first().jdn == gregorian(2026, 4, 11) }
                viewModel.onAction(TimelineAction.ShowMode(TimelineMode.DAY))
                val day = awaitContent { it.mode == TimelineMode.DAY }
                day.columns.map { it.jdn } shouldContainExactly listOf(TODAY + 7)
                viewModel.onAction(TimelineAction.ShowPrevious)
                awaitContent { it.columns.first().jdn == TODAY + 6 }
                viewModel.onAction(TimelineAction.GoToToday)
                awaitContent { it.columns.first().jdn == TODAY }.columns shouldHaveSize 1
                viewModel.onAction(TimelineAction.ShowMode(TimelineMode.WEEK))
                awaitContent { it.mode == TimelineMode.WEEK }.columns.first().jdn shouldBe gregorian(2026, 4, 4)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the week of a chosen day shows, and following today moves to a new week`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                viewModel.onAction(TimelineAction.ShowMode(TimelineMode.DAY))
                viewModel.onAction(TimelineAction.ShowWeekOf(gregorian(2026, 3, 24)))
                val chosen = awaitContent { it.columns.first().jdn == gregorian(2026, 3, 21) }
                chosen.mode shouldBe TimelineMode.WEEK
                viewModel.onAction(TimelineAction.GoToToday)
                awaitContent { it.columns.first().jdn == gregorian(2026, 4, 4) }
                clock.state.value = TimelineNow(TODAY + 1, 10)
                val nextWeek = awaitContent { it.columns.first().jdn == gregorian(2026, 4, 11) }
                nextWeek.now shouldBe TimelineNow(TODAY + 1, 10)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `zoom stays within its limits`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                viewModel.onAction(TimelineAction.ZoomBy(10f))
                awaitContent { it.zoom == TimelineGeometry.MAX_ZOOM }
                viewModel.onAction(TimelineAction.ZoomBy(0.001f))
                awaitContent { it.zoom == TimelineGeometry.MIN_ZOOM }
                viewModel.onAction(TimelineAction.ZoomBy(2.5f))
                awaitContent { it.zoom == 1.25f }
                viewModel.onAction(TimelineAction.ZoomBy(Float.NaN))
                awaitContent { it.zoom == TimelineGeometry.DEFAULT_ZOOM }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a drawn box moves, resizes and creates an event`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitContent { true }

                viewModel.onAction(TimelineAction.SetDraft(TODAY, 628, 545))
                awaitContent { it.draft == TimelineDraft(TODAY, 540, 630) }
                viewModel.onAction(TimelineAction.MoveDraft(2))
                awaitContent { it.draft == TimelineDraft(TODAY, 570, 660) }
                viewModel.onAction(TimelineAction.ResizeDraft(-1))
                awaitContent { it.draft == TimelineDraft(TODAY, 570, 645) }
                viewModel.onAction(TimelineAction.ResizeDraft(-10))
                awaitContent { it.draft == TimelineDraft(TODAY, 570, 585) }
                viewModel.onAction(TimelineAction.ConfirmDraft)
                awaitContent { it.draft == null }
                viewModel.onAction(TimelineAction.SetDraft(TODAY, 60, 90))
                awaitContent { it.draft != null }
                viewModel.onAction(TimelineAction.ShowNext)
                awaitContent { it.draft == null && it.columns.first().jdn == gregorian(2026, 4, 11) }
                viewModel.onAction(TimelineAction.SetDraft(TODAY, 60, 90))
                awaitContent { it.draft != null }
                viewModel.onAction(TimelineAction.CancelDraft)
                awaitContent { it.draft == null }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.effects.test {
                awaitItem() shouldBe TimelineEffect.CreateEvent(TODAY, 570, 585)
                viewModel.onAction(TimelineAction.ConfirmDraft)
                expectNoEvents()
            }
        }

    @Test
    fun `event and place changes update the columns`(): Unit =
        runTest {
            viewModel().uiState.test {
                awaitContent { it.isLoaded }.columns.forEach { it.prayerLines.shouldBeEmpty() }

                place.state.value = TEHRAN
                awaitContent { content -> content.columns.all { it.prayerLines.size == PrayerLineKind.entries.size } }
                days.events.value = mapOf(TODAY to listOf(timed("late", 1200, 1260)))
                awaitContent { content -> content.columns.any { column -> column.timed.any { it.event.id == "late" } } }
                place.state.value = null
                awaitContent { content -> content.columns.all { it.prayerLines.isEmpty() } }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `moves before today is known are ignored and events open through an effect`(): Unit =
        runTest {
            clock.state.value = null
            val viewModel = viewModel()

            viewModel.effects.test {
                viewModel.onAction(TimelineAction.ShowNext)
                viewModel.onAction(TimelineAction.OpenEvent("x", TimelineEventKind.DEVICE))
                awaitItem() shouldBe TimelineEffect.NavigateToEvent("x", TimelineEventKind.DEVICE)
            }
            viewModel.uiState.test {
                awaitItem().content shouldBe null
                clock.state.value = TimelineNow(TODAY, 0)
                awaitContent { true }.columns.first().jdn shouldBe gregorian(2026, 4, 4)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
