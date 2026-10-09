/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.ShiftRotation
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** F-08: building a rotation out of named shift types, and writing it to the store the calendar reads. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShiftRotationViewModelTest {
    private val store = FakeShiftRotationStore()
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

    private fun viewModel(scheduler: TestCoroutineScheduler): ShiftRotationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        return ShiftRotationViewModel(store, settings, FakeClock(NOW))
    }

    @Test
    fun `a pattern is built by tapping the types in order, and saved as those shifts`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Two on, two off")
                addType(viewModel, "Day")
                addType(viewModel, "Night")
                addType(viewModel, "Off")
                listOf(0, 0, 1, 1, 2, 2).forEach(viewModel::onAppendToPattern)
                val draft = awaitDraft { it?.pattern?.size == 6 }.shouldNotBeNull()

                draft.patternTypes.map { it.label } shouldContainExactly
                    listOf("Day", "Day", "Night", "Night", "Off", "Off")
                // Every type is given a colour as it is added, so a rotation is readable without extra work.
                draft.types.map { it.color }.none { it == null } shouldBe true

                viewModel.onSave()
                awaitDraft { it == null && store.saved.isNotEmpty() }
                store.saved
                    .single()
                    .pattern
                    .map { it.label } shouldContainExactly listOf("Day", "Day", "Night", "Night", "Off", "Off")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tapping save twice while the first write is in flight stores one rotation`(): Unit =
        runTest {
            // The draft is cleared only after the store returns, so a second tap inside that window used to see the
            // same draft with no id and save it again — two taps, two rotations.
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Nights")
                addType(viewModel, "Night")
                viewModel.onAppendToPattern(0)
                awaitDraft { it?.canSave == true }

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
    fun `removing a type takes it out of the pattern too, and keeps the rest pointing at the right type`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Mixed")
                addType(viewModel, "Day")
                addType(viewModel, "Night")
                addType(viewModel, "Off")
                listOf(0, 1, 2, 1).forEach(viewModel::onAppendToPattern)
                awaitDraft { it?.pattern?.size == 4 }

                // Removing the middle type must not leave the pattern pointing at the wrong one, which an index
                // shifted by one silently would.
                viewModel.onRemoveType(1)
                val draft = awaitDraft { it?.types?.size == 2 }.shouldNotBeNull()

                draft.types.map { it.label } shouldContainExactly listOf("Day", "Off")
                draft.patternTypes.map { it.label } shouldContainExactly listOf("Day", "Off")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a rotation without a pattern cannot be saved`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onName("Named but empty")
                viewModel.onSave()
                runCurrent()

                val draft =
                    viewModel.uiState.value.editing
                        .shouldNotBeNull()
                draft.canSave shouldBe false
                // The editor is still open and nothing was written: a half-built rotation is not a saved one.
                store.saved.isEmpty() shouldBe true
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the anchor is picked in the user's own calendar`(): Unit =
        runTest {
            val viewModel = viewModel(testScheduler)
            viewModel.uiState.test {
                awaitItem()
                runCurrent()
                viewModel.onNew()
                viewModel.onPickAnchor()
                runCurrent()
                viewModel.uiState.value.picker
                    .shouldNotBeNull()

                viewModel.onAnchorPicked(DateSelection(1405, 1, 1))
                runCurrent()

                // 1 Farvardin 1405 is 21 March 2026, not 1 January: the picker speaks the user's calendar.
                val draft =
                    viewModel.uiState.value.editing
                        .shouldNotBeNull()
                draft.anchorJdn shouldBe PersianCalendarSystem.toJdn(PersianCalendarSystem.date(1405, 1, 1)).value
                viewModel.uiState.value.picker
                    .shouldBeNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun addType(
        viewModel: ShiftRotationViewModel,
        label: String,
    ) {
        viewModel.onNewTypeLabel(label)
        viewModel.onAddType()
    }

    private suspend fun ReceiveTurbine<ShiftRotationUiState>.awaitDraft(
        predicate: (ShiftRotationDraft?) -> Boolean,
    ): ShiftRotationDraft? = awaitState { predicate(it.editing) }.editing

    private suspend fun ReceiveTurbine<ShiftRotationUiState>.awaitState(
        predicate: (ShiftRotationUiState) -> Boolean,
    ): ShiftRotationUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-06-21T09:00:00Z")
    }
}

/** An in-memory [ShiftRotationStore] that records what the editor asked it to do. */
private class FakeShiftRotationStore : ShiftRotationStore {
    val stored = MutableStateFlow<List<ShiftRotation>>(emptyList())
    val saved = mutableListOf<ShiftRotation>()
    val exceptions = mutableListOf<Triple<Long, Long, String?>>()

    /** Held open to keep a save in flight, as a real database write is while the user taps again. */
    var hold: CompletableDeferred<Unit>? = null

    override fun rotations(): Flow<List<ShiftRotation>> = stored

    override suspend fun save(rotation: ShiftRotation): Long {
        hold?.await()
        saved += rotation
        val id = rotation.id.takeIf { it != 0L } ?: (stored.value.size + 1).toLong()
        stored.value = stored.value.filterNot { it.id == id } + rotation.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        stored.value = stored.value.filterNot { it.id == id }
    }

    override suspend fun setException(
        rotationId: Long,
        day: Jdn,
        shift: String?,
    ) {
        exceptions += Triple(rotationId, day.value, shift)
    }
}
