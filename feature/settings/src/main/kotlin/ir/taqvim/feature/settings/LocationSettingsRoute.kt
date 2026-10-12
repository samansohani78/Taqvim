/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the location settings; `:app` provides [LocationSettingsStore], [CitySearch] and
 * [PlaceDescriber].
 */
val locationSettingsFeatureModule: Module =
    module {
        single<TimeZoneIds> { PlatformTimeZoneIds }
        viewModelOf(::LocationSettingsViewModel)
    }

/**
 * The location settings bound to their [LocationSettingsViewModel]. Taqvim holds no location permission and never
 * asks for one (ADR-0055): a place is a city from the bundled catalog, a point on the map or typed coordinates.
 * [embedded] leaves out the title bar, for a page of another screen (the T-1501 onboarding). [onPickOnMap] opens the
 * world map to pick the place; without it the coordinates page offers no map.
 */
@Composable
fun LocationSettingsRoute(
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    onPickOnMap: (() -> Unit)? = null,
    viewModel: LocationSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onPickOnMap) {
            LocationSettingsActions(
                onModeSelected = viewModel::onModeSelected,
                onQueryChanged = viewModel::onQueryChanged,
                onCitySelected = viewModel::onCitySelected,
                onRetrySearch = viewModel::onRetrySearch,
                onLatitudeChanged = viewModel::onLatitudeChanged,
                onLongitudeChanged = viewModel::onLongitudeChanged,
                onTimeZoneChanged = viewModel::onTimeZoneChanged,
                onSaveCoordinates = viewModel::onSaveCoordinates,
                onPickOnMap = onPickOnMap,
            )
        }
    LocationSettingsScreen(state, actions, modifier, embedded)
}
