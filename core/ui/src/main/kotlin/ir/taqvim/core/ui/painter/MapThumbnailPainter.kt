/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

private const val PADDING_DP = 4f
private const val LINE_DP = 1f
private const val MARKER_DP = 5f
private const val MARKER_RING_DP = 2f

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
 * What a map thumbnail shows (T-702): caller-projected [shapes] and an optional [marker] (e.g. the chosen place).
 * Taqvim has no map data yet (T-1301), so all geometry comes from the caller, already projected into 0‥1 coordinates.
 */
public data class MapThumbnailModel(
    public val shapes: List<MapShape>,
    public val marker: NormalizedPoint? = null,
)

/**
 * Draws a [MapThumbnailModel] (T-702): water in the palette's highest surface, closed shapes filled with
 * [PainterPalette.land] and outlined, open shapes as lines, and the marker as a disc with a ring. Maps are never
 * mirrored in RTL.
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
        model.marker?.let { marker ->
            val (x, y) = MapProjection.toPixel(marker, width, height, padding)
            val radius = environment.px(MARKER_DP)
            fill.color = palette.onPrimary
            canvas.drawCircle(x, y, radius + environment.px(MARKER_RING_DP), fill)
            fill.color = palette.primary
            canvas.drawCircle(x, y, radius, fill)
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
