/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.month

import androidx.lifecycle.ViewModel
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonthViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(MonthUiState(title = ""))
    val uiState: StateFlow<MonthUiState> = mutableState.asStateFlow()

    fun select(day: Jdn) {
        mutableState.value = mutableState.value.copy(selected = day)
    }
}

data class MonthUiState(
    val title: String,
    val selected: Jdn? = null,
    val days: List<Jdn> = emptyList(),
)
