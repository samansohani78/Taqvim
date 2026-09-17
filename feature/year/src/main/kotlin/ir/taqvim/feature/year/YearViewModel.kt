/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The year view (T-805): the shown calendar and year, the zoom level and the year selection, with the holidays of the
 * shown year. Actions arrive through [onAction]; opening a month leaves through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModel(
    settingsSource: YearSettingsSource,
    todaySource: YearTodaySource,
    private val daysSource: YearDaysSource,
) : ViewModel() {
    private val navigation = MutableStateFlow(YearNavigationState())
    private val effectChannel = Channel<YearEffect>(Channel.BUFFERED)

    private val today: StateFlow<Jdn?> = todaySource.today().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val calendars: StateFlow<YearCalendars?> =
        settingsSource
            .settings()
            .distinctUntilChanged()
            .map { YearCalendars(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val shown: Flow<ShownYear> =
        combine(today.filterNotNull(), calendars.filterNotNull(), navigation) { today, calendars, state ->
            ShownYear.of(today, calendars, state)
        }.distinctUntilChanged()

    /** Flags of the shown year; a new year emits `null` until its days load. */
    private val days: Flow<LoadedDays?> =
        shown
            .map { YearKey(it.calendars, it.index, it.year) }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                daysSource
                    .days(key.calendars.yearDays(key.index, key.year))
                    .map<List<YearDay>, LoadedDays?> { LoadedDays(key, it.toImmutableList()) }
                    .onStart { emit(null) }
            }.onStart { emit(null) }

    /** One-shot navigation effects. */
    val effects: Flow<YearEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<YearUiState> =
        combine(shown, days) { shown, days -> YearUiState(shown.content(days)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), YearUiState())

    fun onAction(action: YearAction) {
        when (action) {
            is YearAction.Navigation -> navigate(action)
            is YearAction.View -> changeView(action)
            is YearAction.OpenMonth -> effectChannel.trySend(YearEffect.NavigateToMonth(action.firstDay))
        }
    }

    private fun navigate(action: YearAction.Navigation) {
        when (action) {
            is YearAction.SelectCalendar -> navigation.update { it.copy(calendarIndex = action.index) }
            is YearAction.ShowYear -> showYear { action.year }
            YearAction.ShowNextYear -> showYear { it + 1 }
            YearAction.ShowPreviousYear -> showYear { it - 1 }
            YearAction.GoToToday -> navigation.update { it.copy(anchorDay = null, isPickingYear = false) }
        }
    }

    private fun changeView(action: YearAction.View) {
        when (action) {
            YearAction.ZoomIn -> navigation.update { it.copy(columns = zoomed(it.columns - 1)) }
            YearAction.ZoomOut -> navigation.update { it.copy(columns = zoomed(it.columns + 1)) }
            YearAction.OpenYearPicker -> navigation.update { it.copy(isPickingYear = true) }
            YearAction.CloseYearPicker -> navigation.update { it.copy(isPickingYear = false) }
        }
    }

    /** Moves the anchor to the first day of the year [target] gives for the shown year; ignored before loading. */
    private fun showYear(target: (Int) -> Int) {
        val today = today.value ?: return
        val calendars = calendars.value ?: return
        navigation.update { state ->
            val current = ShownYear.of(today, calendars, state)
            state.copy(anchorDay = calendars.yearStart(current.index, target(current.year)), isPickingYear = false)
        }
    }

    private fun zoomed(columns: Int): Int = columns.coerceIn(YearZoom.MIN_COLUMNS, YearZoom.MAX_COLUMNS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** What the user moved to; `null` fields follow today. */
private data class YearNavigationState(
    val calendarIndex: Int = 0,
    val anchorDay: Jdn? = null,
    val columns: Int = YearZoom.DEFAULT_COLUMNS,
    val isPickingYear: Boolean = false,
)

/** The days of [year] in calendar [index] of [calendars]. */
private data class YearKey(
    val calendars: YearCalendars,
    val index: Int,
    val year: Int,
)

private data class LoadedDays(
    val key: YearKey,
    val days: ImmutableList<YearDay>,
)

/** The navigation state resolved against today and the available calendars. */
private data class ShownYear(
    val today: Jdn,
    val calendars: YearCalendars,
    val state: YearNavigationState,
    val index: Int,
    val anchor: Jdn,
    val year: Int,
) {
    fun content(days: LoadedDays?): YearContent =
        YearContent(
            today = today,
            calendars = calendars.systems.toImmutableList(),
            calendarIndex = index,
            anchorDay = anchor,
            year = year,
            columns = state.columns,
            isPickingYear = state.isPickingYear,
            weekStart = calendars.settings.weekStart,
            islamicVariant = calendars.settings.islamicVariant,
            languageCode = calendars.settings.languageCode,
            days = days?.takeIf { it.key == YearKey(calendars, index, year) }?.days,
            islamicOverrides = calendars.settings.islamicOverrides,
        )

    companion object {
        fun of(
            today: Jdn,
            calendars: YearCalendars,
            state: YearNavigationState,
        ): ShownYear {
            val index = calendars.clampIndex(state.calendarIndex)
            val anchor = state.anchorDay ?: today
            return ShownYear(today, calendars, state, index, anchor, calendars.yearOf(index, anchor))
        }
    }
}
