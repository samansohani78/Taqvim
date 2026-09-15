/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import kotlin.math.abs
import kotlin.math.min

/** A shaded grid and the color of each of its values (`null` leaves a cell clear). */
internal class ShadeLayer(
    val grid: ShadeGrid,
    val colorOf: (Int) -> Color?,
)

/** How city names are written next to their markers. */
internal class CityLabels(
    val measurer: TextMeasurer,
    val style: TextStyle,
)

/** The shaded layers of [overlays], bottom first, shared by the flat map and the globe. */
internal fun shadeLayers(
    overlays: MapOverlays,
    criterion: CrescentCriterion,
    palette: MapPalette,
): List<ShadeLayer> =
    listOfNotNull(
        overlays.declination?.let { grid ->
            ShadeLayer(grid) { signedColor(it, DECLINATION_FULL_DEGREES, palette.east, palette.west) }
        },
        overlays.inclination?.let { grid ->
            ShadeLayer(grid) { signedColor(it, INCLINATION_FULL_DEGREES, palette.down, palette.up) }
        },
        overlays.intensity?.let { grid ->
            ShadeLayer(
                grid,
            ) { value -> intensityBand(value)?.let { MapPalette.intensity[it].copy(alpha = BAND_ALPHA) } }
        },
        overlays.moon?.let { grid ->
            ShadeLayer(grid) {
                if (it ==
                    1
                ) {
                    palette.moon.copy(alpha = MOON_ALPHA)
                } else {
                    null
                }
            }
        },
        overlays.illumination?.let { grid ->
            ShadeLayer(grid) { value -> Color.Black.copy(alpha = MapPalette.NIGHT[value]).takeIf { it.alpha > 0f } }
        },
        overlays.crescent?.let { grid ->
            val colors = MapPalette.crescentColors(criterion)
            ShadeLayer(grid) { colors.getOrNull(it)?.copy(alpha = BAND_ALPHA) }
        },
    )

/** The field-strength band (0‥4) of [nanotesla]: under 30 µT, then every 10 µT up to 60 µT or more. */
internal fun intensityBand(nanotesla: Int): Int? =
    if (nanotesla == ShadeGrid.NONE) {
        null
    } else {
        ((nanotesla - FIRST_BAND_NANOTESLA).floorDiv(BAND_NANOTESLA) + 1).coerceIn(0, MapPalette.intensity.lastIndex)
    }

private fun signedColor(
    value: Int,
    fullDegrees: Float,
    positive: Color,
    negative: Color,
): Color? {
    if (value == ShadeGrid.NONE || value == 0) return null
    val alpha = min(abs(value).toFloat() / fullDegrees, 1f) * SIGNED_MAX_ALPHA
    return (if (value > 0) positive else negative).copy(alpha = alpha)
}

/** Qibla and direct path lines, the Sun, the Moon, the place, city markers and the picked point. */
internal fun DrawScope.drawMarks(
    state: MapUiState,
    palette: MapPalette,
    labels: CityLabels,
    toScreen: (MapPoint) -> ScreenPoint?,
) {
    val overlays = state.overlays
    overlays.qibla.forEach { line -> drawPolyline(line.map(toScreen), palette.qibla) }
    overlays.directPath.forEach { line -> drawPolyline(line.map(toScreen), palette.path) }
    state.cities.forEach { city ->
        toScreen(Equirectangular.project(city.coordinates))?.let { drawCity(city, it, palette, labels) }
    }
    overlays.sun?.let(toScreen)?.let { drawMarker(it, MapPalette.SUN, filled = true) }
    overlays.moonPoint?.let(toScreen)?.let { drawMarker(it, palette.moon, filled = false) }
    overlays.place?.let(toScreen)?.let { drawMarker(it, palette.place, filled = true) }
    state.picked
        ?.point
        ?.let(toScreen)
        ?.let { drawMarker(it, palette.path, filled = false) }
}

/**
 * The shown line layers, shared by the flat map and the globe: plate boundaries (solid) and time-zone band boundaries
 * (dashed); [project] places a line's map-unit points on the screen (`null` where hidden).
 */
internal fun DrawScope.drawLineLayers(
    outline: WorldOutline,
    layers: Set<MapLayer>,
    palette: MapPalette,
    project: (FloatArray) -> List<ScreenPoint?>,
) {
    if (MapLayer.TECTONIC_PLATES in layers) {
        outline.plates.forEach { drawPolyline(project(it), MapPalette.PLATE, PLATE_STROKE) }
    }
    if (MapLayer.TIME_ZONES in layers) {
        outline.timeZones.forEach { drawPolyline(project(it), palette.timeZone, ZONE_STROKE, ZONE_DASH) }
    }
}

/** A line through [points], broken where a point is not visible (`null`). */
internal fun DrawScope.drawPolyline(
    points: List<ScreenPoint?>,
    color: Color,
    width: Float = PATH_STROKE,
    pathEffect: PathEffect? = null,
) {
    val path = Path()
    var drawing = false
    var segments = 0
    points.forEach { point ->
        when {
            point == null -> drawing = false
            drawing -> path.lineTo(point.x, point.y).also { segments++ }
            else -> path.moveTo(point.x, point.y).also { drawing = true }
        }
    }
    if (segments > 0) drawPath(path, color, style = Stroke(width, pathEffect = pathEffect))
}

private fun DrawScope.drawMarker(
    screen: ScreenPoint,
    color: Color,
    filled: Boolean,
) {
    val center = Offset(screen.x, screen.y)
    if (filled) {
        drawCircle(color, MARKER_RADIUS, center)
    } else {
        drawCircle(color, MARKER_RADIUS, center, style = Stroke(PATH_STROKE))
    }
}

private fun DrawScope.drawCity(
    city: MapCity,
    screen: ScreenPoint,
    palette: MapPalette,
    labels: CityLabels,
) {
    val center = Offset(screen.x, screen.y)
    val radius = CITY_RADIUS_DP * density
    drawCircle(palette.cityHalo, radius + CITY_HALO_DP * density, center)
    drawCircle(palette.city, radius, center)
    val text = labels.measurer.measure(city.name, labels.style)
    val topLeft = Offset(screen.x + radius * 2, screen.y - text.size.height / 2f)
    drawText(text, color = palette.city, topLeft = topLeft)
}

private const val MOON_ALPHA = 0.22f
private const val BAND_ALPHA = 0.5f
private const val DECLINATION_FULL_DEGREES = 30f
private const val INCLINATION_FULL_DEGREES = 90f
private const val SIGNED_MAX_ALPHA = 0.55f
private const val FIRST_BAND_NANOTESLA = 30_000
private const val BAND_NANOTESLA = 10_000
private const val PATH_STROKE = 3f
private const val PLATE_STROKE = 1.6f
private const val ZONE_STROKE = 1.2f
private val ZONE_DASH = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
private const val MARKER_RADIUS = 7f
private const val CITY_RADIUS_DP = 3f
private const val CITY_HALO_DP = 1.5f
