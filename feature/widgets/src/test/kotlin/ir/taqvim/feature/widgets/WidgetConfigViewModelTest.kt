/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import app.cash.turbine.test
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.FakeClock
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigViewModelTest {
    private val kind = WidgetKind.DAY_SUMMARY_2X2
    private val store = FakeWidgetConfigStore(mapOf(8 to WidgetConfig(transparencyPercent = 33, scalePercent = 150)))
    private val installed = FakeInstalledWidgets(mapOf(kind to setOf(8, 9)))
    private val updater = RecordingUpdater()
    private val calendars = FakeCalendars(listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: Int = 8) =
        WidgetConfigViewModel(
            id,
            kind,
            store,
            calendars,
            WidgetSamples.refresher(installed, updater, configs = store, clock = FakeClock()),
        )

    @Test
    fun `the stored configuration is shown normalized with the offered calendars`(): Unit =
        runTest {
            viewModel().uiState.test {
                val state = expectMostRecentItem()
                state.loading.shouldBeFalse()
                state.config.transparencyPercent shouldBe 30
                state.config.scalePercent shouldBe 150
                state.calendars shouldBe listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN)
                calendars.flow.value = listOf(CalendarSystem.ISLAMIC)
                awaitItem().calendars shouldBe listOf(CalendarSystem.ISLAMIC)
            }
            viewModel(id = 9).uiState.test {
                expectMostRecentItem().config shouldBe WidgetConfig.defaultFor(kind)
            }
        }

    @Test
    fun `edits stay in range and saving stores them and redraws only this widget`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                expectMostRecentItem()
                viewModel.onBackground(WidgetBackground.BLACK)
                viewModel.onTransparency(95)
                viewModel.onScale(110)
                viewModel.onContent(WidgetContent.EVENTS, shown = false)
                viewModel.onContent(WidgetContent.EVENTS, shown = true)
                viewModel.onContent(WidgetContent.NEXT_PRAYER, shown = false)
                viewModel.onSecondaryCalendar(CalendarSystem.GREGORIAN)
                val edited = expectMostRecentItem().config
                edited shouldBe
                    WidgetConfig(
                        background = WidgetBackground.BLACK,
                        transparencyPercent = 90,
                        scalePercent = 100,
                        contents = (kind.contents - WidgetContent.NEXT_PRAYER).toImmutableSet(),
                        secondaryCalendar = CalendarSystem.GREGORIAN,
                    )

                viewModel.onSave()
                expectMostRecentItem().saved.shouldBeTrue()
                store.stored[8] shouldBe edited
                updater.updates shouldBe listOf(mapOf(kind to setOf(8)))
                viewModel.onSave()
                updater.updates.size shouldBe 1
            }
        }

    /** 22 Shahrivar 1405. */
    private val countdownToday = LocalDate(2026, 9, 13).toJdn()
    private val nowruz = WidgetOccasion("Nowruz", CalendarSystem.PERSIAN, 1406, 1, 1, true, "1 Farvardin 1406")

    private fun countdownViewModel(): WidgetConfigViewModel {
        val options =
            WidgetCountdownOptions(
                countdownToday,
                requireNotNull(LanguageTable.forCode("en")),
                listOf(PersianCalendarSystem, GregorianCalendarSystem),
                listOf(nowruz),
            )
        val refresher =
            WidgetSamples.refresher(
                FakeInstalledWidgets(mapOf(WidgetKind.COUNTDOWN to setOf(3))),
                updater,
                configs = store,
                clock = FakeClock(),
            )
        val source = WidgetCountdownSource { options }
        return WidgetConfigViewModel(3, WidgetKind.COUNTDOWN, store, calendars, refresher, source)
    }

    @Test
    fun `countdown widgets start at today and edit their calendar, date, mode and title`(): Unit =
        runTest {
            val today = countdownToday
            val viewModel = countdownViewModel()
            viewModel.uiState.test {
                val start = expectMostRecentItem()
                start.config.countdown shouldBe WidgetCountdown(CalendarSystem.PERSIAN, 1405, 6, 22, today.value)
                start.countdown?.dateText shouldBe "22 Shahrivar 1405"
                viewModel.countdownDaysInMonth(1405, 1) shouldBe 31
                viewModel.countdownDaysInMonth(1404, 12) shouldBe PersianCalendarSystem.monthLength(1404, 12)

                viewModel.onCountdownCalendar(CalendarSystem.GREGORIAN)
                expectMostRecentItem().config.countdown shouldBe
                    WidgetCountdown(CalendarSystem.GREGORIAN, 2026, 9, 13, today.value)
                viewModel.onCountdownDate(2026, 12, 25)
                viewModel.onCountdownMode(CountdownMode.SINCE)
                viewModel.onCountdownRepeats(true)
                viewModel.onCountdownTitle("Holiday")
                expectMostRecentItem().config.countdown shouldBe
                    WidgetCountdown(
                        CalendarSystem.GREGORIAN,
                        2026,
                        12,
                        25,
                        today.value,
                        CountdownMode.SINCE,
                        repeatsYearly = true,
                        title = "Holiday",
                    )
            }
        }

    @Test
    fun `choosing an occasion sets its date and title and saving stores the countdown`(): Unit =
        runTest {
            val today = countdownToday
            val viewModel = countdownViewModel()
            viewModel.uiState.test {
                expectMostRecentItem()
                viewModel.onCountdownOccasion(nowruz)
                val chosen = expectMostRecentItem()
                chosen.config.countdown shouldBe
                    WidgetCountdown(
                        CalendarSystem.PERSIAN,
                        1406,
                        1,
                        1,
                        today.value,
                        CountdownMode.UNTIL,
                        repeatsYearly = true,
                        title = "Nowruz",
                    )
                chosen.countdown?.occasions shouldBe listOf(nowruz)
                viewModel.onSave()
                expectMostRecentItem().saved.shouldBeTrue()
                store.stored[3]?.countdown shouldBe chosen.config.countdown
            }
        }

    @Test
    fun `without countdown options the countdown cannot be edited`(): Unit =
        runTest {
            val failing = WidgetCountdownSource { error("events unavailable") }
            val refresher = WidgetSamples.refresher(installed, updater, configs = store, clock = FakeClock())
            listOf(null, failing).forEach { source ->
                val viewModel = WidgetConfigViewModel(5, WidgetKind.COUNTDOWN, store, calendars, refresher, source)
                viewModel.uiState.test {
                    val state = expectMostRecentItem()
                    state.loading.shouldBeFalse()
                    state.countdown.shouldBeNull()
                    state.config.countdown.shouldBeNull()
                    viewModel.onCountdownCalendar(CalendarSystem.GREGORIAN)
                    viewModel.onCountdownTitle("ignored")
                    viewModel.countdownDaysInMonth(1405, 1) shouldBe 30
                    // Edits of a missing countdown change nothing, so no new state is emitted.
                    expectNoEvents()
                    viewModel.uiState.value.config.countdown
                        .shouldBeNull()
                }
            }
        }

    @Test
    fun `a failed save is reported until the next edit`(): Unit =
        runTest {
            store.failSaves = true
            val viewModel = viewModel()
            viewModel.uiState.test {
                expectMostRecentItem()
                viewModel.onSave()
                val failed = expectMostRecentItem()
                failed.saveFailed.shouldBeTrue()
                failed.saved.shouldBeFalse()
                viewModel.onScale(125)
                expectMostRecentItem().saveFailed.shouldBeFalse()
                store.failSaves = false
                viewModel.onSave()
                expectMostRecentItem().saved.shouldBeTrue()
            }
        }
}
