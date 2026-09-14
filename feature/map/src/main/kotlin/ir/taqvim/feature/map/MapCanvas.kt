/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import kotlin.math.min

/** The map with its gestures and accessibility actions (zoom in/out, pick the center). */
@Composable
internal fun MapCanvas(
    outline: WorldOutline,
    state: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
) {
    val palette = MapPalette.of(MaterialTheme.colorScheme)
    var viewSize by remember { mutableStateOf(ViewSize(0f, 0f)) }
    val currentActions by rememberUpdatedState(actions)
    val description = stringResource(R.string.map_content_description)
    val zoomIn = stringResource(R.string.map_zoom_in)
    val zoomOut = stringResource(R.string.map_zoom_out)
    val pickCenter = stringResource(R.string.map_pick_center)
    Canvas(
        modifier
            .onSizeChanged {
                viewSize = ViewSize(it.width.toFloat(), it.height.toFloat())
                currentActions.onResize(viewSize)
            }.pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val size = ViewSize(size.width.toFloat(), size.height.toFloat())
                    if (zoom != 1f) currentActions.onZoom(zoom.toDouble(), ScreenPoint(centroid.x, centroid.y), size)
                    if (pan != Offset.Zero) currentActions.onPan(pan.x, pan.y, size)
                }
            }.pointerInput(Unit) {
                detectTapGestures { offset ->
                    val size = ViewSize(size.width.toFloat(), size.height.toFloat())
                    currentActions.onPick(ScreenPoint(offset.x, offset.y), size)
                }
            }.semantics {
                contentDescription = description
                customActions =
                    listOf(
                        CustomAccessibilityAction(zoomIn) { zoomAtCenter(currentActions, ZOOM_STEP, viewSize) },
                        CustomAccessibilityAction(zoomOut) { zoomAtCenter(currentActions, 1 / ZOOM_STEP, viewSize) },
                        CustomAccessibilityAction(pickCenter) {
                            currentActions.onPickCenter()
                            true
                        },
                    )
            },
    ) {
        drawMap(outline, state, palette)
    }
}

private fun zoomAtCenter(
    actions: MapActions,
    factor: Double,
    size: ViewSize,
): Boolean {
    actions.onZoom(factor, ScreenPoint(size.width / 2, size.height / 2), size)
    return true
}

private fun DrawScope.drawMap(
    outline: WorldOutline,
    state: MapUiState,
    palette: MapPalette,
) {
    val view = ViewSize(size.width, size.height)
    val viewport = state.viewport
    val topLeft = viewport.toScreen(MapPoint(0.0, 0.0), view)
    val bottomRight = viewport.toScreen(MapPoint(1.0, 1.0), view)
    clipRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y) {
        drawRect(palette.ocean)
        outline.land.forEach { drawPath(it.toPath(viewport, view, closed = true), palette.land) }
        outline.borders.forEach {
            drawPath(it.toPath(viewport, view, closed = false), palette.border, style = Stroke(1f))
        }
        drawOverlays(state, viewport, view, palette)
    }
}

private fun DrawScope.drawOverlays(
    state: MapUiState,
    viewport: MapViewport,
    view: ViewSize,
    palette: MapPalette,
) {
    drawShades(state.overlays, viewport, view, palette)
    if (MapLayer.GRID in state.layers) drawGraticule(viewport, view, palette.grid)
    drawMarks(state, viewport, view, palette)
}

private fun DrawScope.drawShades(
    overlays: MapOverlays,
    viewport: MapViewport,
    view: ViewSize,
    palette: MapPalette,
) {
    overlays.declination?.let { grid ->
        drawGrid(grid, viewport, view) { value -> declinationColor(value, palette) }
    }
    overlays.moon?.let { grid ->
        drawGrid(grid, viewport, view) { if (it == 1) palette.moon.copy(alpha = MOON_ALPHA) else null }
    }
    overlays.illumination?.let { grid ->
        drawGrid(grid, viewport, view) { Color.Black.copy(alpha = MapPalette.NIGHT[it]).takeIf { it.alpha > 0f } }
    }
    overlays.crescent?.let { grid ->
        drawGrid(grid, viewport, view) { MapPalette.crescent.getOrNull(it)?.copy(alpha = CRESCENT_ALPHA) }
    }
}

