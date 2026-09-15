/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ir.taqvim.app.R
import ir.taqvim.app.di.MapPlacePicker
import ir.taqvim.app.di.coordinatesLabel
import ir.taqvim.feature.map.MapEffect
import ir.taqvim.feature.map.MapRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The world map (T-1301); a point or city marker picked on it is saved as the chosen place once the user confirms, and
 * then [onPlaceSaved] runs (the T-1502 map pick closes the map).
 */
@Composable
internal fun MapPickScreen(
    modifier: Modifier,
    onPlaceSaved: () -> Unit = {},
    picker: MapPlacePicker = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onPlaceSaved)
    var picked by remember { mutableStateOf<MapEffect.LocationPicked?>(null) }
    MapRoute(modifier, onLocationPicked = { picked = it })
    picked?.let { pick ->
        UseAsPlaceDialog(
            pick = pick,
            onConfirm = {
                picked = null
                scope.launch {
                    val city = pick.city
                    val stored = if (city != null) picker.useCity(city) else picker.useAsPlace(pick.coordinates)
                    if (stored) saved()
                }
            },
            onDismiss = { picked = null },
        )
    }
}

/** Asks whether the picked point or city of [pick] should become the chosen place. */
@Composable
internal fun UseAsPlaceDialog(
    pick: MapEffect.LocationPicked,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message =
        pick.city?.let { stringResource(R.string.map_use_city_message, it.name) }
            ?: stringResource(R.string.map_use_as_place_message, coordinatesLabel(pick.coordinates))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_use_as_place_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.map_use_as_place_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_use_as_place_cancel)) } },
    )
}
