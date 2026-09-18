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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** The day selected when the screen opens (a link, the year view or search); `null` follows today. */
    initialDay: Jdn? = null,
    /** Reminders before official events (T-1002). */
    private val officialReminders: OfficialReminderStore = OfficialReminderStore.NONE,
) : ViewModel() {
    /** Month arithmetic runs one action at a time, so the actions keep the order they arrived in. */
    private val monthLock = Mutex()

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
            .mapLatest<OverviewInput, DayOverview?> { input ->
                CalendarRangeGuard.orNull("overview of ${input.day.value}") {
                    DayDetailsCalculator.overview(input.day, input.today, input.calendars, input.place)
                }
            }.flowOn(calculationDispatcher)
            .onStart { emit(null) }

    private val times: Flow<DayTimesState> =
        combine(selectedDay, place.filterNotNull(), nowSource.now()) { day, at, now -> TimesInput(day, at.place, now) }
            .mapLatest { input ->
                val place = input.place ?: return@mapLatest DayTimesState.NoPlace
                CalendarRangeGuard
                    .orNull("times of ${input.day.value}") {
                        DayTimesState.Ready(DayDetailsCalculator.times(input.day, place, input.now))
                    } ?: DayTimesState.Loading
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
            CalendarUiState(
                calendarContent(today, calendars, state, search, loaded, menu).copy(officialReminders = reminders),
            )
            // Off the main thread (BUG-2): building the content converts the day into every chosen calendar, and the
            // first day of an Islamic month block is computed from the ephemeris (ADR-0027, ADR-0028) — 20 to 380 ms
            // per new block on a desktop JVM, several times that on a phone.
        }.flowOn(calculationDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState())

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
        // Off the main thread for the same reason as [showMonth]: the picked month may not be computed yet.
        viewModelScope.launch(calculationDispatcher) {
            monthLock.withLock { select(calendars.primaryDay(action.year, action.month, action.day)) }
        }
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
        val safe = inRangeDay(day)
        if (safe != day) emit(CalendarEffect.ShowSnackbar(CalendarMessage.DATE_OUT_OF_RANGE))
        navigation.update { it.copy(selectedDay = safe, shownDay = safe, tab = tab ?: it.tab, sourceEvent = null) }
    }

    /**
     * [day] itself, or the closest day the shown calendars can express (BUG-1). Days are clamped only once today and
     * the settings have loaded; [content] clamps again, so nothing outside the range reaches the screen either way.
     */
    private fun inRangeDay(day: Jdn): Jdn {
        val calendars = calendars.value ?: return day
        val today = today.value ?: return day
        return CalendarRangeGuard.nearestValid(calendars, day, today)
    }

    /**
     * Shows the month at [offset] of the current pager position; ignored until today and the settings load. The month
     * arithmetic runs on [calculationDispatcher] (BUG-2): a month far from today costs tens of milliseconds in a
     * calendar computed from the ephemeris, which must not land on the main thread. [monthLock] keeps the actions in
     * the order they arrived.
     */
    private fun showMonth(offset: (current: Int) -> Int) {
        val today = today.value
        val calendars = calendars.value
        if (today == null || calendars == null) return
        viewModelScope.launch(
            calculationDispatcher,
        ) { monthLock.withLock { updateShownMonth(today, calendars, offset) } }
    }

    private fun updateShownMonth(
        today: Jdn,
        calendars: CalendarCalendars,
        offset: (current: Int) -> Int,
    ) {
        navigation.update { state ->
            val shown = state.shownDay ?: state.selectedDay ?: today
            val wanted =
                CalendarRangeGuard.orNull("month start") {
                    calendars.monthStartAt(today, offset(calendars.monthOffset(today, shown)))
                }
            state.copy(shownDay = wanted?.let { CalendarRangeGuard.nearestValid(calendars, it, today) } ?: shown)
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

    /** The months the pager needs: those around [offset] months from the month of [today]. */
    internal data class MonthWindow(
        val today: Jdn,
        val calendars: CalendarCalendars,
        val offset: Int,
    )

    internal data class LoadedPlace(
        val place: CalendarPlace?,
    )

    internal data class OverviewInput(
        val day: Jdn,
        val today: Jdn,
        val calendars: CalendarCalendars,
        val place: CalendarPlace?,
    )

    internal data class TimesInput(
        val day: Jdn,
        val place: CalendarPlace?,
        val now: Instant,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Months loaded on each side of the shown one (the pager composes its neighbours). */
        const val PREFETCH_MONTHS = 1
        val SEARCH_DEBOUNCE = 200.milliseconds

        fun CalendarDay.toDetails(): DayDetails = DayDetails(jdn, isHoliday, isWeekend, events.toImmutableList())
    }
}