private fun DrawScope.drawMarks(
    state: MapUiState,
    viewport: MapViewport,
    view: ViewSize,
    palette: MapPalette,
) {
    val overlays = state.overlays
    overlays.qibla.forEach { drawLine(it, viewport, view, palette.qibla) }
    overlays.directPath.forEach { drawLine(it, viewport, view, palette.path) }
    overlays.sun?.let { drawMarker(it, viewport, view, MapPalette.SUN, filled = true) }
    overlays.moonPoint?.let { drawMarker(it, viewport, view, palette.moon, filled = false) }
    overlays.place?.let { drawMarker(it, viewport, view, palette.place, filled = true) }
    state.picked?.let { drawMarker(it.point, viewport, view, palette.path, filled = false) }
}

private fun declinationColor(
    value: Int,
    palette: MapPalette,
): Color? {
    if (value == ShadeGrid.NONE || value == 0) return null
    val alpha = min(abs(value).toFloat() / DECLINATION_FULL_DEGREES, 1f) * DECLINATION_MAX_ALPHA
    return (if (value > 0) palette.east else palette.west).copy(alpha = alpha)
}

private fun DrawScope.drawGrid(
    grid: ShadeGrid,
    viewport: MapViewport,
    view: ViewSize,
    colorOf: (Int) -> Color?,
) {
    for (row in 0 until grid.rows) {
        for (column in 0 until grid.columns) {
            val color = colorOf(grid[column, row]) ?: continue
            val start = viewport.toScreen(MapPoint(column.toDouble() / grid.columns, row.toDouble() / grid.rows), view)
            val end = viewport.toScreen(MapPoint((column + 1.0) / grid.columns, (row + 1.0) / grid.rows), view)
            val cell = Size(end.x - start.x + CELL_OVERLAP, end.y - start.y + CELL_OVERLAP)
            drawRect(color, Offset(start.x, start.y), cell)
        }
    }
}

private fun DrawScope.drawGraticule(
    viewport: MapViewport,
    view: ViewSize,
    color: Color,
) {
    for (step in 0..GRATICULE_COLUMNS) {
        val x = step.toDouble() / GRATICULE_COLUMNS
        drawLine(listOf(MapPoint(x, 0.0), MapPoint(x, 1.0)), viewport, view, color, GRID_STROKE)
    }
    for (step in 0..GRATICULE_ROWS) {
        val y = step.toDouble() / GRATICULE_ROWS
        drawLine(listOf(MapPoint(0.0, y), MapPoint(1.0, y)), viewport, view, color, GRID_STROKE)
    }
}

private fun DrawScope.drawLine(
    points: List<MapPoint>,
    viewport: MapViewport,
    view: ViewSize,
    color: Color,
    width: Float = PATH_STROKE,
) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val screen = viewport.toScreen(point, view)
        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
    }
    drawPath(path, color, style = Stroke(width))
}

private fun DrawScope.drawMarker(
    point: MapPoint,
    viewport: MapViewport,
    view: ViewSize,
    color: Color,
    filled: Boolean,
) {
    val screen = viewport.toScreen(point, view)
    val center = Offset(screen.x, screen.y)
    if (filled) {
        drawCircle(color, MARKER_RADIUS, center)
    } else {
        drawCircle(color, MARKER_RADIUS, center, style = Stroke(PATH_STROKE))
    }
}

private fun FloatArray.toPath(
    viewport: MapViewport,
    view: ViewSize,
    closed: Boolean,
): Path {
    val path = Path()
    for (index in 0 until size / 2) {
        val screen = viewport.toScreen(MapPoint(this[2 * index].toDouble(), this[2 * index + 1].toDouble()), view)
        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
    }
    if (closed) path.close()
    return path
}

private const val ZOOM_STEP = 2.0
private const val MOON_ALPHA = 0.22f
private const val CRESCENT_ALPHA = 0.5f
private const val DECLINATION_FULL_DEGREES = 30f
private const val DECLINATION_MAX_ALPHA = 0.55f
private const val CELL_OVERLAP = 0.5f
private const val GRATICULE_COLUMNS = 12
private const val GRATICULE_ROWS = 6
private const val GRID_STROKE = 1f
private const val PATH_STROKE = 3f
private const val MARKER_RADIUS = 7f
