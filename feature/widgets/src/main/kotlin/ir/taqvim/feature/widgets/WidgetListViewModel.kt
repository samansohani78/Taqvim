/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The placed widgets, for the widget settings screen (T-1200).
 *
 * The list is re-read on [refresh] rather than observed: the framework has no change feed for placed widgets, and the
 * screen is reached from settings, so reading it when the screen appears is both enough and honest.
 */
class WidgetListViewModel(
    private val installed: InstalledWidgets,
) : ViewModel() {
    private val state = MutableStateFlow(WidgetListUiState())

    val uiState: StateFlow<WidgetListUiState> = state.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the placed widgets; the screen calls it when it is shown again after a configuration. */
    fun refresh() {
        viewModelScope.launch {
            val placed =
                installed
                    .installed()
                    .flatMap { (kind, ids) -> ids.map { PlacedWidget(it, kind) } }
                    .sortedWith(compareBy({ it.kind.ordinal }, { it.appWidgetId }))
            state.value = WidgetListUiState(loading = false, placed = placed.toImmutableList())
        }
    }
}
