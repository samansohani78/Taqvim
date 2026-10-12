/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.attempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the calendar files page (T-1003); [eventCount] is `null` while it loads. */
data class IcsFilesUiState(
    val eventCount: Int? = null,
    val duplicates: IcsDuplicates = IcsDuplicates.REPLACE,
    val busy: Boolean = false,
    /** The outcome of the last import, or `null`. */
    val importOutcome: IcsImportOutcome? = null,
    /** The outcome of the last export, or `null`. */
    val exportOutcome: IcsExportOutcome? = null,
)

/** User actions of the calendar files page; the route turns the two pick actions into file-picker launches. */
@Immutable
data class IcsFilesActions(
    val onDuplicatesChanged: (IcsDuplicates) -> Unit = {},
    /** The user picked the file to import, at this content address. */
    val onImport: (String) -> Unit = {},
    /** The user picked where to export, at this content address. */
    val onExport: (String) -> Unit = {},
)

/**
 * Imports and exports personal events as `.ics` files through [store].
 *
 * One operation runs at a time: both are a transaction over the whole personal-event table, and a second pick while
 * the first is still writing would interleave with it. The picker can be opened again as soon as [busy] clears.
 */
class IcsFilesViewModel(
    private val store: IcsFilesStore,
) : ViewModel() {
    private val state = MutableStateFlow(IcsFilesUiState())
    val uiState: StateFlow<IcsFilesUiState> = state.asStateFlow()

    init {
        store.eventCount().onEach { count -> state.update { it.copy(eventCount = count) } }.launchIn(viewModelScope)
    }

    fun onDuplicatesChanged(duplicates: IcsDuplicates) {
        state.update { it.copy(duplicates = duplicates) }
    }

    fun onImport(uri: String) {
        start {
            val outcome = attempt { store.import(uri, state.value.duplicates) }.getOrDefault(IcsImportOutcome.Failed)
            state.update { it.copy(importOutcome = outcome) }
        }
    }

    fun onExport(uri: String) {
        start {
            val outcome = attempt { store.export(uri) }.getOrDefault(IcsExportOutcome.Failed)
            state.update { it.copy(exportOutcome = outcome) }
        }
    }

    /** Runs [work] while nothing else is, clearing the previous outcomes so the page never shows a stale one. */
    private fun start(work: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, importOutcome = null, exportOutcome = null) }
        viewModelScope
            .launch { work() }
            .invokeOnCompletion { state.update { it.copy(busy = false) } }
    }
}
