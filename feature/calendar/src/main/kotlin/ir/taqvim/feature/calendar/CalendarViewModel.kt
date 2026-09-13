/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Jdn
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.launch

/**
 * The calendar (home) screen (T-800): selected day, shown month, day-details tab, day events and search. Actions
 * arrive through [onAction]; navigation and snackbars leave through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    settingsSource: CalendarSettingsSource,
    todaySource: TodaySource,
    private val daySource: CalendarDaySource,
    private val monthSource: CalendarMonthSource,
    private val searchEvents: SearchEventsUseCase,
) : ViewModel() {
    private val navigation = MutableStateFlow(NavigationState())
    private val search = MutableStateFlow(CalendarSearch())
    private val effectChannel = Channel<CalendarEffect>(Channel.BUFFERED)
    private var searchJob: Job? = null

    private val today: StateFlow<Jdn?> = todaySource.today().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val calendars: StateFlow<CalendarCalendars?> =
        settingsSource
            .settings()
            .distinctUntilChanged()
            .map { CalendarCalendars(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dayDetails: Flow<DayDetails?> =
        combine(today.filterNotNull(), navigation) { today, state -> state.selectedDay ?: today }
            .distinctUntilChanged()
            .flatMapLatest { day ->
                daySource.day(day).map<CalendarDay, DayDetails?> { it.toDetails() }.onStart { emit(null) }
            }

    /** Events of the shown month and its neighbours (T-801); a new window keeps the previous one until it loads. */
    private val months: Flow<ImmutableList<MonthEvents>> =
        combine(today.filterNotNull(), calendars.filterNotNull(), navigation) { today, calendars, state ->
            MonthWindow(today, calendars, calendars.monthOffset(today, state.shownDay ?: state.selectedDay ?: today))
        }.distinctUntilChanged()
            .flatMapLatest { monthEvents(it) }
            .onStart { emit(persistentListOf()) }

    /** One-shot navigation and snackbar effects. */
    val effects: Flow<CalendarEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<CalendarUiState> =
        combine(
            today.filterNotNull(),
            calendars.filterNotNull(),
            navigation,
            search,
            combine(dayDetails, months) { details, months -> Loaded(details, months) },
        ) { today, calendars, state, search, loaded ->
            CalendarUiState(content(today, calendars, state, search, loaded))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState())

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.Navigation -> navigate(action)
            is CalendarAction.Search -> onSearch(action)
            is CalendarAction.CreateEvent -> emit(CalendarEffect.NavigateToEventEditor(action.jdn))
            is CalendarAction.OpenEvent -> emit(CalendarEffect.NavigateToEvent(action.event))
            is CalendarAction.OpenWeek -> emit(CalendarEffect.NavigateToTimeline(action.firstDay))
        }
    }

    /** The grid days of the months either side of [window]'s month, with their events, in offset order. */
    private fun monthEvents(window: MonthWindow): Flow<ImmutableList<MonthEvents>> {
        val weekStart = window.calendars.settings.weekStart
        val pages =
            (window.offset - PREFETCH_MONTHS..window.offset + PREFETCH_MONTHS).map { offset ->
                val monthStart = window.calendars.monthStartAt(window.today, offset)
                monthSource
                    .days(MonthLayout.gridDays(monthStart, weekStart))
                    .map { MonthEvents(offset, it.toImmutableList()) }
            }
        return combine(pages) { it.toList().toImmutableList() }
    }

    private fun navigate(action: CalendarAction.Navigation) {
        when (action) {
            is CalendarAction.SelectDay -> select(action.jdn)
            is CalendarAction.ShowMonth -> showMonth { action.offset }
            CalendarAction.ShowNextMonth -> showMonth { it + 1 }
            CalendarAction.ShowPreviousMonth -> showMonth { it - 1 }
            CalendarAction.GoToToday -> navigation.update { it.copy(selectedDay = null, shownDay = null) }
            is CalendarAction.SelectTab -> navigation.update { it.copy(tab = action.tab) }
        }
    }

    private fun onSearch(action: CalendarAction.Search) {
        when (action) {
            CalendarAction.OpenSearch -> search.update { it.copy(isOpen = true) }
            is CalendarAction.ChangeSearchQuery -> changeQuery(action.query)
            CalendarAction.CloseSearch -> closeSearch()
            is CalendarAction.OpenSearchResult -> openResult(action.result)
        }
    }

    private fun select(
        day: Jdn,
        tab: DayDetailsTab? = null,
    ) {
        navigation.update { it.copy(selectedDay = day, shownDay = day, tab = tab ?: it.tab) }
    }

    /** Shows the month at [offset] of the current pager position; ignored until today and the settings load. */
    private fun showMonth(offset: (current: Int) -> Int) {
        val today = today.value
        val calendars = calendars.value
        if (today == null || calendars == null) return
        navigation.update { state ->
            val shown = state.shownDay ?: state.selectedDay ?: today
            state.copy(shownDay = calendars.monthStartAt(today, offset(calendars.monthOffset(today, shown))))
        }
    }

    private fun changeQuery(query: String) {
        search.update { it.copy(isOpen = true, query = query, isSearching = query.isNotBlank()) }
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE)
                val results = searchEvents(query).toImmutableList()
                search.update { if (it.query == query) it.copy(isSearching = false, results = results) else it }
            }
    }

    private fun closeSearch() {
        searchJob?.cancel()
        search.value = CalendarSearch()
    }

    private fun openResult(result: EventSearchResult) {
        val day = result.nextDay
        if (day == null) {
            emit(CalendarEffect.ShowSnackbar(CalendarMessage.NO_UPCOMING_OCCURRENCE))
        } else {
            closeSearch()
            select(day, DayDetailsTab.EVENTS)
        }
    }

    private fun emit(effect: CalendarEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun content(
        today: Jdn,
        calendars: CalendarCalendars,
        state: NavigationState,
        search: CalendarSearch,
        loaded: Loaded,
    ): CalendarContent {
        val selected = state.selectedDay ?: today
        val shown = state.shownDay ?: selected
        return CalendarContent(
            today = today,
            selectedDay = selected,
            calendars = calendars.systems.toImmutableList(),
            selectedDates = calendars.datesOf(selected).toImmutableList(),
            monthOffset = calendars.monthOffset(today, shown),
            visibleMonth = calendars.monthStart(shown),
            weekStart = calendars.settings.weekStart,
            selectedTab = state.tab,
            dayDetails = loaded.details?.takeIf { it.jdn == selected },
            search = search,
            islamicVariant = calendars.settings.islamicVariant,
            languageCode = calendars.settings.languageCode,
            showWeekNumbers = calendars.settings.showWeekNumbers,
            months = loaded.months,
        )
    }

    /** What loads after today and the preferences: the selected day's events and the pager's months. */
    private data class Loaded(
        val details: DayDetails?,
        val months: ImmutableList<MonthEvents>,
    )

    /** The months the pager needs: those around [offset] months from the month of [today]. */
    private data class MonthWindow(
        val today: Jdn,
        val calendars: CalendarCalendars,
        val offset: Int,
    )

    /** `null` days follow today: no explicit selection, or the shown month is the selected day's month. */
    private data class NavigationState(
        val selectedDay: Jdn? = null,
        val shownDay: Jdn? = null,
        val tab: DayDetailsTab = DayDetailsTab.CALENDARS,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Months loaded on each side of the shown one (the pager composes its neighbours). */
        const val PREFETCH_MONTHS = 1
        val SEARCH_DEBOUNCE = 200.milliseconds

        fun CalendarDay.toDetails(): DayDetails = DayDetails(jdn, isHoliday, isWeekend, events.toImmutableList())
    }
}
