/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** Review F01 (ADR-0034): changing or cancelling one occurrence of a repeating event. */
@OptIn(ExperimentalCoroutinesApi::class)
class OccurrenceEditTest {
    private val weekly = EditorFixtures.event().copy(recurrence = RecurrenceRule(Frequency.WEEKLY))
    private val nextWeek = PersianCalendarSystem.toJdn(weekly.start) + 7
    private val target = OccurrenceTarget(nextWeek)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeEventStore,
        occurrence: OccurrenceTarget? = target,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): EventEditorViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(EditorFixtures.NOW)
        val settings = FakeSettingsSource()
        return EventEditorViewModel(1, store, settings, clock, savedState, occurrence = occurrence).also { model ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        }
    }

    private val EventEditorViewModel.content: EditorContent
        get() = uiState.value.content

    @Test
    fun `a repeating event opened from a day asks first, then edits only that occurrence`(): Unit =
        runTest {
            val store = FakeEventStore(weekly)
            val viewModel = viewModel(store)
            viewModel.content shouldBe EditorContent.ChoosingScope

            viewModel.onChooseScope(thisOccurrence = true)
            val editing = viewModel.content.shouldBeInstanceOf<EditorContent.Editing>()
            editing.occurrenceOnly shouldBe true
            editing.form.repeat.shouldBeNull()
            PersianCalendarSystem.toJdn(editing.form.start) shouldBe nextWeek

            viewModel.onIntent(DetailsIntent.Title("Moved"))
            viewModel.onSave()

            viewModel.content shouldBe EditorContent.Finished(EditorOutcome.SAVED)
            store.occurrences[1L to nextWeek]?.title shouldBe "Moved"
            store.events.getValue(1) shouldBe weekly
        }

    @Test
    fun `deleting in occurrence mode cancels only that occurrence`(): Unit =
        runTest {
            val store = FakeEventStore(weekly)
            val viewModel = viewModel(store)
            viewModel.onChooseScope(thisOccurrence = true)

            viewModel.onDelete()

            viewModel.content shouldBe EditorContent.Finished(EditorOutcome.DELETED)
            (1L to nextWeek in store.occurrences) shouldBe true
            store.occurrences[1L to nextWeek].shouldBeNull()
            store.events.getValue(1) shouldBe weekly
        }

    @Test
    fun `choosing all occurrences edits the series`(): Unit =
        runTest {
            val store = FakeEventStore(weekly)
            val viewModel = viewModel(store)
            viewModel.onChooseScope(thisOccurrence = false)
            val editing = viewModel.content.shouldBeInstanceOf<EditorContent.Editing>()
            editing.occurrenceOnly shouldBe false
            editing.form.repeat?.frequency shouldBe Frequency.WEEKLY

            viewModel.onIntent(DetailsIntent.Title("Renamed"))
            viewModel.onSave()

            store.events.getValue(1).title shouldBe "Renamed"
            store.occurrences shouldBe emptyMap()
        }

    @Test
    fun `one-off events, events without a day and dismissals do not change occurrences`(): Unit =
        runTest {
            viewModel(FakeEventStore(EditorFixtures.event())).content.shouldBeInstanceOf<EditorContent.Editing>()
            viewModel(FakeEventStore(weekly), occurrence = null)
                .content
                .shouldBeInstanceOf<EditorContent.Editing>()
                .occurrenceOnly shouldBe false

            val asking = viewModel(FakeEventStore(weekly))
            asking.onDiscard()
            asking.content shouldBe EditorContent.Finished(EditorOutcome.DISCARDED)
        }

    @Test
    fun `a recreated editor keeps the chosen scope and the occurrence draft`(): Unit =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel(FakeEventStore(weekly), savedState = savedState)
            first.onChooseScope(thisOccurrence = true)
            first.onIntent(DetailsIntent.Title("Unsaved"))

            val restored = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) })
            val recreated = viewModel(FakeEventStore(weekly), savedState = restored)
            val editing = recreated.content.shouldBeInstanceOf<EditorContent.Editing>()
            editing.occurrenceOnly shouldBe true
            editing.form.title shouldBe "Unsaved"

            recreated.onDiscard()
            restored.keys() shouldBe emptySet()
        }
}
