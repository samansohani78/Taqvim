/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val MILLIS_PER_MINUTE = 60_000L

/** The current instant now and then at the start of every following minute. */
internal fun minuteTicks(clock: Clock): Flow<Instant> =
    flow {
        while (currentCoroutineContext().isActive) {
            val now = clock.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - Math.floorMod(now.toEpochMilliseconds(), MILLIS_PER_MINUTE))
        }
    }

/** Today on the watch: `null` while loading. */
data class TodayUiState(
    val today: WearToday? = null,
    val placeName: String? = null,
)

/** Today screen state (T-1600), refreshed every minute and after every settings change. */
class TodayViewModel(
    graph: WearGraph,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        combine(graph.setups, minuteTicks(graph.clock)) { setup, now ->
            TodayUiState(graph.calculator.today(setup, now), setup.place?.name)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayUiState())
}

/** A month of the primary calendar, [offset] months from today's month; `null` while loading. */
data class MonthUiState(
    val month: WearMonth? = null,
    val offset: Int = 0,
)

/** Month screen state: today's month, moved with [show]. */
class MonthViewModel(
    graph: WearGraph,
) : ViewModel() {
    private val offset = MutableStateFlow(0)

    val uiState: StateFlow<MonthUiState> =
        combine(graph.setups, offset, minuteTicks(graph.clock)) { setup, months, now ->
            val lookup = graph.calculator.lookup(setup.islamicVariant)
            MonthUiState(WearMonthBuilder.build(setup, lookup, now.toJdn(setup.zone), months), months)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MonthUiState())

    /** Moves the shown month by [months]; `0` returns to today's month. */
    fun show(months: Int) {
        offset.update { if (months == 0) 0 else it + months }
    }
}

/** Converter state: the date being edited in [source] and the same day in the other calendars. */
data class ConverterUiState(
    val source: CalendarSystem? = null,
    val calendars: List<CalendarSystem> = emptyList(),
    val date: CalendarDate? = null,
    val sourceText: String = "",
    val results: List<ConvertedDate> = emptyList(),
)

/** Converter screen state (T-1600): starts at today in the primary calendar. */
class ConverterViewModel(
    private val graph: WearGraph,
) : ViewModel() {
    private val selection = MutableStateFlow<CalendarDate?>(null)

    val uiState: StateFlow<ConverterUiState> =
        combine(graph.setups, selection) { setup, chosen ->
            val calendar = setup.calendars.firstOrNull { it.system == chosen?.system } ?: setup.primary
            val date =
                chosen?.takeIf { it.system == calendar.system } ?: calendar.fromJdn(graph.clock.now().toJdn(setup.zone))
            ConverterUiState(
                source = calendar.system,
                calendars = setup.calendars.map { it.system },
                date = date,
                sourceText =
                    DateFormatter.format(
                        date,
                        calendar.toJdn(date).weekday(),
                        setup.language,
                        DateStyle.LONG,
                        setup.numerals,
                    ),
                results = WearConverter.results(setup, calendar, date),
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConverterUiState())

    /** Moves [field] of the shown date by [delta]. */
    fun step(
        field: ConverterField,
        delta: Int,
    ) {
        viewModelScope.launch {
            val setup = graph.setup()
            val state = uiState.first { it.date != null }
            val date = state.date ?: return@launch
            val calendar = setup.calendars.firstOrNull { it.system == date.system } ?: return@launch
            selection.value = WearConverter.step(calendar, date, field, delta)
        }
    }

    /** Shows the same day in [system]. */
    fun switchCalendar(system: CalendarSystem) {
        viewModelScope.launch {
            val setup = graph.setup()
            val date = uiState.first { it.date != null }.date ?: return@launch
            val from = setup.calendars.firstOrNull { it.system == date.system } ?: return@launch
            val to = setup.calendars.firstOrNull { it.system == system } ?: return@launch
            selection.value = WearConverter.switchCalendar(from, to, date)
        }
    }
}

/** A language the watch offers. */
data class LanguageOption(
    val code: String,
    val name: String,
)

/** Settings state: the current choices and the options to pick from. */
data class WearSettingsUiState(
    val languageCode: String = "",
    val languages: List<LanguageOption> = emptyList(),
    val primaryCalendar: CalendarSystem? = null,
    val prayerMethod: PrayerMethod? = null,
    val placeName: String? = null,
    val cities: List<CityOption> = emptyList(),
)

/** Settings screen state (T-1600): language, primary calendar, prayer method and city, stored on the watch. */
class WearSettingsViewModel(
    private val graph: WearGraph,
) : ViewModel() {
    val uiState: StateFlow<WearSettingsUiState> =
        combine(graph.preferences.preferences, graph.setups) { stored, setup ->
            WearSettingsUiState(
                languageCode = stored.languageCode,
                languages = LanguageTable.languages.map { LanguageOption(it.code, it.nativeName) },
                primaryCalendar = setup.primary.system,
                prayerMethod = stored.prayerMethod,
                placeName = setup.place?.name,
                cities = WearSettingsModel.cityOptions(graph.catalog, setup.language.code),
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WearSettingsUiState())

    fun chooseLanguage(code: String) = update { WearSettingsModel.withLanguage(it, code) }

    fun choosePrimaryCalendar(system: CalendarSystem) = update { WearSettingsModel.withPrimaryCalendar(it, system) }

    fun choosePrayerMethod(method: PrayerMethod) = update { WearSettingsModel.withPrayerMethod(it, method) }

    fun chooseCity(id: Long) {
        viewModelScope.launch(Dispatchers.Default) {
            val city = graph.catalog.city(id) ?: return@launch
            graph.preferences.update { WearSettingsModel.withCity(it, city) }
        }
    }

    private fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { graph.preferences.update(transform) }
    }
}
