/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable

/** Callbacks of the map screen. */
@Immutable
data class MapActions(
    val onToggleLayer: (MapLayer) -> Unit = {},
    val onZoom: (Double, ScreenPoint, ViewSize) -> Unit = { _, _, _ -> },
    val onPan: (Float, Float, ViewSize) -> Unit = { _, _, _ -> },
    val onResize: (ViewSize) -> Unit = {},
    val onPick: (ScreenPoint, ViewSize) -> Unit = { _, _ -> },
    val onPickCenter: () -> Unit = {},
    val onSelectMinute: (Int) -> Unit = {},
    val onStepDay: (Int) -> Unit = {},
    val onNow: () -> Unit = {},
    val onSelectProjection: (MapProjection) -> Unit = {},
    val onSelectCrescentCriterion: (CrescentCriterion) -> Unit = {},
)
