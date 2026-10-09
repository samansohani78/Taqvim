/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.core.workdays.WorkdayProfile
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** F-07: the editor that finally writes `workday_profiles`, which nothing wrote to before. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkdayProfileViewModelTest {
    private val store = FakeWorkdayProfileStore()
    private val settings =
        ToolsSettingsSource {
            flowOf(
                ToolsSettings(
                    language = requireNotNull(LanguageTable.forCode("en")),
                    homeZone = TimeZone.UTC,
                    calendars = listOf(PersianCalendarSystem),
                ),
            )
        }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler): WorkdayProfileViewModel {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        return WorkdayProfileViewModel(store, settings, FakeClock(NOW))
    }

    @Test
    fun `a saved profile reaches the store with everything the editor collected`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                // StandardTestDispatcher runs nothing until asked: let the settings collection start before the
                // picker is used, exactly as a subscribed screen would.
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Iran office")
                viewModel.onWeekend(Weekday.FRIDAY, true)
                viewModel.onWeekend(Weekday.THURSDAY, true)
                viewModel.onWeekend(Weekday.THURSDAY, false)
                viewModel.onHolidaySource(EventSource.IRAN_OFFICIAL, true)
                viewModel.onHalfDays(HalfDayPolicy.FULL_WORKDAY)
                viewModel.onSave()
                awaitDraft { it == null && store.saved.isNotEmpty() }

                val saved = store.saved.single()
                saved.second shouldBe "Iran office"
                saved.third.weekend shouldContainExactly setOf(Weekday.FRIDAY)
                saved.third.holidaySources shouldContainExactly setOf(EventSource.IRAN_OFFICIAL)
                saved.third.halfDays shouldBe HalfDayPolicy.FULL_WORKDAY
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tapping save twice while the first write is in flight stores one profile`(): Unit =
        runTest {
            // The draft is cleared only after the store returns, so a second tap inside that window used to see the
            // same draft with no id and save it again — two taps, two profiles.
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onName("My company")
                viewModel.onWeekend(Weekday.FRIDAY, true)
                runCurrent()

                store.hold = CompletableDeferred()
                viewModel.onSave()
                runCurrent()
                viewModel.onSave()
                runCurrent()
                store.hold?.complete(Unit)
                awaitDraft { it == null && store.saved.isNotEmpty() }

                store.saved.size shouldBe 1
                store.stored.value.size shouldBe 1
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `leave is picked as two days and kept in order, whichever is chosen first`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                // StandardTestDispatcher runs nothing until asked: let the settings collection start before the
                // picker is used, exactly as a subscribed screen would.
                runCurrent()
                viewModel.onNew()
                viewModel.onName("With leave")
                viewModel.onAddLeave()
                // The later day is chosen first: the range must still start on the earlier one.
                viewModel.onLeaveDatePicked(DateSelection(1405, 1, 10))
                viewModel.onLeaveDatePicked(DateSelection(1405, 1, 3))
                val draft = awaitDraft { it?.leave?.isNotEmpty() == true }.shouldNotBeNull()

                val row = draft.leave.single()
                (row.firstJdn < row.lastJdn) shouldBe true
                row.lastJdn - row.firstJdn shouldBe 7L

                viewModel.onSave()
                awaitDraft { it == null && store.saved.isNotEmpty() }
                store.saved
                    .single()
                    .third.personalLeave shouldContainExactly
                    listOf(LeaveRange(Jdn(row.firstJdn), Jdn(row.lastJdn)))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a draft without a name cannot be saved, and leaving the editor writes nothing`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                // StandardTestDispatcher runs nothing until asked: let the settings collection start before the
                // picker is used, exactly as a subscribed screen would.
                runCurrent()
                viewModel.onNew()
                viewModel.onWeekend(Weekday.FRIDAY, true)
                awaitDraft { it != null }.shouldNotBeNull().canSave shouldBe false

                viewModel.onSave()
                viewModel.onCancel()
                awaitDraft { it == null }.shouldBeNull()

                store.saved.shouldBeEmptyList()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the first profile becomes the default without being asked for`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                // StandardTestDispatcher runs nothing until asked: let the settings collection start before the
                // picker is used, exactly as a subscribed screen would.
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Only one")
                viewModel.onSave()
                awaitDraft { store.defaults.isNotEmpty() }

                store.defaults shouldContainExactly listOf(1L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<WorkdayProfileUiState>.awaitDraft(
        predicate: (WorkdayProfileDraft?) -> Boolean,
    ): WorkdayProfileDraft? {
        while (true) {
            val state = awaitItem()
            if (predicate(state.editing)) return state.editing
        }
    }

    private fun List<*>.shouldBeEmptyList() = isEmpty() shouldBe true

    private companion object {
        val NOW: Instant = Instant.parse("2026-06-21T09:00:00Z")
    }
}

/** An in-memory [WorkdayProfileStore] that records what the editor asked it to do. */
private class FakeWorkdayProfileStore : WorkdayProfileStore {
    val stored = MutableStateFlow<List<NamedWorkdayProfile>>(emptyList())
    val saved = mutableListOf<Triple<Long?, String, WorkdayProfile>>()
    val defaults = mutableListOf<Long>()

    /** Held open to keep a save in flight, as a real database write is while the user taps again. */
    var hold: CompletableDeferred<Unit>? = null

    override fun profiles(): Flow<List<NamedWorkdayProfile>> = stored

    override suspend fun save(
        id: Long?,
        name: String,
        profile: WorkdayProfile,
    ): Long {
        hold?.await()
        saved += Triple(id, name, profile)
        val newId = id ?: (stored.value.size + 1).toLong()
        stored.value = stored.value.filterNot { it.id == newId } + NamedWorkdayProfile(newId, name, profile, false)
        if (id == null && stored.value.size == 1) makeDefault(newId)
        return newId
    }

    override suspend fun delete(id: Long) {
        stored.value = stored.value.filterNot { it.id == id }
    }

    override suspend fun makeDefault(id: Long) {
        defaults += id
        stored.value = stored.value.map { it.copy(isDefault = it.id == id) }
    }
}
