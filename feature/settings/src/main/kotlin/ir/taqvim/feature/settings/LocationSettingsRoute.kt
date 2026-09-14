/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the location settings; `:app` provides [LocationSettingsStore], [CitySearch], [DeviceLocation] and
 * [PlaceDescriber].
 */
val locationSettingsFeatureModule: Module =
    module {
        single<TimeZoneIds> { PlatformTimeZoneIds }
        viewModelOf(::LocationSettingsViewModel)
    }

/** The location settings bound to their [LocationSettingsViewModel]; asks for location permission when needed. */
@Composable
fun LocationSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: LocationSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) viewModel.onLocate() else viewModel.onPermissionDenied()
        }
    val actions =
        remember(viewModel, context, permissionLauncher) {
            LocationSettingsActions(
                onModeSelected = viewModel::onModeSelected,
                onQueryChanged = viewModel::onQueryChanged,
                onCitySelected = viewModel::onCitySelected,
                onUseDeviceLocation = {
                    if (hasLocationPermission(context)) {
                        viewModel.onLocate()
                    } else {
                        permissionLauncher.launch(locationPermissions())
                    }
                },
                onLatitudeChanged = viewModel::onLatitudeChanged,
                onLongitudeChanged = viewModel::onLongitudeChanged,
                onTimeZoneChanged = viewModel::onTimeZoneChanged,
                onSaveCoordinates = viewModel::onSaveCoordinates,
            )
        }
    LocationSettingsScreen(state, actions, modifier)
}

/** The permissions asked for: fine, or coarse if the user grants only that. */
internal fun locationPermissions(): Array<String> =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

internal fun hasLocationPermission(context: Context): Boolean =
    locationPermissions().any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
