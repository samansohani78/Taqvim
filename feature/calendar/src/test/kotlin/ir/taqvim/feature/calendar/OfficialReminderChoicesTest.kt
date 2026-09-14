/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** T-1002: reminders before the official event whose source is shown are listed, stored and turned on and off. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfficialReminderChoicesTest {
    private val today = gregorian(2026, 3, 18)
    private val days = FakeDaySource()
    private val store = FakeOfficialReminderStore()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(reminders: OfficialReminderStore = store): CalendarViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return CalendarViewModel(
            FakeSettingsSource(PERSIAN_FIRST),
            FakeTodaySource(today),
            days,
            days,
            SearchEventsUseCase(FakeSearchSource(emptyMap()), dispatcher),
            FakePlaceSource(null),
            FakeNowSource(TEST_NOW),
            FakeDisplayStore(),
            dispatcher,
            officialReminders = reminders,
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

    @Test
    fun `an official event's reminders are listed and turned on and off`(): Unit =
        runTest {
            val viewModel = viewModel()
            val official = DayDetailsSamples.official
            store.enabled.value = mapOf(official.id to setOf(1))
            viewModel.uiState.test {
                awaitContent { true }.officialReminders shouldBe null
                viewModel.onAction(CalendarAction.ShowEventSource(official))
                awaitContent { it.officialReminders != null }.officialReminders shouldBe
                    OfficialReminderChoices(official.id, persistentSetOf(1))

                viewModel.onAction(CalendarAction.ToggleOfficialReminder(3, enabled = true))
                awaitContent { it.officialReminders?.enabled == setOf(1, 3) }
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(1, enabled = false))
                awaitContent { it.officialReminders?.enabled == setOf(3) }

                viewModel.onAction(CalendarAction.DismissEventSource)
                awaitContent { it.officialReminders == null }
                cancelAndIgnoreRemainingEvents()
            }
            store.writes shouldBe listOf(Triple(official.id, 3, true), Triple(official.id, 1, false))
        }

    @Test
    fun `turning a reminder on asks for notifications, other events and failures store nothing`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.effects.test {
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(0, enabled = true))
                viewModel.onAction(CalendarAction.ShowEventSource(DayDetailsSamples.personal))
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(0, enabled = true))
                store.writes shouldBe emptyList()

                viewModel.onAction(CalendarAction.ShowEventSource(DayDetailsSamples.official))
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(7, enabled = true))
                awaitItem() shouldBe CalendarEffect.RequestNotificationPermission
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(7, enabled = false))
                expectNoEvents()

                store.failing = true
                viewModel.onAction(CalendarAction.ToggleOfficialReminder(1, enabled = true))
                awaitItem() shouldBe CalendarEffect.ShowSnackbar(CalendarMessage.SETTING_NOT_SAVED)
            }
        }

    @Test
    fun `without reminder storage nothing is enabled and writes do nothing`(): Unit =
        runTest {
            OfficialReminderStore.NONE.daysBefore("any").first() shouldBe emptySet()
            OfficialReminderStore.NONE.setReminder("any", 1, enabled = true)
            val viewModel = viewModel(OfficialReminderStore.NONE)
            viewModel.uiState.test {
                viewModel.onAction(CalendarAction.ShowEventSource(DayDetailsSamples.official))
                awaitContent { it.officialReminders != null }.officialReminders?.enabled shouldBe emptySet()
                cancelAndIgnoreRemainingEvents()
            }
        }
}

/** Reminders per official event in memory; while [failing] is set, every write fails. */
internal class FakeOfficialReminderStore : OfficialReminderStore {
    val enabled = MutableStateFlow(emptyMap<String, Set<Int>>())
    val writes = mutableListOf<Triple<String, Int, Boolean>>()
    var failing = false

    override fun daysBefore(eventId: String): Flow<Set<Int>> = enabled.map { it[eventId].orEmpty() }

    override suspend fun setReminder(
        eventId: String,
        daysBefore: Int,
        enabled: Boolean,
    ) {
        check(!failing) { "storage unavailable" }
        writes += Triple(eventId, daysBefore, enabled)
        this.enabled.update { current ->
            val days = current[eventId].orEmpty()
            current + (eventId to if (enabled) days + daysBefore else days - daysBefore)
        }
    }
}
