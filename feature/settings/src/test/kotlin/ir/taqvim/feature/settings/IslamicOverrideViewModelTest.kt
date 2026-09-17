/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** ADR-0037: the official Islamic dates page switches, imports and removes overrides and reports every outcome. */
@OptIn(ExperimentalCoroutinesApi::class)
class IslamicOverrideViewModelTest {
    private class FakeStore : IslamicOverrideStore {
        val status = MutableStateFlow(IslamicOverrideStatus(LocationFixtures.english, IslamicOverrideKind.NONE))
        var importResult = OverrideImportResult.IMPORTED
        var failing = false
        var gate: CompletableDeferred<Unit>? = null
        val imported = mutableListOf<String>()

        override fun status(): Flow<IslamicOverrideStatus> = status

        override suspend fun setOfficial(enabled: Boolean) {
            act()
            status.value =
                status.value.copy(kind = if (enabled) IslamicOverrideKind.OFFICIAL else IslamicOverrideKind.NONE)
        }

        override suspend fun import(uri: String): OverrideImportResult {
            act()
            imported += uri
            return importResult
        }

        override suspend fun remove() {
            act()
            status.value = status.value.copy(kind = IslamicOverrideKind.NONE)
        }

        private suspend fun act() {
            gate?.await()
            check(!failing) { "store unavailable" }
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(store: FakeStore): IslamicOverrideViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return IslamicOverrideViewModel(store)
    }

    @Test
    fun `the status is loaded and switching the official dates on and off is stored`(): Unit =
        runTest {
            val store = FakeStore()
            val model = viewModel(store)
            model.uiState.value.status shouldBe null
            advanceUntilIdle()
            model.uiState.value.status
                ?.kind shouldBe IslamicOverrideKind.NONE

            model.onOfficialChanged(true)
            advanceUntilIdle()
            model.uiState.value.status
                ?.kind shouldBe IslamicOverrideKind.OFFICIAL
            model.uiState.value.busy shouldBe false

            model.onRemove()
            advanceUntilIdle()
            model.uiState.value.status
                ?.kind shouldBe IslamicOverrideKind.NONE
        }

    @Test
    fun `an import reports its result and a refused file keeps the page usable`(): Unit =
        runTest {
            val store = FakeStore()
            val model = viewModel(store)
            model.onImport("content://files/good.json")
            advanceUntilIdle()
            model.uiState.value.importResult shouldBe OverrideImportResult.IMPORTED
            store.imported shouldBe listOf("content://files/good.json")

            store.importResult = OverrideImportResult.INVALID_LENGTH
            model.onImport("content://files/bad.json")
            advanceUntilIdle()
            model.uiState.value.importResult shouldBe OverrideImportResult.INVALID_LENGTH
            model.uiState.value.busy shouldBe false
        }

    @Test
    fun `failures are reported, a busy page ignores new actions and nothing stays busy`(): Unit =
        runTest {
            val store = FakeStore()
            val model = viewModel(store)
            store.failing = true
            model.onOfficialChanged(true)
            advanceUntilIdle()
            model.uiState.value.changeFailed shouldBe true
            model.onImport("content://files/any.json")
            advanceUntilIdle()
            model.uiState.value.importResult shouldBe OverrideImportResult.UNREADABLE
            model.uiState.value.changeFailed shouldBe false

            store.failing = false
            store.gate = CompletableDeferred()
            model.onImport("content://files/slow.json")
            advanceUntilIdle()
            model.uiState.value.busy shouldBe true
            model.onRemove()
            model.onImport("content://files/other.json")
            store.gate?.complete(Unit)
            advanceUntilIdle()
            store.imported shouldBe listOf("content://files/slow.json")
            model.uiState.value.busy shouldBe false
        }
}
