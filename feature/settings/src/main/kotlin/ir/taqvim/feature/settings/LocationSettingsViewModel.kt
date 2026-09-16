/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.attempt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Location settings (T-1502): choose the place by searching the city catalog, from the device position, or by typing
 * coordinates and a time zone (with a live suggestion of name and zone). Every choice is stored immediately.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LocationSettingsViewModel(
    private val store: LocationSettingsStore,
    private val citySearch: CitySearch,
    private val deviceLocation: DeviceLocation,
    private val describer: PlaceDescriber,
    private val timeZoneIds: TimeZoneIds,
) : ViewModel() {
    private val state = MutableStateFlow(LocationSettingsUiState())
    val uiState: StateFlow<LocationSettingsUiState> = state.asStateFlow()

    private val settings = MutableStateFlow<LocationSettings?>(null)
    private val query = MutableStateFlow(SearchRequest(""))
    private val options = MutableStateFlow<List<CityOption>>(emptyList())
    private val typedCoordinates = MutableStateFlow<Coordinates?>(null)
    private val zoneEdited = MutableStateFlow(false)

    private val numerals: NumeralSystem
        get() = settings.value?.language?.numerals ?: NumeralSystem.LATIN

    init {
        store.settings().onEach(::onSettings).launchIn(viewModelScope)
        query.debounce(SEARCH_DEBOUNCE_MILLIS).mapLatest(::search).launchIn(viewModelScope)
        typedCoordinates
            .debounce(DESCRIBE_DEBOUNCE_MILLIS)
            .filterNotNull()
            .mapLatest { attempt { describer.describe(it) }.getOrNull() }
            .filterNotNull()
            .onEach(::onDescribed)
            .launchIn(viewModelScope)
    }

    fun onModeSelected(mode: LocationMode) {
        state.update { it.copy(mode = mode) }
    }

    fun onQueryChanged(text: String) {
        query.value = SearchRequest(text)
        state.update {
            it.copy(
                search = it.search.copy(query = text, searching = text.isNotBlank(), noResults = false, failed = false),
            )
        }
    }

    /** Runs the failed search for the same text again. */
    fun onRetrySearch() {
        val current = query.value
        query.value = current.copy(attempt = current.attempt + 1)
        state.update { it.copy(search = it.search.copy(searching = current.text.isNotBlank(), failed = false)) }
    }

    fun onCitySelected(id: Long) {
        val option = options.value.firstOrNull { it.id == id } ?: return
        val zone = option.timeZoneId ?: return
        choose(PlaceChoice(PlaceKind.CITY, option.id, option.name, option.coordinates, zone))
    }

    /** Looks up the device position; call it once the location permission is granted. */
    fun onLocate() {
        state.update { it.copy(device = DeviceState.Locating) }
        viewModelScope
            .launch {
                val device = attempt { locate() }.getOrDefault(DeviceState.Unavailable)
                state.update { it.copy(device = device) }
            }.invokeOnCompletion {
                // A cancelled lookup leaves nothing in progress; the button can be pressed again.
                state.update { if (it.device == DeviceState.Locating) it.copy(device = DeviceState.Idle) else it }
            }
    }

    private suspend fun locate(): DeviceState =
        when (val fix = deviceLocation.locate()) {
            is DeviceFix.Found -> saveDevicePlace(fix.coordinates)
            DeviceFix.PermissionDenied -> DeviceState.PermissionDenied
            DeviceFix.LocationDisabled -> DeviceState.LocationDisabled
            DeviceFix.TimedOut -> DeviceState.TimedOut
            DeviceFix.Unavailable -> DeviceState.Unavailable
        }

    /** The user refused the location permission. */
    fun onPermissionDenied() {
        state.update { it.copy(device = DeviceState.PermissionDenied) }
    }

    fun onLatitudeChanged(text: String) {
        updateManual { it.copy(latitude = text, latitudeError = text.isNotBlank() && latitude(text) == null) }
    }

    fun onLongitudeChanged(text: String) {
        updateManual { it.copy(longitude = text, longitudeError = text.isNotBlank() && longitude(text) == null) }
    }

    fun onTimeZoneChanged(text: String) {
        zoneEdited.value = true
        updateManual { it.copy(timeZoneId = text, timeZoneError = text.isNotBlank() && !isKnownZone(text)) }
    }

    fun onSaveCoordinates() {
        val manual = state.value.manual
        val coordinates = CoordinateInput.coordinates(manual.latitude, manual.longitude)
        val zone = manual.timeZoneId.trim()
        if (coordinates == null || !isKnownZone(zone)) {
            state.update {
                it.copy(
                    manual =
                        manual.copy(
                            latitudeError = latitude(manual.latitude) == null,
                            longitudeError = longitude(manual.longitude) == null,
                            timeZoneError = !isKnownZone(zone),
                        ),
                )
            }
            return
        }
        choose(PlaceChoice(PlaceKind.COORDINATES, null, manual.suggestedName, coordinates, zone)) {
            state.update { it.copy(manual = it.manual.copy(saved = true)) }
        }
    }

    /** Stores [place]; a failure is shown so the user can choose it again, a cancellation stays silent. */
    private fun choose(
        place: PlaceChoice,
        onSaved: () -> Unit = {},
    ) {
        state.update { it.copy(saveFailed = false) }
        viewModelScope.launch {
            val saved = attempt { store.choose(place) }.isSuccess
            if (saved) onSaved() else state.update { it.copy(saveFailed = true) }
        }
    }

    private fun onSettings(value: LocationSettings) {
        settings.value = value
        val numerals = value.language.numerals
        state.update {
            it.copy(
                loading = false,
                current = LocationStateMapper.current(value.place, numerals),
                search = it.search.copy(results = LocationStateMapper.rows(options.value, numerals, value.place)),
            )
        }
    }

    private suspend fun search(request: SearchRequest) {
        val text = request.text
        val found = if (text.isBlank()) Result.success(emptyList()) else attempt { citySearch.search(text.trim()) }
        val cities = found.getOrDefault(emptyList())
        options.value = cities
        val rows = LocationStateMapper.rows(cities, numerals, settings.value?.place)
        val noResults = found.isSuccess && text.isNotBlank() && cities.isEmpty()
        state.update {
            it.copy(
                search =
                    it.search.copy(searching = false, results = rows, noResults = noResults, failed = found.isFailure),
            )
        }
    }

    private suspend fun saveDevicePlace(coordinates: Coordinates): DeviceState {
        val description = describer.describe(coordinates)
        val place = PlaceChoice(PlaceKind.DEVICE, null, description.name, coordinates, description.timeZoneId)
        store.choose(place)
        return LocationStateMapper.saved(place, numerals)
    }

    private fun onDescribed(description: PlaceDescription) {
        val keepZone = zoneEdited.value
        updateManual { manual ->
            val zone = if (keepZone) manual.timeZoneId else description.timeZoneId
            manual.copy(
                suggestedName = description.name,
                timeZoneId = zone,
                timeZoneError = zone.isNotBlank() && !isKnownZone(zone),
            )
        }
    }

    /** Applies [transform] to the typed fields; a change of the coordinates clears the suggestion and saved flag. */
    private fun updateManual(transform: (ManualState) -> ManualState) {
        val current = state.value.manual
        val next = transform(current)
        val coordinates = CoordinateInput.coordinates(next.latitude, next.longitude)
        val moved = coordinates != typedCoordinates.value
        typedCoordinates.value = coordinates
        val manual =
            next.copy(
                suggestedName = if (moved) null else next.suggestedName,
                saved = next.saved && next == current,
            )
        state.update { it.copy(manual = manual) }
    }

    private fun latitude(text: String): Double? = CoordinateInput.latitude(text)

    private fun longitude(text: String): Double? = CoordinateInput.longitude(text)

    private fun isKnownZone(text: String): Boolean = timeZoneIds.isKnown(text.trim())

    /** A search for [text]; [attempt] changes when the same text is searched again. */
    private data class SearchRequest(
        val text: String,
        val attempt: Int = 0,
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        const val DESCRIBE_DEBOUNCE_MILLIS = 600L
    }
}
