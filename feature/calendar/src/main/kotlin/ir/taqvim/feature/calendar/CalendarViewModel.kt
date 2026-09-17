/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.attempt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The calendar (home) screen (T-800): selected day, shown month, day-details tab, day events and search, plus the
 * Calendars and Times tabs of the selected day (T-802). Actions arrive through [onAction]; navigation and snackbars
 * leave through [effects].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    settingsSource: CalendarSettingsSource,
    todaySource: TodaySource,
    private val daySource: CalendarDaySource,
    private val monthSource: CalendarMonthSource,
    private val searchEvents: SearchEventsUseCase,
    placeSource: CalendarPlaceSource,
    nowSource: NowSource,
    private val displayStore: CalendarDisplayStore,
    calculationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** The day selected when the screen opens (a link, the year view or search); `null` follows today. */
    initialDay: Jdn? = null,
    /** Reminders before official events (T-1002). */
    private val officialReminders: OfficialReminderStore = OfficialReminderStore.NONE,
) : ViewModel() {
    private val navigation = MutableStateFlow(NavigationState(selectedDay = initialDay, shownDay = initialDay))
    private val search = MutableStateFlow(CalendarSearch())
    private val menu = MutableStateFlow(CalendarMenu())
    private val effectChannel = Channel<CalendarEffect>(Channel.BUFFERED)
    private var searchJob: Job? = null

    private val today: StateFlow<Jdn?> = todaySource.today().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val calendars: StateFlow<CalendarCalendars?> =
        settingsSource
            .settings()
            .distinctUntilChanged()
            .map { CalendarCalendars(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The chosen place once loaded (its [LoadedPlace.place] is `null` when none is chosen). */
    private val place: StateFlow<LoadedPlace?> =
        placeSource
            .place()
            .map { LoadedPlace(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val selectedDay: Flow<Jdn> =
        combine(today.filterNotNull(), navigation) { today, state -> state.selectedDay ?: today }.distinctUntilChanged()

    private val dayDetails: Flow<DayDetails?> =
        selectedDay.flatMapLatest { day ->
            daySource.day(day).map<CalendarDay, DayDetails?> { it.toDetails() }.onStart { emit(null) }
        }

    /** Events of the shown month and its neighbours (T-801); a new window keeps the previous one until it loads. */
    private val months: Flow<ImmutableList<MonthEvents>> =
        combine(today.filterNotNull(), calendars.filterNotNull(), navigation) { today, calendars, state ->
            MonthWindow(today, calendars, calendars.monthOffset(today, state.shownDay ?: state.selectedDay ?: today))
        }.distinctUntilChanged()
            .flatMapLatest { monthSource.monthEvents(it.today, it.calendars, it.offset, PREFETCH_MONTHS) }
            .onStart { emit(persistentListOf()) }

    private val overview: Flow<DayOverview?> =
        combine(
            selectedDay,
            today.filterNotNull(),
            calendars.filterNotNull(),
            place.filterNotNull(),
        ) { day, today, cal, at ->
            OverviewInput(day, today, cal, at.place)
        }.distinctUntilChanged()
            .mapLatest<OverviewInput, DayOverview?> {
                DayDetailsCalculator.overview(it.day, it.today, it.calendars, it.place)
            }.flowOn(calculationDispatcher)
            .onStart { emit(null) }

    private val times: Flow<DayTimesState> =
        combine(selectedDay, place.filterNotNull(), nowSource.now()) { day, at, now -> TimesInput(day, at.place, now) }
            .mapLatest { input ->
                input.place?.let { DayTimesState.Ready(DayDetailsCalculator.times(input.day, it, input.now)) }
                    ?: DayTimesState.NoPlace
            }.flowOn(calculationDispatcher)
            .distinctUntilChanged()
            .onStart { emit(DayTimesState.Loading) }

    /** Reminder choices of the official event whose source is shown (T-1002). */
    private val reminders: Flow<OfficialReminderChoices?> =
        navigation
            .map { state -> state.sourceEvent?.takeIf { it.kind == DayEventKind.OFFICIAL }?.id }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    officialReminders.daysBefore(id).map { OfficialReminderChoices(id, it.toImmutableSet()) }
                }
            }

    /** One-shot navigation and snackbar effects. */
    val effects: Flow<CalendarEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<CalendarUiState> =
        combine(
            today.filterNotNull(),
            calendars.filterNotNull(),
            navigation,
            combine(search, menu, reminders, ::Triple),
            combine(dayDetails, months, overview, times, ::Loaded),
        ) { today, calendars, state, (search, menu, reminders), loaded ->
            CalendarUiState(content(today, calendars, state, search, loaded, menu).copy(officialReminders = reminders))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState())

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.Navigation -> navigate(action)
            is CalendarAction.Search -> onSearch(action)
            is CalendarAction.Menu -> onMenu(action)
            is CalendarAction.Event -> onEvent(action)
        }
    }

    private fun onEvent(action: CalendarAction.Event) {
        menu.update { it.copy(isOpen = false) }
        when (action) {
            is CalendarAction.CreateEvent -> emit(CalendarEffect.NavigateToEventEditor(action.jdn))
            is CalendarAction.OpenEvent -> emit(CalendarEffect.NavigateToEvent(action.event))
            is CalendarAction.OpenWeek -> emit(CalendarEffect.NavigateToTimeline(action.firstDay))
            is CalendarAction.OpenCitation -> emit(CalendarEffect.OpenUrl(action.url))
            CalendarAction.OpenSearchScreen -> emit(CalendarEffect.NavigateToSearch)
            CalendarAction.OpenShiftWork -> emit(CalendarEffect.NavigateToShiftWork)
            CalendarAction.OpenPlanetaryHours -> openPlanetaryHours()
            CalendarAction.PrintMonth -> emit(CalendarEffect.PrintMonth)
            is CalendarAction.ToggleOfficialReminder -> toggleReminder(action)
        }
    }

    /** Stores a reminder before the shown official event; turning one on asks for the notification permission. */
    private fun toggleReminder(action: CalendarAction.ToggleOfficialReminder) {
        val event = navigation.value.sourceEvent?.takeIf { it.kind == DayEventKind.OFFICIAL } ?: return
        viewModelScope.launch {
            attempt { officialReminders.setReminder(event.id, action.daysBefore, action.enabled) }
                .onSuccess { if (action.enabled) effectChannel.send(CalendarEffect.RequestNotificationPermission) }
                .onFailure { effectChannel.send(CalendarEffect.ShowSnackbar(CalendarMessage.SETTING_NOT_SAVED)) }
        }
    }

    /** The toolbar menu (T-803): dialogs, going to a date and the stored display choices. */
    private fun onMenu(action: CalendarAction.Menu) {
        when (action) {
            CalendarAction.OpenMenu -> {
                menu.update { it.copy(isOpen = true) }
            }

            CalendarAction.DismissMenu -> {
                menu.update { it.copy(isOpen = false) }
            }

            CalendarAction.OpenDatePicker -> {
                menu.value = CalendarMenu(dialog = CalendarDialog.DATE_PICKER)
            }

            CalendarAction.OpenSecondaryCalendarChooser -> {
                menu.value = CalendarMenu(dialog = CalendarDialog.SECONDARY_CALENDAR)
            }

            CalendarAction.DismissDialog -> {
                menu.value = CalendarMenu()
            }

            is CalendarAction.PickDate -> {
                pickDate(action)
            }

            is CalendarAction.ChooseSecondaryCalendar -> {
                store { setSecondaryCalendar(action.system) }
            }

            is CalendarAction.ShowWeekNumbers -> {
                store { setShowWeekNumbers(action.show) }
            }
        }
    }

    private fun pickDate(action: CalendarAction.PickDate) {
        val calendars = calendars.value ?: return
        menu.value = CalendarMenu()
        select(calendars.primaryDay(action.year, action.month, action.day))
    }

    /** Closes the menu and stores a display choice; a failure is reported in a snackbar. */
    private fun store(write: suspend CalendarDisplayStore.() -> Unit) {
        menu.value = CalendarMenu()
        viewModelScope.launch {
            attempt { displayStore.write() }
                .onFailure { effectChannel.send(CalendarEffect.ShowSnackbar(CalendarMessage.SETTING_NOT_SAVED)) }
        }
    }

    private fun openPlanetaryHours() {
        val day = navigation.value.selectedDay ?: today.value ?: return
        emit(CalendarEffect.NavigateToPlanetaryHours(day))
    }

    private fun navigate(action: CalendarAction.Navigation) {
        when (action) {
            is CalendarAction.SelectDay -> select(action.jdn)
            is CalendarAction.ShowMonth -> showMonth { action.offset }
            CalendarAction.ShowNextMonth -> showMonth { it + 1 }
            CalendarAction.ShowPreviousMonth -> showMonth { it - 1 }
            CalendarAction.GoToToday -> navigation.update { NavigationState(tab = it.tab) }
            is CalendarAction.SelectTab -> navigation.update { it.copy(tab = action.tab) }
            is CalendarAction.ShowEventSource -> navigation.update { it.copy(sourceEvent = action.event) }
            CalendarAction.DismissEventSource -> navigation.update { it.copy(sourceEvent = null) }
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
        navigation.update { it.copy(selectedDay = day, shownDay = day, tab = tab ?: it.tab, sourceEvent = null) }
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
        menu: CalendarMenu,
    ): CalendarContent {
        val selected = state.selectedDay ?: today
        val shown = state.shownDay ?: selected
        return CalendarContent(
            today = today,
            selectedDay = selected,
            calendars = calendars.systems.toImmutableList(),
            selectedDates = calendars.datesOf(selected).toImmutableList(),
            selectedOrigins = calendars.originsOf(selected).toImmutableList(),
            monthOffset = calendars.monthOffset(today, shown),
            visibleMonth = calendars.monthStart(shown),
            weekStart = calendars.settings.weekStart,
            selectedTab = state.tab,
            dayDetails = loaded.details?.takeIf { it.jdn == selected },
            search = search,
            islamicVariant = calendars.settings.islamicVariant,
            islamicOverrides = calendars.settings.islamicOverrides,
            languageCode = calendars.settings.languageCode,
            showWeekNumbers = calendars.settings.showWeekNumbers,
            months = loaded.months,
            overview = loaded.overview?.takeIf { it.day == selected },
            times =
                loaded.times.takeIf { it !is DayTimesState.Ready || it.times.day == selected } ?: DayTimesState.Loading,
            sourceEvent = state.sourceEvent,
            menu = menu,
            secondaryChoices = calendars.secondaryChoices.toImmutableList(),
        )
    }

    /** What loads after today and the preferences: the selected day's details and the pager's months. */
    private data class Loaded(
        val details: DayDetails?,
        val months: ImmutableList<MonthEvents>,
        val overview: DayOverview?,
        val times: DayTimesState,
    )

    /** The months the pager needs: those around [offset] months from the month of [today]. */
    private data class MonthWindow(
        val today: Jdn,
        val calendars: CalendarCalendars,
        val offset: Int,
    )

    private data class LoadedPlace(
        val place: CalendarPlace?,
    )

    private data class OverviewInput(
        val day: Jdn,
        val today: Jdn,
        val calendars: CalendarCalendars,
        val place: CalendarPlace?,
    )

    private data class TimesInput(
        val day: Jdn,
        val place: CalendarPlace?,
        val now: Instant,
    )

    /** `null` days follow today: no explicit selection, or the shown month is the selected day's month. */
    private data class NavigationState(
        val selectedDay: Jdn? = null,
        val shownDay: Jdn? = null,
        val tab: DayDetailsTab = DayDetailsTab.CALENDARS,
        val sourceEvent: DayEventItem? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Months loaded on each side of the shown one (the pager composes its neighbours). */
        const val PREFETCH_MONTHS = 1
        val SEARCH_DEBOUNCE = 200.milliseconds

        fun CalendarDay.toDetails(): DayDetails = DayDetails(jdn, isHoliday, isWeekend, events.toImmutableList())
    }
}
