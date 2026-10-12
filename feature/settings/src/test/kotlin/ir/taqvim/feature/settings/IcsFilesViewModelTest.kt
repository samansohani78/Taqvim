/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.collections.shouldContainExactly
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

/** T-1003 calendar files: the page's state over its store, including the one-at-a-time rule. */
@OptIn(ExperimentalCoroutinesApi::class)
class IcsFilesViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(store: IcsFilesStore): IcsFilesViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return IcsFilesViewModel(store)
    }

    @Test
    fun `the event count follows the store and starts unknown`(): Unit =
        runTest {
            val store = FakeIcsFilesStore()
            val model = viewModel(store)

            model.uiState.value.eventCount shouldBe null
            advanceUntilIdle()
            model.uiState.value.eventCount shouldBe 0

            store.events.value = 12
            advanceUntilIdle()
            model.uiState.value.eventCount shouldBe 12
        }

    @Test
    fun `importing passes the chosen duplicate policy and reports the outcome`(): Unit =
        runTest {
            val store = FakeIcsFilesStore()
            val model = viewModel(store)
            advanceUntilIdle()

            model.onImport("content://a.ics")
            advanceUntilIdle()
            store.imports shouldContainExactly listOf("content://a.ics" to IcsDuplicates.REPLACE)
            model.uiState.value.importOutcome shouldBe IcsImportOutcome.Imported(1, 0, 0, 0)

            model.onDuplicatesChanged(IcsDuplicates.SKIP)
            model.onImport("content://b.ics")
            advanceUntilIdle()
            store.imports shouldContainExactly
                listOf("content://a.ics" to IcsDuplicates.REPLACE, "content://b.ics" to IcsDuplicates.SKIP)
        }

    @Test
    fun `exporting reports the outcome and clears the previous import message`(): Unit =
        runTest {
            val store = FakeIcsFilesStore()
            val model = viewModel(store)
            advanceUntilIdle()

            model.onImport("content://a.ics")
            advanceUntilIdle()
            model.uiState.value.importOutcome shouldBe IcsImportOutcome.Imported(1, 0, 0, 0)

            model.onExport("content://out.ics")
            advanceUntilIdle()
            // A stale "imported" line beside a fresh export result would read as if both had just happened.
            model.uiState.value.importOutcome shouldBe null
            model.uiState.value.exportOutcome shouldBe IcsExportOutcome.Exported(3)
            store.exports shouldContainExactly listOf("content://out.ics")
        }

    @Test
    fun `a second pick while one is running is ignored, and failures are reported not thrown`(): Unit =
        runTest {
            val gate = CompletableDeferred<IcsImportOutcome>()
            val store = FakeIcsFilesStore(importGate = gate)
            val model = viewModel(store)
            advanceUntilIdle()

            model.onImport("content://first.ics")
            advanceUntilIdle()
            model.uiState.value.busy shouldBe true

            // Both are a transaction over the whole table; letting a second one in would interleave the writes.
            model.onExport("content://out.ics")
            model.onImport("content://second.ics")
            advanceUntilIdle()
            store.exports.shouldContainExactly(emptyList())
            store.imports shouldContainExactly listOf("content://first.ics" to IcsDuplicates.REPLACE)

            gate.complete(IcsImportOutcome.TooLarge)
            advanceUntilIdle()
            model.uiState.value.busy shouldBe false
            model.uiState.value.importOutcome shouldBe IcsImportOutcome.TooLarge
        }

    @Test
    fun `a store that throws leaves the page usable`(): Unit =
        runTest {
            val model = viewModel(ThrowingIcsFilesStore)
            advanceUntilIdle()

            model.onImport("content://a.ics")
            advanceUntilIdle()
            model.uiState.value.importOutcome shouldBe IcsImportOutcome.Failed
            model.uiState.value.busy shouldBe false

            model.onExport("content://out.ics")
            advanceUntilIdle()
            model.uiState.value.exportOutcome shouldBe IcsExportOutcome.Failed
            model.uiState.value.busy shouldBe false
        }
}

/** Records what the page asked for and answers with fixed outcomes. */
internal class FakeIcsFilesStore(
    val events: MutableStateFlow<Int> = MutableStateFlow(0),
    private val importGate: CompletableDeferred<IcsImportOutcome>? = null,
) : IcsFilesStore {
    val imports = mutableListOf<Pair<String, IcsDuplicates>>()
    val exports = mutableListOf<String>()

    override fun eventCount(): Flow<Int> = events

    override suspend fun import(
        uri: String,
        duplicates: IcsDuplicates,
    ): IcsImportOutcome {
        imports += uri to duplicates
        return importGate?.await() ?: IcsImportOutcome.Imported(created = 1, replaced = 0, skipped = 0, problems = 0)
    }

    override suspend fun export(uri: String): IcsExportOutcome {
        exports += uri
        return IcsExportOutcome.Exported(3)
    }
}

private object ThrowingIcsFilesStore : IcsFilesStore {
    override fun eventCount(): Flow<Int> = MutableStateFlow(1)

    override suspend fun import(
        uri: String,
        duplicates: IcsDuplicates,
    ): IcsImportOutcome = error("the document provider died")

    override suspend fun export(uri: String): IcsExportOutcome = error("the document provider died")
}
