/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The Times tab (T-1100): times of the selected day at the chosen place, the countdown to the next time (refreshed at
 * every minute boundary), expand/collapse and the monthly report of the selected day's month.
 */
class TimesViewModel(
    settingsSource: TimesSettingsSource,
    private val clock: Clock,
) : ViewModel() {
    private val dayOffset = MutableStateFlow(0L)
    private val expanded = MutableStateFlow(false)
    private val latestSettings = MutableStateFlow<TimesSettings?>(null)

    val uiState: StateFlow<TimesUiState> =
        combine(
            settingsSource.settings().onEach { latestSettings.value = it },
            dayOffset,
            expanded,
            minuteTicks(clock),
        ) { settings, offset, isExpanded, now ->
            TimesStateMapper.map(settings, offset, now, isExpanded)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TimesUiState())

    fun onPreviousDay() {
        dayOffset.update { it - 1 }
    }

    fun onNextDay() {
        dayOffset.update { it + 1 }
    }

    fun onToday() {
        dayOffset.value = 0
    }

    fun onToggleExpanded() {
        expanded.update { !it }
    }

    /** The report of the selected day's month, or `null` while no place is known. */
    fun monthlyReport(): MonthlyReport? {
        val settings = latestSettings.value ?: return null
        val day = clock.now().toJdn(settings.timeZone) + dayOffset.value
        return MonthlyReportBuilder.forDay(day, settings)
    }

    /** The printable HTML of [monthlyReport] with [labels], or `null` while no place is known. */
    fun monthlyReportHtml(labels: ReportLabels): String? {
        val settings = latestSettings.value ?: return null
        return monthlyReport()?.let { ReportHtml.render(it, labels, settings) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

/** [clock]'s time now and then at every following minute boundary. */
internal fun minuteTicks(clock: Clock): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
        }
    }
