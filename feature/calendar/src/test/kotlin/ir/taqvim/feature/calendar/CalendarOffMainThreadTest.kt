/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * BUG-2: the calendar's state must be built on the calculation dispatcher, never on the main thread. Converting a day
 * into the chosen calendars computes the first month of an Islamic block from the ephemeris (ADR-0027, ADR-0028),
 * which costs tens to hundreds of milliseconds and used to run in `viewModelScope`, i.e. on the main thread.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CalendarOffMainThreadTest {
    private val today = Jdn(2461141)

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    /** Counts what the view model hands to its calculation dispatcher. */
    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        val dispatches = AtomicInteger()

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches.incrementAndGet()
            delegate.dispatch(context, block)
        }

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    }

    @Test
    fun `the shown state and the month arithmetic run on the calculation dispatcher`(): Unit =
        runTest {
            val counting = CountingDispatcher(StandardTestDispatcher(testScheduler))
            val days = FakeDaySource()
            val viewModel =
                CalendarViewModel(
                    // Persian first with the Islamic calendar second: the conversion that must stay off the main thread.
                    FakeSettingsSource(
                        PERSIAN_FIRST.copy(
                            calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC),
                            islamicVariant = IslamicVariant.CALCULATED_OBSERVATIONAL,
                        ),
                    ),
                    FakeTodaySource(today),
                    days,
                    days,
                    SearchEventsUseCase(FakeSearchSource(emptyMap()), UnconfinedTestDispatcher(testScheduler)),
                    FakePlaceSource(null),
                    FakeNowSource(TEST_NOW),
                    FakeDisplayStore(),
                    counting,
                )
            viewModel.uiState.test {
                while (awaitItem().content == null) { /* wait for the first built state */ }
                val afterFirstState = counting.dispatches.get()
                check(afterFirstState > 0) { "the state was built without the calculation dispatcher" }

                viewModel.onAction(CalendarAction.ShowNextMonth)
                while (awaitItem().content?.monthOffset != 1) { /* wait for the next month */ }
                check(counting.dispatches.get() > afterFirstState) { "the month arithmetic ran on the main thread" }
                cancelAndIgnoreRemainingEvents()
            }
            (counting.dispatches.get() > 0) shouldBe true
        }
}
