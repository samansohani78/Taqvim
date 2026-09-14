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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ir.taqvim.app.R
import ir.taqvim.app.di.MapPlacePicker
import ir.taqvim.app.di.coordinatesLabel
import ir.taqvim.core.model.Coordinates
import ir.taqvim.feature.map.MapRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** The world map (T-1301); a point picked on it is saved as the chosen place once the user confirms. */
@Composable
internal fun MapPickScreen(
    modifier: Modifier,
    picker: MapPlacePicker = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Coordinates?>(null) }
    MapRoute(modifier, onLocationPicked = { picked = it })
    picked?.let { point ->
        UseAsPlaceDialog(
            coordinates = point,
            onConfirm = {
                picked = null
                scope.launch { picker.useAsPlace(point) }
            },
            onDismiss = { picked = null },
        )
    }
}

/** Asks whether [coordinates] should become the chosen place. */
@Composable
internal fun UseAsPlaceDialog(
    coordinates: Coordinates,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_use_as_place_title)) },
        text = { Text(stringResource(R.string.map_use_as_place_message, coordinatesLabel(coordinates))) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.map_use_as_place_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_use_as_place_cancel)) } },
    )
}
