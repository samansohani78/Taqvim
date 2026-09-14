/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import app.cash.turbine.test
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.FakeClock
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
