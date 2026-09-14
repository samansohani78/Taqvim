/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The ways of choosing a place, one tab each. */
enum class LocationMode {
    CITY,
    DEVICE,
    COORDINATES,
}

/** State of the location settings (T-1502). */
data class LocationSettingsUiState(
    val loading: Boolean = true,
    /** The stored place, or `null` when none is chosen. */
    val current: CurrentPlace? = null,
    val mode: LocationMode = LocationMode.CITY,
    val search: CitySearchState = CitySearchState(),
    val device: DeviceState = DeviceState.Idle,
    val manual: ManualState = ManualState(),
)

/** The stored place, formatted for display. */
data class CurrentPlace(
    val kind: PlaceKind,
    val name: String?,
    /** Latitude and longitude in the app's digits. */
    val coordinates: String,
    val timeZoneId: String,
)

/** The city search tab. */
data class CitySearchState(
    val query: String = "",
    val searching: Boolean = false,
    val results: ImmutableList<CityRow> = persistentListOf(),
    /** Whether the finished search for a non-blank [query] found nothing. */
    val noResults: Boolean = false,
)

/** A search result. */
data class CityRow(
    val id: Long,
    val name: String,
    val detail: String?,
    val coordinates: String,
    /** Whether the city has a time zone and so can be chosen. */
    val selectable: Boolean,
    /** Whether it is the stored place. */
    val selected: Boolean,
)

/** The device location tab. */
sealed interface DeviceState {
    data object Idle : DeviceState

    data object Locating : DeviceState

    data object PermissionDenied : DeviceState

    data object LocationDisabled : DeviceState

    data object TimedOut : DeviceState

    data object Unavailable : DeviceState

    /** The device position was found and stored. */
    data class Saved(
        val name: String?,
        val coordinates: String,
        val timeZoneId: String,
    ) : DeviceState
}

/** The typed coordinates tab; the error flags refer to the current field texts. */
data class ManualState(
    val latitude: String = "",
    val longitude: String = "",
    val timeZoneId: String = "",
    val latitudeError: Boolean = false,
    val longitudeError: Boolean = false,
    val timeZoneError: Boolean = false,
    /** Name suggested for the typed coordinates, or `null`. */
    val suggestedName: String? = null,
    val saved: Boolean = false,
)

/** User actions of the location settings. */
@Immutable
data class LocationSettingsActions(
    val onModeSelected: (LocationMode) -> Unit = {},
    val onQueryChanged: (String) -> Unit = {},
    val onCitySelected: (Long) -> Unit = {},
    val onUseDeviceLocation: () -> Unit = {},
    val onLatitudeChanged: (String) -> Unit = {},
    val onLongitudeChanged: (String) -> Unit = {},
    val onTimeZoneChanged: (String) -> Unit = {},
    val onSaveCoordinates: () -> Unit = {},
)
