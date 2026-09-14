/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.ui.permission.NOTIFICATION_PERMISSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1000 UI tests: create, edit and delete through the route, and what each section shows and reports. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EventEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val outcomes = mutableListOf<EditorOutcome>()

    private fun route(
        store: FakeEventStore,
        eventId: Long? = null,
    ): EventEditorViewModel {
        val viewModel = EventEditorViewModel(eventId, store, FakeSettingsSource(), FakeClock(EditorFixtures.NOW))
        composeRule.setContent {
            EditorTestTheme { EventEditorRoute(eventId, onClose = { outcomes += it }, viewModel = viewModel) }
        }
        return viewModel
    }

    private fun screen(
        form: EditorForm,
        intents: MutableList<EditorIntent>,
        session: (EditorForm) -> EditorSession.Editing = { EditorSession.Editing(it, it) },
    ) {
        val state = EditorPresenter.present(session(form), EditorFixtures.settings())
        composeRule.setContent {
            EditorTestTheme { EventEditorScreen(state, EventEditorActions(onIntent = { intents += it })) }
        }
    }

    @Test
    fun createsAnEvent() {
        val store = FakeEventStore()
        route(store)
        composeRule.onNodeWithText("New event").assertIsDisplayed()
        composeRule.onNodeWithText("Title").performTextInput("Dentist")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertEquals(
            "Dentist",
            store.events.values
                .single()
                .title,
        )
        assertEquals(listOf(EditorOutcome.SAVED), outcomes)
    }

    @Test
    fun editsAnEvent() {
        val store = FakeEventStore(EditorFixtures.event(id = 2, title = "Dentist"))
        route(store, eventId = 2)
        composeRule.onNodeWithText("Edit event").assertIsDisplayed()
        composeRule.onNodeWithText("Dentist").performTextReplacement("Dentist visit")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertEquals("Dentist visit", store.events.getValue(2).title)
        assertEquals(listOf(EditorOutcome.SAVED), outcomes)
    }

    @Test
    fun deletesAfterConfirmation() {
        val store = FakeEventStore(EditorFixtures.event(id = 2))
        route(store, eventId = 2)
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this event?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete event").performClick()
        composeRule.waitForIdle()

        assertTrue(store.events.isEmpty())
        assertEquals(listOf(EditorOutcome.DELETED), outcomes)
    }

    @Test
    fun showsValidationErrorsInsteadOfSaving() {
        val store = FakeEventStore()
        route(store)
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Enter a title").assertIsDisplayed()
        composeRule.onNodeWithText("Fix the highlighted fields to save.").assertIsDisplayed()
        assertTrue(store.events.isEmpty())
        assertTrue(outcomes.isEmpty())
    }

    @Test
    fun discardingChangesAsksFirst() {
        route(FakeEventStore())
        composeRule.onNodeWithText("Title").performTextInput("Dentist")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep editing").performClick()
        assertTrue(outcomes.isEmpty())

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(EditorOutcome.DISCARDED), outcomes)
    }

    @Test
    fun typedDatesMoveTheStart() {
        val viewModel = route(FakeEventStore())
        composeRule.onNodeWithText("Date in words").performTextInput("1405/07/10")
        composeRule.onNodeWithText("Set date").performClick()
        composeRule.waitForIdle()

        val editing = viewModel.uiState.value.content as? EditorContent.Editing
        assertEquals(EditorFixtures.persian(1405, 7, 10), editing?.form?.start)
    }

    @Test
    fun scheduleAndWeeklyRepeatReportTheirChanges() {
        val intents = mutableListOf<EditorIntent>()
        val repeat = RepeatForm.create(Frequency.WEEKLY, EditorFixtures.form().start, NumeralSystem.LATIN)
        screen(EditorFixtures.form().copy(allDay = false, repeat = repeat), intents)

        composeRule.onNodeWithText("All day").performClick()
        composeRule.onNodeWithText("Gregorian").performClick()
        composeRule.onNodeWithContentDescription("Start time: 09:00").assertIsDisplayed()
        composeRule.onNodeWithText("Every how many weeks").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Monday").performScrollTo().performClick()
        composeRule.onNodeWithText("Skip that month").assertDoesNotExist()
        composeRule.onNodeWithText("Monthly").performScrollTo().performClick()
        composeRule.onNodeWithText("On date").performScrollTo().performClick()

        assertEquals(
            listOf(
                ScheduleIntent.AllDay(true),
                ScheduleIntent.ChangeCalendar(CalendarSystem.GREGORIAN),
                RepeatIntent.ToggleWeekday(Weekday.MONDAY),
                SetRepeat(Frequency.MONTHLY),
                RepeatIntent.Ends(RecurrenceEnd.UNTIL),
            ),
            intents,
        )
    }

    @Test
    fun monthlyRepeatOffersTheInvalidDayPolicyAndDatePickers() {
        val intents = mutableListOf<EditorIntent>()
        val repeat =
            RepeatForm.create(Frequency.MONTHLY, EditorFixtures.form().start, NumeralSystem.LATIN)
        screen(EditorFixtures.form().copy(repeat = repeat), intents)

        composeRule.onNodeWithText("Move to the next day").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Start date:", substring = true).performScrollTo().performClick()
        composeRule.onNodeWithText("Start date").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(
            listOf(
                RepeatIntent.InvalidDays(InvalidDatePolicy.NEXT_DAY),
                ScheduleIntent.StartDate(DateSelection(1405, 6, 23)),
            ),
            intents,
        )
    }

    @Test
    fun timePickerReportsTheMinuteOfTheDay() {
        val intents = mutableListOf<EditorIntent>()
        screen(EditorFixtures.form().copy(allDay = false), intents)

        composeRule.onNodeWithContentDescription("End time: 10:00").performScrollTo().performClick()
        composeRule.onNodeWithText("End time").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf<EditorIntent>(ScheduleIntent.EndTime(600)), intents)
    }

    @Test
    fun addingAReminderAsksForNotificationsWhileMissing() {
        val launched = mutableListOf<Any?>()
        val registry =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry =
                    object : ActivityResultRegistry() {
                        override fun <I, O> onLaunch(
                            requestCode: Int,
                            contract: ActivityResultContract<I, O>,
                            input: I,
                            options: ActivityOptionsCompat?,
                        ) {
                            launched += input
                            dispatchResult(requestCode, false)
                        }
                    }
            }
        val store = FakeEventStore()
        val viewModel = EventEditorViewModel(null, store, FakeSettingsSource(), FakeClock(EditorFixtures.NOW))
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry) {
                EditorTestTheme { EventEditorRoute(null, onClose = { outcomes += it }, viewModel = viewModel) }
            }
        }

        composeRule.onNodeWithText("Add reminder").performScrollTo().performClick()
        composeRule.onNodeWithText("At the start").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<Any?>(NOTIFICATION_PERMISSION), launched)
    }

    @Test
    fun remindersAndColorsReportTheirChanges() {
        val intents = mutableListOf<EditorIntent>()
        screen(EditorFixtures.form().copy(reminderMinutes = listOf(15)), intents)

        composeRule.onNodeWithContentDescription("Remove reminder: 15 minutes before").performScrollTo().performClick()
        composeRule.onNodeWithText("Add reminder").performScrollTo().performClick()
        composeRule.onNodeWithText("At the start").performClick()
        composeRule.onNodeWithContentDescription("Color 1").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("No color").performScrollTo().performClick()

        assertEquals(
            listOf(
                ReminderIntent.Remove(15),
                ReminderIntent.Add(0),
                DetailsIntent.Color(EditorPresenter.COLORS[0]),
                DetailsIntent.Color(null),
            ),
            intents,
        )
    }

    @Test
    fun loadingNotFoundAndFailureStates() {
        var state by mutableStateOf(EventEditorUiState())
        composeRule.setContent { EditorTestTheme { EventEditorScreen(state, EventEditorActions()) } }
        composeRule.onNodeWithContentDescription("Loading event").assertExists()

        state = EventEditorUiState(EditorContent.NotFound)
        composeRule.onNodeWithText("Event not found").assertIsDisplayed()

        val form = EditorFixtures.form()
        val failed = EditorSession.Editing(form, form, storeFailed = true)
        state = EditorPresenter.present(failed, EditorFixtures.settings())
        composeRule.onNodeWithText("The event could not be saved or deleted. Try again.").assertIsDisplayed()

        state = EventEditorUiState(EditorContent.Finished(EditorOutcome.SAVED))
        composeRule.onNodeWithText("Save").assertDoesNotExist()
    }
}
