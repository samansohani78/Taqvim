/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import ir.taqvim.core.ui.component.unitFraction

private const val PADDING_DP = 4f
private const val LINE_DP = 1f
private const val MARKER_DP = 5f
private const val MARKER_RING_DP = 2f
private const val NIGHT_COLOR = 0xFF000000.toInt()
private const val MAX_NIGHT_ALPHA = 150

/** A point of a map thumbnail in normalized coordinates: 0‥1 from the left and from the top of the drawing area. */
public data class NormalizedPoint(
    public val x: Float,
    public val y: Float,
)

/** A projected outline: a filled [closed] polygon (e.g. a country) or an open line (e.g. a coast or border). */
public data class MapShape(
    public val points: List<NormalizedPoint>,
    public val closed: Boolean,
) {
    init {
        require(points.size >= 2) { "A map shape needs at least two points" }
    }
}

/**
 * Darkness over the whole map on a [columns] × [rows] grid (e.g. night and twilight, T-1211): cell (column, row) spans
 * the matching share of the drawing area from its top-left corner, and [darkness] holds one 0 (clear) … 1 (darkest)
 * value per cell, row by row.
 */
public data class MapShade(
    public val columns: Int,
    public val rows: Int,
    public val darkness: List<Float>,
) {
    init {
        require(columns > 0 && rows > 0) { "A map shade needs cells (was $columns × $rows)" }
        require(darkness.size == columns * rows) { "A map shade needs ${columns * rows} values (was ${darkness.size})" }
    }
}

/**
 * What a map thumbnail shows (T-702): caller-projected [shapes], an optional [shade] drawn over them (T-1211) and an
 * optional [marker] (e.g. the chosen place). All geometry comes from the caller, already projected into 0‥1
 * coordinates.
 */
public data class MapThumbnailModel(
    public val shapes: List<MapShape>,
    public val marker: NormalizedPoint? = null,
    public val shade: MapShade? = null,
)

/**
 * Draws a [MapThumbnailModel] (T-702): water in the palette's highest surface, closed shapes filled with
 * [PainterPalette.land] and outlined, open shapes as lines, the shade as translucent black cells, and the marker as a
 * disc with a ring on top. Maps are never mirrored in RTL.
 */
public class MapThumbnailPainter(
    private val palette: PainterPalette,
    private val environment: PainterEnvironment,
) : BitmapPainter<MapThumbnailModel> {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = environment.px(LINE_DP)
            strokeJoin = Paint.Join.ROUND
        }
    private val path = Path()

    override fun draw(
        canvas: Canvas,
        model: MapThumbnailModel,
        width: Int,
        height: Int,
    ) {
        canvas.drawColor(palette.surfaceHighest)
        val padding = environment.px(PADDING_DP)
        model.shapes.forEach { shape -> drawShape(canvas, shape, width, height, padding) }
        model.shade?.let { shade -> drawShade(canvas, shade, width, height, padding) }
        model.marker?.let { marker ->
            val (x, y) = MapProjection.toPixel(marker, width, height, padding)
            val radius = environment.px(MARKER_DP)
            fill.color = palette.onPrimary
            canvas.drawCircle(x, y, radius + environment.px(MARKER_RING_DP), fill)
            fill.color = palette.primary
            canvas.drawCircle(x, y, radius, fill)
        }
    }

    private fun drawShade(
        canvas: Canvas,
        shade: MapShade,
        width: Int,
        height: Int,
        padding: Float,
    ) {
        val (left, top) = MapProjection.toPixel(NormalizedPoint(0f, 0f), width, height, padding)
        val (right, bottom) = MapProjection.toPixel(NormalizedPoint(1f, 1f), width, height, padding)
        val cellWidth = (right - left) / shade.columns
        val cellHeight = (bottom - top) / shade.rows
        shade.darkness.forEachIndexed { index, darkness ->
            val alpha = (unitFraction(darkness) * MAX_NIGHT_ALPHA).toInt()
            if (alpha > 0) {
                fill.color = PainterColors.withAlpha(NIGHT_COLOR, alpha)
                val x = left + index % shade.columns * cellWidth
                val y = top + index / shade.columns * cellHeight
                canvas.drawRect(x, y, x + cellWidth, y + cellHeight, fill)
            }
        }
    }

    private fun drawShape(
        canvas: Canvas,
        shape: MapShape,
        width: Int,
        height: Int,
        padding: Float,
    ) {
        path.reset()
        shape.points.forEachIndexed { index, point ->
            val (x, y) = MapProjection.toPixel(point, width, height, padding)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (shape.closed) {
            path.close()
            fill.color = palette.land
            canvas.drawPath(path, fill)
        }
        line.color = palette.outline
        canvas.drawPath(path, line)
    }
}
