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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.collections.immutable.ImmutableList

/** The flat map or the globe with its gestures and accessibility actions (zoom in/out, pick the center or a city). */
@Composable
internal fun MapCanvas(
    outline: WorldOutline,
    state: MapUiState,
    actions: MapActions,
    modifier: Modifier = Modifier,
) {
    val palette = MapPalette.of(MaterialTheme.colorScheme)
    val labels = CityLabels(rememberTextMeasurer(LABEL_CACHE), MaterialTheme.typography.labelSmall)
    val density = LocalDensity.current.density
    var viewSize by remember { mutableStateOf(ViewSize(0f, 0f)) }
    val currentActions by rememberUpdatedState(actions)
    val description = stringResource(R.string.map_content_description)
    val accessibilityActions = rememberMapAccessibilityActions(state.cities, { currentActions }, { viewSize })
    Canvas(
        modifier
            .clipToBounds()
            .onSizeChanged {
                viewSize = ViewSize(it.width.toFloat(), it.height.toFloat(), density)
                currentActions.onResize(viewSize)
            }.mapGestures { currentActions }
            .semantics {
                contentDescription = description
                customActions = accessibilityActions
            },
    ) {
        when (state.projection) {
            MapProjection.FLAT -> drawMap(outline, state, palette, labels)
            MapProjection.GLOBE -> drawGlobe(outline, state, palette, labels)
        }
    }
}

/** Pinch and drag zoom and pan (or turn the globe); a tap picks. */
private fun Modifier.mapGestures(actions: () -> MapActions): Modifier =
    pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val size = ViewSize(size.width.toFloat(), size.height.toFloat())
            if (zoom != 1f) actions().onZoom(zoom.toDouble(), ScreenPoint(centroid.x, centroid.y), size)
            if (pan != Offset.Zero) actions().onPan(pan.x, pan.y, size)
        }
    }.pointerInput(Unit) {
        detectTapGestures { offset ->
            actions().onPick(ScreenPoint(offset.x, offset.y), ViewSize(size.width.toFloat(), size.height.toFloat()))
        }
    }

/** Zoom in and out, pick the center, and pick each shown city marker (in population order), for TalkBack. */
@Composable
private fun rememberMapAccessibilityActions(
    cities: ImmutableList<MapCity>,
    actions: () -> MapActions,
    viewSize: () -> ViewSize,
): List<CustomAccessibilityAction> {
    val resources = LocalResources.current
    val zoomIn = stringResource(R.string.map_zoom_in)
    val zoomOut = stringResource(R.string.map_zoom_out)
    val pickCenter = stringResource(R.string.map_pick_center)
    return remember(cities, resources, zoomIn, zoomOut, pickCenter) {
        val fixed =
            listOf(
                CustomAccessibilityAction(zoomIn) { zoomAtCenter(actions(), ZOOM_STEP, viewSize()) },
                CustomAccessibilityAction(zoomOut) { zoomAtCenter(actions(), 1 / ZOOM_STEP, viewSize()) },
                CustomAccessibilityAction(pickCenter) {
                    actions().onPickCenter()
                    true
                },
            )
        fixed +
            cities.map { city ->
                CustomAccessibilityAction(resources.getString(R.string.map_pick_city, city.name)) {
                    actions().onPickCity(city)
                    true
                }
            }
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
    labels: CityLabels,
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
        drawOverlays(state, viewport, view, palette, labels)
    }
}

private fun DrawScope.drawOverlays(
    state: MapUiState,
    viewport: MapViewport,
    view: ViewSize,
    palette: MapPalette,
    labels: CityLabels,
) {
    shadeLayers(state.overlays, state.crescentCriterion, palette).forEach { drawGrid(it, viewport, view) }
    if (MapLayer.GRID in state.layers) drawGraticule(viewport, view, palette.grid)
    drawMarks(state, palette, labels) { viewport.toScreen(it, view) }
}

/** Cells of one color are filled as one path, so neighbouring cells neither overlap nor leave seams. */
private fun DrawScope.drawGrid(
    layer: ShadeLayer,
    viewport: MapViewport,
    view: ViewSize,
) {
    val grid = layer.grid
    val paths = LinkedHashMap<Color, Path>()
    for (row in 0 until grid.rows) {
        for (column in 0 until grid.columns) {
            val color = layer.colorOf(grid[column, row]) ?: continue
            val start = viewport.toScreen(MapPoint(column.toDouble() / grid.columns, row.toDouble() / grid.rows), view)
            val end = viewport.toScreen(MapPoint((column + 1.0) / grid.columns, (row + 1.0) / grid.rows), view)
            paths.getOrPut(color) { Path() }.addRect(Rect(start.x, start.y, end.x, end.y))
        }
    }
    paths.forEach { (color, path) -> drawPath(path, color) }
}

private fun DrawScope.drawGraticule(
    viewport: MapViewport,
    view: ViewSize,
    color: Color,
) {
    for (step in 0..GRATICULE_COLUMNS) {
        val x = step.toDouble() / GRATICULE_COLUMNS
        drawPolyline(listOf(MapPoint(x, 0.0), MapPoint(x, 1.0)).map { viewport.toScreen(it, view) }, color, GRID_STROKE)
    }
    for (step in 0..GRATICULE_ROWS) {
        val y = step.toDouble() / GRATICULE_ROWS
        drawPolyline(listOf(MapPoint(0.0, y), MapPoint(1.0, y)).map { viewport.toScreen(it, view) }, color, GRID_STROKE)
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
private const val GRATICULE_COLUMNS = 12
private const val GRATICULE_ROWS = 6
private const val GRID_STROKE = 1f
private const val LABEL_CACHE = 128
