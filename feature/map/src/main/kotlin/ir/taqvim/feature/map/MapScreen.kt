/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The world map (T-1301): the map canvas above its time and layer controls. */
@Composable
fun MapScreen(
    state: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        MapScreenContent(state, actions)
    }
}

@Composable
private fun MapScreenContent(
    state: MapUiState,
    actions: MapActions,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.map_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp).semantics { heading() },
        )
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (val outline = state.outline) {
                is OutlineState.Ready -> MapCanvas(outline.outline, state, actions, Modifier.fillMaxSize())
                OutlineState.Unavailable -> Text(stringResource(R.string.map_unavailable))
                OutlineState.Loading -> CircularProgressIndicator()
            }
            if (MapLayer.TECTONIC_PLATES in state.layers && state.outline is OutlineState.Ready) {
                PlateAttribution(Modifier.align(Alignment.BottomStart))
            }
        }
        MapControls(state, actions)
    }
}

/** The plate data's CC BY 4.0 attribution, on the map itself so that it shows without scrolling the controls. */
@Composable
private fun PlateAttribution(modifier: Modifier) {
    Text(
        stringResource(R.string.map_plates_attribution),
        style = MaterialTheme.typography.bodySmall,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun MapControls(
    state: MapUiState,
    actions: MapActions,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = CONTROLS_MAX_HEIGHT.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.time?.let { TimeControls(it, actions) }
        ProjectionChips(state.projection, actions)
        LayerChips(state, actions)
        MapLegends(state, actions)
        if (!state.hasPlace && (MapLayer.QIBLA in state.layers || MapLayer.DIRECT_PATH in state.layers)) {
            Text(stringResource(R.string.map_no_place), style = MaterialTheme.typography.bodySmall)
        }
        state.picked?.let { Text(stringResource(R.string.map_picked, it.text)) }
    }
}

@Composable
private fun TimeControls(
    time: MapTime,
    actions: MapActions,
) {
    val sliderLabel = stringResource(R.string.map_time_slider)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { actions.onStepDay(-1) }) { Text(stringResource(R.string.map_previous_day)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time.dateText, style = MaterialTheme.typography.bodyMedium)
            Text(time.timeText, style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { actions.onStepDay(1) }) { Text(stringResource(R.string.map_next_day)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = time.minuteOfDay.toFloat(),
            onValueChange = { actions.onSelectMinute(it.roundToInt()) },
            valueRange = 0f..LAST_MINUTE,
            modifier =
                Modifier.weight(1f).semantics {
                    contentDescription = sliderLabel
                    stateDescription = time.timeText
                },
        )
        TextButton(onClick = actions.onNow, enabled = !time.live) { Text(stringResource(R.string.map_now)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerChips(
    state: MapUiState,
    actions: MapActions,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MapLayer.entries.forEach { layer ->
            FilterChip(
                selected = layer in state.layers,
                onClick = { actions.onToggleLayer(layer) },
                label = { Text(stringResource(layer.label)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectionChips(
    selected: MapProjection,
    actions: MapActions,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MapProjection.entries.forEach { projection ->
            FilterChip(
                selected = projection == selected,
                onClick = { actions.onSelectProjection(projection) },
                label = { Text(stringResource(projection.label)) },
            )
        }
    }
}

/** The chip label of each projection. */
internal val MapProjection.label: Int
    get() =
        when (this) {
            MapProjection.FLAT -> R.string.map_projection_flat
            MapProjection.GLOBE -> R.string.map_projection_globe
        }

/** The chip label of each layer. */
internal val MapLayer.label: Int
    get() =
        when (this) {
            MapLayer.DAY_NIGHT -> R.string.map_layer_day_night
            MapLayer.MOON_VISIBILITY -> R.string.map_layer_moon
            MapLayer.CRESCENT_VISIBILITY -> R.string.map_layer_crescent
            MapLayer.MAGNETIC_DECLINATION -> R.string.map_layer_magnetic
            MapLayer.MAGNETIC_INCLINATION -> R.string.map_layer_inclination
            MapLayer.MAGNETIC_INTENSITY -> R.string.map_layer_intensity
            MapLayer.GRID -> R.string.map_layer_grid
            MapLayer.TIME_ZONES -> R.string.map_layer_time_zones
            MapLayer.TECTONIC_PLATES -> R.string.map_layer_plates
            MapLayer.CITIES -> R.string.map_layer_cities
            MapLayer.QIBLA -> R.string.map_layer_qibla
            MapLayer.DIRECT_PATH -> R.string.map_layer_direct_path
        }

private const val CONTROLS_MAX_HEIGHT = 320
private const val ATTRIBUTION_BACKGROUND_ALPHA = 0.8f
private const val LAST_MINUTE = 1_439f
