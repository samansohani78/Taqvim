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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
        }
        MapControls(state, actions)
    }
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
        LayerChips(state, actions)
        if (MapLayer.CRESCENT_VISIBILITY in state.layers) CrescentLegend()
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
private fun CrescentLegend() {
    val labels =
        listOf(
            R.string.map_crescent_a,
            R.string.map_crescent_b,
            R.string.map_crescent_c,
            R.string.map_crescent_d,
            R.string.map_crescent_e,
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).background(MapPalette.crescent[index], CircleShape))
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** The chip label of each layer. */
internal val MapLayer.label: Int
    get() =
        when (this) {
            MapLayer.DAY_NIGHT -> R.string.map_layer_day_night
            MapLayer.MOON_VISIBILITY -> R.string.map_layer_moon
            MapLayer.CRESCENT_VISIBILITY -> R.string.map_layer_crescent
            MapLayer.MAGNETIC_DECLINATION -> R.string.map_layer_magnetic
            MapLayer.GRID -> R.string.map_layer_grid
            MapLayer.QIBLA -> R.string.map_layer_qibla
            MapLayer.DIRECT_PATH -> R.string.map_layer_direct_path
        }

private const val CONTROLS_MAX_HEIGHT = 320
private const val LAST_MINUTE = 1_439f
