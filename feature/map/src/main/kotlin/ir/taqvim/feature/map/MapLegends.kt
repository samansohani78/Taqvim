/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet

/** The legends of the shown shaded layers, with the crescent criterion's choice. */
@Composable
internal fun MapLegends(
    state: MapUiState,
    actions: MapActions,
) {
    val palette = MapPalette.of(MaterialTheme.colorScheme)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (MapLayer.CRESCENT_VISIBILITY in state.layers) {
            CriterionChips(state.crescentCriterion, actions)
            Legend(MapPalette.crescentColors(state.crescentCriterion).zip(crescentLabels(state.crescentCriterion)))
        }
        if (MapLayer.MAGNETIC_DECLINATION in state.layers) {
            Legend(listOf(palette.east to R.string.map_declination_east, palette.west to R.string.map_declination_west))
        }
        if (MapLayer.MAGNETIC_INCLINATION in state.layers) {
            Legend(listOf(palette.down to R.string.map_inclination_down, palette.up to R.string.map_inclination_up))
        }
        if (MapLayer.MAGNETIC_INTENSITY in state.layers) Legend(MapPalette.intensity.zip(INTENSITY_LABELS))
        LineLegends(state.layers, palette)
    }
}

/**
 * The time-zone and plate line keys, with how the offsets are computed and when smaller plates show; the plate model's
 * attribution is written on the map itself.
 */
@Composable
private fun LineLegends(
    layers: ImmutableSet<MapLayer>,
    palette: MapPalette,
) {
    if (MapLayer.TIME_ZONES in layers) {
        Legend(listOf(palette.timeZone to R.string.map_legend_time_zone))
        Text(stringResource(R.string.map_time_zones_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.map_time_zones_mixed_note), style = MaterialTheme.typography.bodySmall)
    }
    if (MapLayer.TECTONIC_PLATES in layers) {
        Legend(listOf(MapPalette.PLATE to R.string.map_legend_plate))
        Text(stringResource(R.string.map_plates_zoom_note), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CriterionChips(
    selected: CrescentCriterion,
    actions: MapActions,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CrescentCriterion.entries.forEach { criterion ->
            FilterChip(
                selected = criterion == selected,
                onClick = { actions.onSelectCrescentCriterion(criterion) },
                label = { Text(stringResource(criterion.label)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(entries: List<Pair<Color, Int>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun crescentLabels(criterion: CrescentCriterion): List<Int> =
    when (criterion) {
        CrescentCriterion.YALLOP -> {
            listOf(
                R.string.map_crescent_a,
                R.string.map_crescent_b,
                R.string.map_crescent_c,
                R.string.map_crescent_d,
                R.string.map_crescent_e,
            )
        }

        CrescentCriterion.ODEH -> {
            listOf(R.string.map_odeh_a, R.string.map_odeh_b, R.string.map_odeh_c, R.string.map_odeh_d)
        }
    }

/** The chip label of each criterion. */
@get:StringRes
internal val CrescentCriterion.label: Int
    get() =
        when (this) {
            CrescentCriterion.YALLOP -> R.string.map_criterion_yallop
            CrescentCriterion.ODEH -> R.string.map_criterion_odeh
        }

private val INTENSITY_LABELS =
    listOf(
        R.string.map_intensity_under_30,
        R.string.map_intensity_30_40,
        R.string.map_intensity_40_50,
        R.string.map_intensity_50_60,
        R.string.map_intensity_over_60,
    )
