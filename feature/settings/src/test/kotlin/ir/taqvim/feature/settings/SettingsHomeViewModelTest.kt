/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Weekday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1500: the settings home stores every change, opens pages and dialogs, and follows deep links. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsHomeViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeGeneralSettingsStore,
        initialItem: SettingsItemId? = null,
    ): SettingsHomeViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return SettingsHomeViewModel(store, initialItem)
    }

    @Test
    fun `stored settings become rows and switches are stored`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.english)
            val model = viewModel(store)
            advanceUntilIdle()

            val state = model.uiState.value
            state.loading shouldBe false
            state.rows.map { it.id } shouldBe SettingsItemId.entries
            state.rows.first { it.id == SettingsItemId.WEEK_NUMBERS }.value shouldBe RowValue.Switch(false)

            model.onRowClicked(SettingsItemId.WEEK_NUMBERS)
            advanceUntilIdle()

            store.current.showWeekNumbers shouldBe true
            model.uiState.value.rows
                .first { it.id == SettingsItemId.WEEK_NUMBERS }
                .value shouldBe RowValue.Switch(true)
        }

    @Test
    fun `a choice opens a dialog, stores the option and closes`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.english)
            val model = viewModel(store)
            advanceUntilIdle()

            model.onRowClicked(SettingsItemId.THEME)
            model.uiState.value.dialog
                ?.selected shouldBe setOf(ThemeChoice.SYSTEM.name)
            model.onOptionClicked(ThemeChoice.BLACK.name)
            advanceUntilIdle()

            store.current.theme shouldBe ThemeChoice.BLACK
            model.uiState.value.dialog shouldBe null
        }

    @Test
    fun `a multiple choice toggles options and never empties the weekend`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.persian)
            val model = viewModel(store)
            advanceUntilIdle()
            val weekend = store.current.weekend.single()

            model.onRowClicked(SettingsItemId.WEEKEND)
            model.onOptionClicked(Weekday.THURSDAY.name)
            advanceUntilIdle()
            store.current.weekend shouldBe setOf(weekend, Weekday.THURSDAY)
            model.uiState.value.dialog
                ?.selected shouldBe setOf(weekend.name, Weekday.THURSDAY.name)

            model.onOptionClicked(Weekday.THURSDAY.name)
            model.onOptionClicked(weekend.name)
            advanceUntilIdle()
            store.current.weekend shouldBe setOf(weekend)

            model.onRowClicked(SettingsItemId.EVENT_SOURCES)
            store.current.enabledEventSources.forEach { model.onOptionClicked(it.name) }
            advanceUntilIdle()
            store.current.enabledEventSources shouldBe emptySet()
            model.onDialogDismissed()
            model.uiState.value.dialog shouldBe null
        }

    @Test
    fun `links open pages and clearing searches happens only when some exist`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.english)
            val model = viewModel(store)
            advanceUntilIdle()

            model.effects.test {
                model.onRowClicked(SettingsItemId.ATHAN)
                awaitItem() shouldBe SettingsEffect(SettingsDestination.ATHAN)
                model.onRowClicked(SettingsItemId.SUBSCRIPTIONS)
                awaitItem() shouldBe SettingsEffect(SettingsDestination.SUBSCRIPTIONS)
            }
            model.onRowClicked(SettingsItemId.CLEAR_SEARCHES)
            advanceUntilIdle()
            model.onRowClicked(SettingsItemId.CLEAR_SEARCHES)
            advanceUntilIdle()

            store.clears shouldBe 1
            model.uiState.value.rows
                .first { it.id == SettingsItemId.CLEAR_SEARCHES }
                .value shouldBe RowValue.Action(false)
        }

    @Test
    fun `a deep link opens the item's tab highlighted until the user moves on`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.english)
            val model = viewModel(store, SettingsItemId.HIGH_LATITUDE)
            model.onRowClicked(SettingsItemId.HIGH_LATITUDE)
            model.uiState.value.dialog shouldBe null
            advanceUntilIdle()

            model.uiState.value.tab shouldBe SettingsTab.LOCATION_ATHAN
            model.uiState.value.highlighted shouldBe SettingsItemId.HIGH_LATITUDE

            model.onQueryChanged("asr")
            model.uiState.value.query shouldBe "asr"
            model.uiState.value.highlighted shouldBe null

            model.onTabSelected(SettingsTab.WIDGETS_NOTIFICATION)
            model.uiState.value.tab shouldBe SettingsTab.WIDGETS_NOTIFICATION
            model.uiState.value.query shouldBe ""
            model.onOptionClicked("ignored")
            model.uiState.value.dialog shouldBe null
        }
}
