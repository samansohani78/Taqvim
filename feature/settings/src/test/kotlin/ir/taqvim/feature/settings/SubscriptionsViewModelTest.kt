/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.shouldBe
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

/** T-1500 over T-1003: subscriptions are listed, validated before adding, refreshed, paused and removed. */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeSubscriptionsStore,
        settings: FakeGeneralSettingsStore = FakeGeneralSettingsStore(LocationFixtures.english),
    ): SubscriptionsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return SubscriptionsViewModel(store, settings)
    }

    @Test
    fun `addresses are checked before they are added`() {
        SubscriptionAddress.isValid(" https://example.org/a.ics ") shouldBe true
        SubscriptionAddress.isValid("WEBCAL://example.org/a.ics") shouldBe true
        SubscriptionAddress.isValid("http://example.org/a.ics") shouldBe false
        SubscriptionAddress.isValid("https://") shouldBe false
        SubscriptionAddress.isValid("https:///path") shouldBe false
        SubscriptionAddress.isValid("https://exa mple.org") shouldBe false
    }

    @Test
    fun `subscriptions are listed and valid addresses are added`(): Unit =
        runTest {
            val store = FakeSubscriptionsStore()
            val model = viewModel(store)
            advanceUntilIdle()

            model.uiState.value.items
                .map { it.name to it.downloaded } shouldBe listOf("Holidays feed" to true, "Team" to false)

            model.onDraftChanged("example.org/x.ics")
            model.onAdd()
            model.uiState.value.message shouldBe SubscriptionMessage.INVALID_ADDRESS
            store.added shouldBe emptyList()

            model.onDraftChanged(" https://example.org/new.ics ")
            model.uiState.value.message shouldBe null
            model.onAdd()
            model.uiState.value.adding shouldBe true
            model.onAdd()
            advanceUntilIdle()

            store.added shouldBe listOf("https://example.org/new.ics")
            model.uiState.value.draft shouldBe ""
            model.uiState.value.message shouldBe SubscriptionMessage.ADDED
            model.uiState.value.items.size shouldBe 3
        }

    @Test
    fun `failures keep the address and name the problem`(): Unit =
        runTest {
            val store = FakeSubscriptionsStore(outcome = SubscriptionOutcome.ALREADY_SUBSCRIBED)
            val model = viewModel(store)
            advanceUntilIdle()

            model.onDraftChanged("https://example.org/holidays.ics")
            model.onAdd()
            advanceUntilIdle()

            model.uiState.value.draft shouldBe "https://example.org/holidays.ics"
            model.uiState.value.message shouldBe SubscriptionMessage.ALREADY_SUBSCRIBED
            listOf(
                SubscriptionOutcome.INVALID_ADDRESS to SubscriptionMessage.INVALID_ADDRESS,
                SubscriptionOutcome.NETWORK_NOT_ALLOWED to SubscriptionMessage.NETWORK_NOT_ALLOWED,
                SubscriptionOutcome.FAILED to SubscriptionMessage.FAILED,
                SubscriptionOutcome.DONE to SubscriptionMessage.REFRESHED,
            ).forEach { (outcome, message) ->
                store.outcome = outcome
                model.onRefresh(1)
                model.onRefresh(1)
                advanceUntilIdle()
                model.uiState.value.message shouldBe message
            }
            store.refreshed shouldBe listOf(1L, 1L, 1L, 1L)
        }

    @Test
    fun `subscriptions are paused, removed and the network switch is stored`(): Unit =
        runTest {
            val store = FakeSubscriptionsStore()
            val settings = FakeGeneralSettingsStore(LocationFixtures.english)
            val model = viewModel(store, settings)
            advanceUntilIdle()

            model.onEnabledChanged(2, true)
            model.onRemove(1)
            model.onNetworkAllowedChanged(false)
            advanceUntilIdle()

            store.current.map { it.id to it.enabled } shouldBe listOf(2L to true)
            settings.current.subscriptionsNetworkAllowed shouldBe false
            model.uiState.value.networkAllowed shouldBe false
        }
}
