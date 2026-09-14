/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1103 entry: a new event starts on a given day, or on a range drawn on the timeline. */
@OptIn(ExperimentalCoroutinesApi::class)
class NewEventDraftTest {
    /** 4 Dey 1405. */
    private val day = LocalDate(2026, 12, 25).toJdn()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.form(draft: NewEventDraft?): EditorForm {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel =
            EventEditorViewModel(null, FakeEventStore(), FakeSettingsSource(), FakeClock(EditorFixtures.NOW), draft)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value.content
            .shouldBeInstanceOf<EditorContent.Editing>()
            .form
    }

    @Test
    fun `a drawn range opens a timed event on its day`(): Unit =
        runTest {
            val form = form(NewEventDraft(day, startMinute = 600, endMinute = 690))
            form.start shouldBe EditorFixtures.persian(1405, 10, 4)
            form.end shouldBe form.start
            form.allDay shouldBe false
            form.startMinute shouldBe 600
            form.endMinute shouldBe 690
        }

    @Test
    fun `a day alone opens an all-day event and a missing or earlier end lasts an hour within the day`(): Unit =
        runTest {
            form(NewEventDraft(day)).run {
                start shouldBe EditorFixtures.persian(1405, 10, 4)
                allDay shouldBe true
            }
            form(NewEventDraft(day, startMinute = 720, endMinute = 700)).endMinute shouldBe 780
            form(NewEventDraft(day, startMinute = 1_430)).endMinute shouldBe EditorForm.LAST_MINUTE
            form(null).start shouldBe EditorFixtures.persian(1405, 6, 23)
        }

    @Test
    fun `minutes outside the day are rejected`() {
        shouldThrow<IllegalArgumentException> { NewEventDraft(day, startMinute = 1_440) }
        shouldThrow<IllegalArgumentException> { NewEventDraft(day, startMinute = 0, endMinute = -1) }
    }
}
