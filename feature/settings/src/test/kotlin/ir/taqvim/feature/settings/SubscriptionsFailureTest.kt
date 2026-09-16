/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.viewModelScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** Code review I04: failing or cancelled subscription actions never leave the page busy and can be retried. */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsFailureTest {
    /** A store whose actions throw while [failing], or wait for [gate] when it is set. */
    private class FlakyStore : SubscriptionsStore {
        var failing = true
        var gate: CompletableDeferred<Unit>? = null
        private val items = MutableStateFlow(listOf(GeneralSettingsFixtures.calendarFeed))

        override fun subscriptions(): Flow<List<SubscriptionItem>> = items

        override suspend fun add(url: String): SubscriptionOutcome = act()

        override suspend fun remove(id: Long) {
            act()
        }

        override suspend fun setEnabled(
            id: Long,
            enabled: Boolean,
        ) {
            act()
        }

        override suspend fun refresh(id: Long): SubscriptionOutcome = act()

        private suspend fun act(): SubscriptionOutcome {
            gate?.await()
            check(!failing) { "store unavailable" }
            return SubscriptionOutcome.DONE
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(store: FlakyStore): SubscriptionsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return SubscriptionsViewModel(store, FakeGeneralSettingsStore(LocationFixtures.english))
    }

    @Test
    fun `a failing add or refresh reports a failure, clears the busy state and can be retried`(): Unit =
        runTest {
            val store = FlakyStore()
            val model = viewModel(store)
            model.onDraftChanged("https://example.org/a.ics")
            model.onAdd()
            advanceUntilIdle()
            model.uiState.value.adding shouldBe false
            model.uiState.value.message shouldBe SubscriptionMessage.FAILED
            model.uiState.value.draft shouldBe "https://example.org/a.ics"

            val id = GeneralSettingsFixtures.calendarFeed.id
            model.onRefresh(id)
            advanceUntilIdle()
            model.uiState.value.items
                .single()
                .refreshing shouldBe false
            model.uiState.value.message shouldBe SubscriptionMessage.FAILED

            store.failing = false
            model.onAdd()
            advanceUntilIdle()
            model.uiState.value.message shouldBe SubscriptionMessage.ADDED
            model.onRefresh(id)
            advanceUntilIdle()
            model.uiState.value.message shouldBe SubscriptionMessage.REFRESHED
        }

    @Test
    fun `failing changes are reported so the user can try again`(): Unit =
        runTest {
            val store = FlakyStore()
            val model = viewModel(store)
            advanceUntilIdle()
            model.onEnabledChanged(GeneralSettingsFixtures.calendarFeed.id, false)
            advanceUntilIdle()
            model.uiState.value.message shouldBe SubscriptionMessage.CHANGE_FAILED

            model.onDraftChanged("")
            model.onRemove(GeneralSettingsFixtures.calendarFeed.id)
            advanceUntilIdle()
            model.uiState.value.message shouldBe SubscriptionMessage.CHANGE_FAILED

            model.onDraftChanged("")
            store.failing = false
            model.onRemove(GeneralSettingsFixtures.calendarFeed.id)
            advanceUntilIdle()
            model.uiState.value.message shouldBe null
        }

    @Test
    fun `a cancelled refresh is not an error and leaves nothing busy`(): Unit =
        runTest {
            val store = FlakyStore().apply { gate = CompletableDeferred() }
            val model = viewModel(store)
            model.onDraftChanged("https://example.org/a.ics")
            model.onAdd()
            model.onRefresh(GeneralSettingsFixtures.calendarFeed.id)
            advanceUntilIdle()
            model.uiState.value.adding shouldBe true

            model.viewModelScope.cancel()
            advanceUntilIdle()
            model.uiState.value.adding shouldBe false
            model.uiState.value.message shouldBe null
        }
}
