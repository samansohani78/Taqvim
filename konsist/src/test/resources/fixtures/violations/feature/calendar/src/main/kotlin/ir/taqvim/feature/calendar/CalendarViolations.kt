/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.lifecycle.ViewModel
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.feature.month.MonthScreen
import kotlinx.coroutines.flow.MutableStateFlow

class CalendarScreenModel : ViewModel()

class CalendarViewModel(
    private val repository: EventsRepository,
) : ViewModel() {
    val state: MutableStateFlow<CalendarUiState> = MutableStateFlow(CalendarUiState(MonthScreen.TITLE))
}

class PretendViewModel

class CalendarUiState(
    var title: String,
    val selected: MutableList<Int> = mutableListOf(),
)
