/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.nlp.ParseContext
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

/** T-1000 view model: opening, editing, validation, saving, deleting and discarding. */
@OptIn(ExperimentalCoroutinesApi::class)
class EventEditorViewModelTest {
    private val settings = FakeSettingsSource()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeEventStore = FakeEventStore(),
        eventId: Long? = null,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): EventEditorViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(EditorFixtures.NOW)
        return EventEditorViewModel(eventId, store, settings, clock, savedState).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }
    }

    private val EventEditorViewModel.content: EditorContent
        get() = uiState.value.content

    private val EventEditorViewModel.editing: EditorContent.Editing
        get() = content.shouldBeInstanceOf()

    private suspend fun ReceiveTurbine<EventEditorUiState>.awaitEditing(): EditorContent.Editing =
        awaitItem().content.shouldBeInstanceOf()

    @Test
    fun `a new event opens on today and each change is emitted`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                val opened = awaitEditing()
                opened.isNew shouldBe true
                opened.hasChanges shouldBe false
                opened.form.start shouldBe EditorFixtures.persian(1405, 6, 23)

                viewModel.onIntent(DetailsIntent.Title("Dentist"))
                awaitEditing().run {
                    form.title shouldBe "Dentist"
                    hasChanges shouldBe true
                }
                viewModel.onIntent(ScheduleIntent.ChangeCalendar(CalendarSystem.GREGORIAN))
                awaitEditing().form.calendar shouldBe CalendarSystem.GREGORIAN
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stored events open for editing and missing or unsupported ones do not`(): Unit =
        runTest {
            val store = FakeEventStore(EditorFixtures.event(id = 3, title = "Checkup"))
            viewModel(store, eventId = 3).editing.run {
                isNew shouldBe false
                form.title shouldBe "Checkup"
            }
            viewModel(store, eventId = 4).content shouldBe EditorContent.NotFound

            // A calendar the settings have no arithmetic for cannot be edited.
            val bikramSambat = NepaliCalendarSystem.date(2083, 5, 28)
            val nepali =
                FakeEventStore(EditorFixtures.event(id = 5, calendar = CalendarSystem.NEPALI, start = bikramSambat))
            viewModel(nepali, eventId = 5).content shouldBe EditorContent.NotFound
            settings.flow.value =
                EditorFixtures.settings().copy(
                    arithmetic =
                        ParseContext.DEFAULT_CALENDARS + (CalendarSystem.NEPALI to NepaliCalendarSystem),
                )
            viewModel(nepali, eventId = 5).editing.form.start shouldBe bikramSambat

            val failing = FakeEventStore(EditorFixtures.event(id = 6)).apply { failLoads = true }
            viewModel(failing, eventId = 6).content shouldBe EditorContent.NotFound
        }

    @Test
    fun `invalid forms show their errors and valid ones are saved`(): Unit =
        runTest {
            val store = FakeEventStore()
            val viewModel = viewModel(store)
            viewModel.onSave()
            viewModel.editing.errors shouldBe setOf(EditorError.TITLE_REQUIRED)
            store.events.shouldBeEmpty()

            viewModel.onIntent(DetailsIntent.Title("  Dentist "))
            viewModel.onIntent(ReminderIntent.Add(15))
            viewModel.editing.errors.shouldBeEmpty()
            viewModel.onSave()

            viewModel.content shouldBe EditorContent.Finished(EditorOutcome.SAVED)
            store.events.values.single().run {
                title shouldBe "Dentist"
                reminderMinutes shouldBe listOf(15)
                id shouldBe 1L
            }
        }

    @Test
    fun `a failed save can be retried`(): Unit =
        runTest {
            val store = FakeEventStore().apply { failSaves = true }
            val viewModel = viewModel(store)
            viewModel.onIntent(DetailsIntent.Title("Dentist"))
            viewModel.onSave()
            viewModel.editing.run {
                storeFailed shouldBe true
                busy shouldBe false
            }
            viewModel.onIntent(DetailsIntent.Notes("Bring the card"))
            viewModel.editing.storeFailed shouldBe false

            store.failSaves = false
            viewModel.onSave()
            viewModel.content shouldBe EditorContent.Finished(EditorOutcome.SAVED)
            store.events.values
                .single()
                .notes shouldBe "Bring the card"
            viewModel.onSave()
            viewModel.content shouldBe EditorContent.Finished(EditorOutcome.SAVED)
        }

    @Test
    fun `stored events are deleted and new ones discarded`(): Unit =
        runTest {
            val store = FakeEventStore(EditorFixtures.event(id = 7))
            val stored = viewModel(store, eventId = 7)
            stored.onDelete()
            stored.content shouldBe EditorContent.Finished(EditorOutcome.DELETED)
            store.events.shouldBeEmpty()
            stored.onDelete()
            stored.onDiscard()
            stored.content shouldBe EditorContent.Finished(EditorOutcome.DELETED)

            val fresh = viewModel(store)
            fresh.onDelete()
            fresh.content shouldBe EditorContent.Finished(EditorOutcome.DISCARDED)

            val failing = FakeEventStore(EditorFixtures.event(id = 8)).apply { failDeletes = true }
            val undeletable = viewModel(failing, eventId = 8)
            undeletable.onDelete()
            undeletable.editing.storeFailed shouldBe true
            failing.events.keys shouldBe setOf(8L)

            val discarded = viewModel(store)
            discarded.onDiscard()
            discarded.content shouldBe EditorContent.Finished(EditorOutcome.DISCARDED)
        }

    @Test
    fun `typed dates are applied or flagged`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onDateTextChange("hello")
            viewModel.onApplyDateText()
            viewModel.editing.run {
                dateTextUnrecognized shouldBe true
                dateText shouldBe "hello"
            }
            viewModel.onDateTextChange("1405/07/10")
            viewModel.editing.dateTextUnrecognized shouldBe false
            viewModel.onApplyDateText()
            viewModel.editing.run {
                form.start shouldBe EditorFixtures.persian(1405, 7, 10)
                dateText shouldBe ""
            }
        }

    @Test
    fun `a settings change keeps the form and relocalizes it`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onIntent(DetailsIntent.Title("Dentist"))
            viewModel.onIntent(ScheduleIntent.AllDay(false))
            settings.flow.value = EditorFixtures.settings("fa")

            viewModel.editing.run {
                form.title shouldBe "Dentist"
                display.startTime shouldBe Numerals.localizeDigits("09:00", NumeralSystem.PERSIAN)
            }
        }

    @Test
    fun `actions before the editor opens are ignored`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val viewModel =
            EventEditorViewModel(null, FakeEventStore(), settings, FakeClock(EditorFixtures.NOW), SavedStateHandle())
        viewModel.onIntent(DetailsIntent.Title("Dentist"))
        viewModel.onApplyDateText()
        viewModel.onSave()
        viewModel.onDelete()
        viewModel.onDiscard()
        viewModel.uiState.value.content shouldBe EditorContent.Loading
    }

    /** The saved state as a recreated process sees it: a copy of the values, without the old view model. */
    private fun SavedStateHandle.afterProcessDeath(): SavedStateHandle =
        SavedStateHandle(keys().associateWith { get<Any?>(it) })

    @Test
    fun `unsaved changes to a new event survive process death and are forgotten on discard`(): Unit =
        runTest {
            val saved = SavedStateHandle()
            viewModel(savedState = saved).run {
                onIntent(DetailsIntent.Title("Dentist"))
                onDateTextChange("tomorrow")
            }
            val restored = saved.afterProcessDeath()

            val recreated = viewModel(savedState = restored)
            recreated.editing.run {
                form.title shouldBe "Dentist"
                dateText shouldBe "tomorrow"
                hasChanges shouldBe true
            }
            recreated.onDiscard()
            restored.contains(EventEditorViewModel.DRAFT_KEY) shouldBe false
        }

    @Test
    fun `edits of a stored event survive process death and are forgotten once saved`(): Unit =
        runTest {
            val store = FakeEventStore(EditorFixtures.event(id = 3, title = "Checkup"))
            val saved = SavedStateHandle()
            viewModel(store, eventId = 3, savedState = saved).onIntent(DetailsIntent.Title("Checkup at 10"))
            val restored = saved.afterProcessDeath()

            val recreated = viewModel(store, eventId = 3, savedState = restored)
            recreated.editing.form.title shouldBe "Checkup at 10"
            recreated.onSave()
            store.events.getValue(3).title shouldBe "Checkup at 10"
            restored.contains(EventEditorViewModel.DRAFT_KEY) shouldBe false
        }

    @Test
    fun `an unchanged form keeps no draft and a draft of another event is ignored`(): Unit =
        runTest {
            val store = FakeEventStore(EditorFixtures.event(id = 3), EditorFixtures.event(id = 4, title = "Other"))
            val saved = SavedStateHandle()
            val viewModel = viewModel(store, eventId = 3, savedState = saved)
            viewModel.onIntent(DetailsIntent.Title("Changed"))
            saved.contains(EventEditorViewModel.DRAFT_KEY) shouldBe true
            viewModel.onIntent(DetailsIntent.Title("Dentist"))
            saved.contains(EventEditorViewModel.DRAFT_KEY) shouldBe false

            viewModel.onIntent(DetailsIntent.Title("Changed"))
            viewModel(store, eventId = 4, savedState = saved.afterProcessDeath()).editing.form.title shouldBe "Other"
        }
}
