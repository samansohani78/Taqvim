/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import ir.taqvim.core.ui.component.ProgressRingGeometry
import kotlin.math.max
import kotlin.math.min

private const val MIN_STROKE_DP = 3f
private const val STROKE_SHARE = 0.09f
private const val FULL_TURN = 360f

/** A progress ring for a widget bitmap (T-1212): [progress] 0 … 1 of the ring filled, like the T-701 `ProgressRing`. */
public data class ProgressRingBitmapModel(
    public val progress: Float,
)

/**
 * Draws a [ProgressRingBitmapModel]: a full track in the outline variant and the indicator in the primary color,
 * clockwise from the top in LTR and counter-clockwise in RTL, centered in the largest square that fits.
 */
public class ProgressRingBitmapPainter(
    private val palette: PainterPalette,
    private val environment: PainterEnvironment,
) : BitmapPainter<ProgressRingBitmapModel> {
    private val track =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    private val indicator =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    private val oval = RectF()

    override fun draw(
        canvas: Canvas,
        model: ProgressRingBitmapModel,
        width: Int,
        height: Int,
    ) {
        canvas.drawColor(palette.background)
        val side = min(width, height).toFloat()
        val stroke = max(environment.px(MIN_STROKE_DP), side * STROKE_SHARE)
        val inset = stroke / 2
        val left = (width - side) / 2 + inset
        val top = (height - side) / 2 + inset
        oval.set(left, top, left + side - stroke, top + side - stroke)
        track.strokeWidth = stroke
        track.color = palette.outlineVariant
        canvas.drawArc(oval, 0f, FULL_TURN, false, track)
        val sweep = ProgressRingGeometry.sweep(model.progress, environment.rtl)
        if (sweep != 0f) {
            indicator.strokeWidth = stroke
            indicator.color = palette.primary
            canvas.drawArc(oval, ProgressRingGeometry.START_ANGLE, sweep, false, indicator)
        }
    }
}
