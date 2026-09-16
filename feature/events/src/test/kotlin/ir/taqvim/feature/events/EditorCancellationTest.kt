/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.FakeClock
import kotlin.coroutines.cancellation.CancellationException
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

/**
 * Code review I04: a cancelled load, save or delete is not reported as a failure and leaves nothing busy. The store
 * throws [CancellationException] the way a cancelled call inside it would.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorCancellationTest {
    private class CancellingStore(
        private val event: PersonalEvent,
    ) : PersonalEventStore {
        var cancelLoads = false
        var cancelWrites = true

        override suspend fun load(id: Long): PersonalEvent? {
            if (cancelLoads) throw CancellationException("load cancelled")
            return event
        }

        override suspend fun save(event: PersonalEvent): Long {
            if (cancelWrites) throw CancellationException("save cancelled")
            return 1
        }

        override suspend fun delete(id: Long) {
            if (cancelWrites) throw CancellationException("delete cancelled")
        }

        override suspend fun loadOccurrence(
            id: Long,
            originalDay: Jdn,
        ): PersonalEvent? = load(id)

        override suspend fun saveOccurrence(
            id: Long,
            originalDay: Jdn,
            occurrence: PersonalEvent,
        ) {
            save(occurrence)
        }

        override suspend fun cancelOccurrence(
            id: Long,
            originalDay: Jdn,
        ) {
            delete(id)
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(store: PersonalEventStore): EventEditorViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = FakeClock(EditorFixtures.NOW)
        return EventEditorViewModel(STORED_ID, store, FakeSettingsSource(), clock, SavedStateHandle()).also { model ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        }
    }

    private fun EventEditorViewModel.editing(): EditorContent.Editing = uiState.value.content.shouldBeInstanceOf()

    @Test
    fun `a cancelled save or delete clears busy without a failure, and saving again works`(): Unit =
        runTest {
            val store = CancellingStore(EditorFixtures.event(id = STORED_ID, title = "Checkup"))
            val model = viewModel(store)
            model.onIntent(DetailsIntent.Title("Dentist"))
            model.onSave()
            model.editing().run {
                busy shouldBe false
                storeFailed shouldBe false
            }
            model.onDelete()
            model.editing().run {
                busy shouldBe false
                storeFailed shouldBe false
            }

            store.cancelWrites = false
            model.onSave()
            model.uiState.value.content shouldBe EditorContent.Finished(EditorOutcome.SAVED)
        }

    @Test
    fun `a cancelled load is not reported as a missing event`(): Unit =
        runTest {
            val store = CancellingStore(EditorFixtures.event(id = STORED_ID)).apply { cancelLoads = true }
            viewModel(store).uiState.value.content shouldBe EditorContent.Loading
        }

    private companion object {
        const val STORED_ID = 3L
    }
}
