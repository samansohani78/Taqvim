/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
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

/** T-802: the Calendars and Times tabs follow the selection, the place and the clock; event sources and citations. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayDetailsViewModelTest {
    /** 27 Esfand 1404. */
    private val today = gregorian(2026, 3, 18)

    private val place = FakePlaceSource(null)

    /** 03:30 in Tehran, before Fajr. */
    private val now = FakeNowSource(Instant.parse("2026-03-18T00:00:00Z"))
    private val days = FakeDaySource()

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
            FakeSettingsSource(PERSIAN_FIRST),
            FakeTodaySource(today),
            days,
            days,
            SearchEventsUseCase(FakeSearchSource(emptyMap()), dispatcher),
            place,
            now,
            dispatcher,
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

    private fun CalendarContent.readyTimes(): DayTimes? =
        when (val state = times) {
            is DayTimesState.Ready -> state.times
            DayTimesState.Loading, DayTimesState.NoPlace -> null
        }

    @Test
    fun `the Calendars tab follows the selected day and the Times tab the place and the clock`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                val first = awaitContent { it.overview != null && it.times == DayTimesState.NoPlace }
                first.overview?.day shouldBe today
                first.overview?.daysFromToday shouldBe 0

                place.state.value = TEHRAN
                val ready = awaitContent { it.times is DayTimesState.Ready }
                val times = ready.times.shouldBeInstanceOf<DayTimesState.Ready>().times
                times.placeName shouldBe "Tehran"
                times.next shouldBe PrayerTimeKind.FAJR

                // 13:30 in Tehran: after noon, before Asr.
                now.state.value = Instant.parse("2026-03-18T10:00:00Z")
                awaitContent { it.readyTimes()?.next == PrayerTimeKind.ASR }

                viewModel.onAction(CalendarAction.SelectDay(today + 3))
                val later = awaitContent { it.overview?.day == today + 3 && it.readyTimes()?.day == today + 3 }
                later.overview?.daysFromToday shouldBe 3
                later.readyTimes()?.next.shouldBeNull()

                place.state.value = null
                awaitContent { it.times == DayTimesState.NoPlace }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `official event sources open, close and clear with a new selection`(): Unit =
        runTest {
            val viewModel = viewModel()
            val event = FakeDaySource.eventOn(today)
            viewModel.uiState.test {
                awaitContent { it.dayDetails != null }

                viewModel.onAction(CalendarAction.ShowEventSource(event))
                awaitContent { it.sourceEvent == event }
                viewModel.onAction(CalendarAction.DismissEventSource)
                awaitContent { it.sourceEvent == null }

                viewModel.onAction(CalendarAction.ShowEventSource(event))
                awaitContent { it.sourceEvent == event }
                viewModel.onAction(CalendarAction.SelectDay(today + 1))
                awaitContent { it.selectedDay == today + 1 && it.sourceEvent == null }

                viewModel.onAction(CalendarAction.SelectTab(DayDetailsTab.TIMES))
                viewModel.onAction(CalendarAction.GoToToday)
                val back = awaitContent { it.selectedDay == today }
                back.selectedTab shouldBe DayDetailsTab.TIMES
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a citation opens its source`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.OpenCitation(SOURCE_URL))
                awaitItem() shouldBe CalendarEffect.OpenUrl(SOURCE_URL)
            }
        }

    @Test
    fun `the minute source emits now and then at every minute boundary`(): Unit =
        runTest {
            val start = Instant.parse("2026-03-18T08:30:30Z")
            val clock =
                object : Clock {
                    override fun now(): Instant = start + testScheduler.currentTime.milliseconds
                }
            MinuteNowSource(clock).now().test {
                awaitItem() shouldBe start
                awaitItem() shouldBe Instant.parse("2026-03-18T08:31:00Z")
                awaitItem() shouldBe Instant.parse("2026-03-18T08:32:00Z")
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val SOURCE_URL = "https://calendar.ut.ac.ir/Fa/"
    }
}
