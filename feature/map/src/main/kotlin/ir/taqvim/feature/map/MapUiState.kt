/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/** State of the world map screen (T-1301). */
data class MapUiState(
    val outline: OutlineState = OutlineState.Loading,
    val layers: ImmutableSet<MapLayer> = DEFAULT_LAYERS,
    val viewport: MapViewport = MapViewport(),
    /** The shown moment; `null` until settings arrive. */
    val time: MapTime? = null,
    val overlays: MapOverlays = MapOverlays(),
    /** The last picked point with its coordinates written in the language's digits. */
    val picked: MapPick? = null,
    /** Whether a place is chosen (Qibla and direct path need one). */
    val hasPlace: Boolean = false,
) {
    companion object {
        val DEFAULT_LAYERS: ImmutableSet<MapLayer> = persistentSetOf(MapLayer.DAY_NIGHT, MapLayer.GRID, MapLayer.QIBLA)
    }
}

/** Loading of the bundled world outline. */
sealed interface OutlineState {
    data object Loading : OutlineState

    data object Unavailable : OutlineState

    data class Ready(
        val outline: WorldOutline,
    ) : OutlineState
}

/** The shown moment and its texts; [live] while it follows the clock. */
data class MapTime(
    val instant: Instant,
    /** Minutes after local midnight in the settings' zone (the slider's value). */
    val minuteOfDay: Int,
    val dateText: String,
    val timeText: String,
    val live: Boolean,
)

/** Everything drawn over the outline; grids are `null` while their layer is off. */
data class MapOverlays(
    val illumination: ShadeGrid? = null,
    val moon: ShadeGrid? = null,
    val crescent: ShadeGrid? = null,
    val declination: ShadeGrid? = null,
    val sun: MapPoint? = null,
    val moonPoint: MapPoint? = null,
    val place: MapPoint? = null,
    val qibla: ImmutableList<ImmutableList<MapPoint>> = persistentListOf(),
    val directPath: ImmutableList<ImmutableList<MapPoint>> = persistentListOf(),
)

/** A picked map point and its coordinates text. */
data class MapPick(
    val point: MapPoint,
    val text: String,
)
