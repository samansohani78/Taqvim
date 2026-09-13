/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimesViewModelTest {
    private val tehran = TimesFixtures.tehran()
    private val labels =
        ReportLabels({ place, _, _ -> "Report $place" }, "Date", emptyMap(), "-", "day", "night")

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A clock following the test scheduler's virtual time from [start]. */
    private fun TestScope.clockFrom(start: Instant): Clock =
        object : Clock {
            override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }

    private fun TestScope.viewModel(
        settings: MutableStateFlow<TimesSettings?>,
        start: Instant = TimesFixtures.at("2026-06-21T12:00", tehran),
    ): TimesViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return TimesViewModel({ settings }, clockFrom(start))
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<TimesUiState>.awaitDay(): TimesContent.Day =
        awaitItem().content.shouldBeInstanceOf<TimesContent.Day>()

    @Test
    fun `loads the day, refreshes the countdown every minute and expands`(): Unit =
        runTest {
            val viewModel = viewModel(MutableStateFlow(tehran))
            viewModel.uiState.test {
                awaitItem().content shouldBe TimesContent.Loading
                val first = awaitDay()
                first.next.shouldNotBeNull().kind shouldBe PrayerKind.DHUHR
                val second = awaitDay()
                (second.next.shouldNotBeNull().remaining == first.next.remaining) shouldBe false
                testScheduler.currentTime shouldBe 60_000L

                viewModel.onToggleExpanded()
                awaitItem().expanded shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `moves between days and back to today`(): Unit =
        runTest {
            val viewModel = viewModel(MutableStateFlow(tehran))
            viewModel.uiState.test {
                awaitItem()
                awaitDay().isToday shouldBe true
                viewModel.onNextDay()
                val tomorrow = awaitDay()
                tomorrow.isToday shouldBe false
                tomorrow.next.shouldBeNull()
                viewModel.onPreviousDay()
                viewModel.onPreviousDay()
                awaitDay().isToday shouldBe false
                viewModel.onToday()
                awaitDay().isToday shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `follows the settings and reports the selected month`(): Unit =
        runTest {
            val settings = MutableStateFlow<TimesSettings?>(null)
            val viewModel = viewModel(settings)
            viewModel.monthlyReport().shouldBeNull()
            viewModel.uiState.test {
                awaitItem().content shouldBe TimesContent.Loading
                awaitItem().content shouldBe TimesContent.NoLocation
                viewModel.monthlyReportHtml(labels).shouldBeNull()
                settings.value = tehran
                awaitDay().placeName shouldBe "Tehran"
                viewModel
                    .monthlyReport()
                    .shouldNotBeNull()
                    .rows.size shouldBe
                    PersianCalendarSystem.monthLength(1405, 3)
                repeat(10) { viewModel.onNextDay() }
                awaitDay()
                viewModel.monthlyReport().shouldNotBeNull().month shouldBe 4
                viewModel.monthlyReportHtml(labels).shouldNotBeNull() shouldContain "Report Tehran"
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()
        }
}
