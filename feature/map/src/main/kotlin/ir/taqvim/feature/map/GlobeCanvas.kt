/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ir.taqvim.core.model.Coordinates
import kotlin.math.hypot

/**
 * The globe (ADR-0024): the orthographic projection drawn on the canvas. Land and shaded cells that cross the horizon
 * are pinned to the globe's rim; lines break there and markers on the far side are hidden.
 */
internal fun DrawScope.drawGlobe(
    outline: WorldOutline,
    state: MapUiState,
    palette: MapPalette,
    labels: CityLabels,
) {
    val frame = GlobeFrame(state.globe, ViewSize(size.width, size.height))
    val center = Offset(frame.centerX, frame.centerY)
    drawCircle(palette.ocean, frame.radius, center)
    outline.land.forEach { ring -> frame.polygon(ring)?.let { drawPath(it, palette.land) } }
    outline.borders.forEach { line -> drawPolyline(frame.polyline(line), palette.border, width = 1f) }
    shadeLayers(state.overlays, state.crescentCriterion, palette).forEach { drawGlobeGrid(it, frame) }
    drawLineLayers(outline, state, palette, frame::polyline)
    drawOffsetLabels(state, palette, labels) { frame.visible(Equirectangular.unproject(it)) }
    if (MapLayer.GRID in state.layers) drawGlobeGraticule(frame, palette.grid)
    drawMarks(state, palette, labels) { frame.visible(Equirectangular.unproject(it)) }
    drawCircle(palette.border, frame.radius, center, style = Stroke(1f))
}

private fun DrawScope.drawGlobeGrid(
    layer: ShadeLayer,
    frame: GlobeFrame,
) {
    val grid = layer.grid
    val corners = frame.corners(grid.columns, grid.rows)
    val paths = LinkedHashMap<Color, Path>()
    for (row in 0 until grid.rows) {
        for (column in 0 until grid.columns) {
            val color = layer.colorOf(grid[column, row]) ?: continue
            corners.addCell(paths.getOrPut(color) { Path() }, column, row)
        }
    }
    paths.forEach { (color, path) -> drawPath(path, color) }
}

private fun DrawScope.drawGlobeGraticule(
    frame: GlobeFrame,
    color: Color,
) {
    for (meridian in -HALF_TURN until HALF_TURN step GRATICULE_STEP) {
        val line =
            (-RIGHT_ANGLE..RIGHT_ANGLE step SAMPLE_STEP).map {
                frame.visible(Coordinates(it.toDouble(), meridian.toDouble()))
            }
        drawPolyline(line, color, width = 1f)
    }
    for (parallel in -RIGHT_ANGLE + GRATICULE_STEP until RIGHT_ANGLE step GRATICULE_STEP) {
        val line =
            (-HALF_TURN..HALF_TURN step SAMPLE_STEP).map {
                frame.visible(Coordinates(parallel.toDouble(), it.toDouble()))
            }
        drawPolyline(line, color, width = 1f)
    }
}

/** One frame's projection onto the view: the globe's center and radius in pixels. */
internal class GlobeFrame(
    globe: GlobeView,
    size: ViewSize,
) {
    private val projection = globe.projection
    val radius: Float = globe.radius(size)
    val centerX: Float = size.width / 2
    val centerY: Float = size.height / 2

    /** Where [place] is drawn, or `null` on the far side. */
    fun visible(place: Coordinates): ScreenPoint? =
        projection.project(place).takeIf { it.visible }?.let { ScreenPoint(x(it.x), y(it.y)) }

    /** A land ring (map units) with far-side points pinned to the rim; `null` when all of it is on the far side. */
    fun polygon(ring: FloatArray): Path? {
        val path = Path()
        var anyVisible = false
        for (index in 0 until ring.size / 2) {
            val point = project(ring[2 * index], ring[2 * index + 1])
            anyVisible = anyVisible || point.visible
            val (x, y) = pinned(point)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path.takeIf { anyVisible }
    }

    /** A line (map units) with far-side points left out. */
    fun polyline(line: FloatArray): List<ScreenPoint?> =
        (0 until line.size / 2).map { index ->
            project(line[2 * index], line[2 * index + 1]).takeIf { it.visible }?.let { ScreenPoint(x(it.x), y(it.y)) }
        }

    /** The pinned screen positions of the corners of a [columns] × [rows] grid. */
    fun corners(
        columns: Int,
        rows: Int,
    ): GridCorners {
        val xs = FloatArray((columns + 1) * (rows + 1))
        val ys = FloatArray(xs.size)
        val visible = BooleanArray(xs.size)
        for (index in xs.indices) {
            val point = project((index % (columns + 1)).toFloat() / columns, (index / (columns + 1)).toFloat() / rows)
            val (x, y) = pinned(point)
            xs[index] = x
            ys[index] = y
            visible[index] = point.visible
        }
        return GridCorners(columns, xs, ys, visible)
    }

    private fun project(
        mapX: Float,
        mapY: Float,
    ): GlobePoint =
        projection.project(
            RIGHT_ANGLE - mapY.toDouble() * HALF_TURN,
            mapX.toDouble() * FULL_TURN - HALF_TURN,
        )

    private fun pinned(point: GlobePoint): Pair<Float, Float> {
        if (point.visible) return x(point.x) to y(point.y)
        val length = hypot(point.x, point.y).takeIf { it > 0 } ?: 1.0
        return x(point.x / length) to y(point.y / length)
    }

    private fun x(unit: Double): Float = centerX + radius * unit.toFloat()

    private fun y(unit: Double): Float = centerY - radius * unit.toFloat()
}

/**
 * Pinned corner positions of a grid; a cell is drawn when at least half of its corners are on the near side, so cells
 * behind the rim do not pile up along it.
 */
internal class GridCorners(
    private val columns: Int,
    private val xs: FloatArray,
    private val ys: FloatArray,
    private val visible: BooleanArray,
) {
    fun addCell(
        path: Path,
        column: Int,
        row: Int,
    ) {
        val topLeft = row * (columns + 1) + column
        val cell = intArrayOf(topLeft, topLeft + 1, topLeft + columns + 2, topLeft + columns + 1)
        if (cell.count { visible[it] } < MIN_VISIBLE_CORNERS) return
        path.moveTo(xs[cell[0]], ys[cell[0]])
        for (corner in 1 until cell.size) path.lineTo(xs[cell[corner]], ys[cell[corner]])
        path.close()
    }
}

private const val HALF_TURN = 180
private const val FULL_TURN = 360.0
private const val RIGHT_ANGLE = 90
private const val GRATICULE_STEP = 30
private const val SAMPLE_STEP = 3
private const val MIN_VISIBLE_CORNERS = 2
