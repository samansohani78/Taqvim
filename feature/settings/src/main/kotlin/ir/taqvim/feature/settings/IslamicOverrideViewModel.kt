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

/** State of the official Islamic dates page (ADR-0037); [status] is `null` while it loads. */
data class IslamicOverrideUiState(
    val status: IslamicOverrideStatus? = null,
    val busy: Boolean = false,
    /** The outcome of the last import, or `null`. */
    val importResult: OverrideImportResult? = null,
    /** Whether the last change could not be stored. */
    val changeFailed: Boolean = false,
)

/** User actions of the official Islamic dates page. */
@Immutable
data class IslamicOverrideActions(
    val onOfficialChanged: (Boolean) -> Unit = {},
    /** The user picked the file at this content address. */
    val onImport: (String) -> Unit = {},
    val onRemove: () -> Unit = {},
)

/** Switches the bundled official dates on or off, imports and removes override files through [store]. */
class IslamicOverrideViewModel(
    private val store: IslamicOverrideStore,
) : ViewModel() {
    private val state = MutableStateFlow(IslamicOverrideUiState())
    val uiState: StateFlow<IslamicOverrideUiState> = state.asStateFlow()

    init {
        store.status().onEach { status -> state.update { it.copy(status = status) } }.launchIn(viewModelScope)
    }

    fun onOfficialChanged(enabled: Boolean) {
        run { store.setOfficial(enabled) }
    }

    fun onRemove() {
        run { store.remove() }
    }

    fun onImport(uri: String) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, importResult = null, changeFailed = false) }
        viewModelScope
            .launch {
                val result = attempt { store.import(uri) }.getOrDefault(OverrideImportResult.UNREADABLE)
                state.update { it.copy(importResult = result) }
            }.invokeOnCompletion { state.update { it.copy(busy = false) } }
    }

    /** Runs a stored change; a failure is shown so the user can try again, a cancellation stays silent. */
    private fun run(change: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, importResult = null, changeFailed = false) }
        viewModelScope
            .launch {
                if (attempt { change() }.isFailure) state.update { it.copy(changeFailed = true) }
            }.invokeOnCompletion { state.update { it.copy(busy = false) } }
    }
}
