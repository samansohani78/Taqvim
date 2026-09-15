/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Colors of the map, derived from the theme; crescent classes and field-strength bands use fixed legend colors. */
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
    val down: Color,
    val up: Color,
    val city: Color,
    val cityHalo: Color,
) {
    companion object {
        val SUN = Color(0xFFFFB300)
        val NIGHT = listOf(0f, 0.18f, 0.32f, 0.44f, 0.55f)
        val crescent =
            listOf(Color(0xFF2E7D32), Color(0xFF9CCC65), Color(0xFFFDD835), Color(0xFFFB8C00), Color(0xFFE53935))

        /** Odeh's zones A–D: visible to the eye, with optical aid (maybe the eye), optical aid only, not visible. */
        val odeh = listOf(crescent[0], crescent[1], crescent[3], crescent[4])

        /** Field strength bands: under 30 µT, 30–40, 40–50, 50–60, and 60 µT or more. */
        val intensity =
            listOf(Color(0xFF3949AB), Color(0xFF1E88E5), Color(0xFF26A69A), Color(0xFFFDD835), Color(0xFFE53935))

        /** The legend colors of [criterion]'s classes. */
        fun crescentColors(criterion: CrescentCriterion): List<Color> =
            when (criterion) {
                CrescentCriterion.YALLOP -> crescent
                CrescentCriterion.ODEH -> odeh
            }

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
                down = scheme.primary,
                up = scheme.tertiary,
                city = scheme.onSurface,
                cityHalo = scheme.surface,
            )
    }
}
