/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Colors of the map, derived from the theme; crescent classes A–E use fixed legend colors. */
internal data class MapPalette(
    val ocean: Color,
    val land: Color,
    val border: Color,
    val grid: Color,
    val moon: Color,
    val qibla: Color,
    val path: Color,
    val place: Color,
    val east: Color,
    val west: Color,
) {
    companion object {
        val SUN = Color(0xFFFFB300)
        val NIGHT = listOf(0f, 0.18f, 0.32f, 0.44f, 0.55f)
        val crescent =
            listOf(Color(0xFF2E7D32), Color(0xFF9CCC65), Color(0xFFFDD835), Color(0xFFFB8C00), Color(0xFFE53935))

        fun of(scheme: ColorScheme): MapPalette =
            MapPalette(
                ocean = scheme.surfaceContainerHigh,
                land = scheme.secondaryContainer,
                border = scheme.outline,
                grid = scheme.outlineVariant,
                moon = scheme.tertiary,
                qibla = scheme.tertiary,
                path = scheme.primary,
                place = scheme.error,
                east = scheme.primary,
                west = scheme.error,
            )
    }
}
