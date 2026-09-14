/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import ir.taqvim.core.ui.component.MoonGeometry
import ir.taqvim.core.ui.component.SunArcGeometry
import ir.taqvim.core.ui.component.SunArcModel

private const val DISC_FILL = 0.95f
private const val TOP_ANGLE = -90f
private const val BOTTOM_ANGLE = 90f
private const val LEFT_ANGLE = 180f
private const val HALF_TURN_SWEEP = 180f
private const val MOON_OUTLINE_DP = 1f
private const val ARC_STROKE_DP = 2f
private const val DASH = 6f
private const val SUN_RADIUS_DP = 7f
private const val LABEL_SP = 12f
private const val LABEL_LINE = 1.8f

/**
 * The Moon for a widget bitmap (T-702): [illuminatedFraction] (0 new … 1 full) lit from the right when [waxing], as
 * seen from the northern hemisphere, turned by [rotationDegrees]. Same geometry as the T-701 `MoonDisc`.
 */
public data class MoonBitmapModel(
    public val illuminatedFraction: Float,
    public val waxing: Boolean,
    public val rotationDegrees: Float = 0f,
)

/** Draws a [MoonBitmapModel] centered in the bitmap (not mirrored in RTL). */
public class MoonBitmapPainter(
    private val palette: PainterPalette,
    environment: PainterEnvironment,
) : BitmapPainter<MoonBitmapModel> {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = environment.px(MOON_OUTLINE_DP)
        }
    private val lit = Path()
    private val disc = RectF()
    private val terminator = RectF()

    @SuppressLint("UseKtx") // androidx.core KTX (Canvas.withRotation) is not a dependency of :core:ui.
    override fun draw(
        canvas: Canvas,
        model: MoonBitmapModel,
        width: Int,
        height: Int,
    ) {
        if (palette.background != 0) canvas.drawColor(palette.background)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f * DISC_FILL
        val checkpoint = canvas.save()
        canvas.rotate(model.rotationDegrees, centerX, centerY)
        canvas.scale(if (model.waxing) 1f else -1f, 1f, centerX, centerY)
        fill.color = palette.surfaceHighest
        canvas.drawCircle(centerX, centerY, radius, fill)
        disc.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        val semiAxis = radius * MoonGeometry.terminatorScale(model.illuminatedFraction)
        terminator.set(centerX - semiAxis, centerY - radius, centerX + semiAxis, centerY + radius)
        lit.reset()
        lit.moveTo(centerX, centerY - radius)
        lit.arcTo(disc, TOP_ANGLE, HALF_TURN_SWEEP, false)
        lit.arcTo(terminator, BOTTOM_ANGLE, MoonGeometry.terminatorSweep(model.illuminatedFraction), false)
        lit.close()
        fill.color = palette.tertiary
        canvas.drawPath(lit, fill)
        outline.color = palette.outlineVariant
        canvas.drawCircle(centerX, centerY, radius, outline)
        canvas.restoreToCount(checkpoint)
    }
}

/**
 * Draws a [SunArcModel] (T-702) like the T-701 `SunArc`: a dashed arc over the horizon, the travelled part and the Sun
 * while it is up, with the start and end labels under the start and end edges (sunrise on the right in RTL).
 */
public class SunArcBitmapPainter(
    private val palette: PainterPalette,
    private val environment: PainterEnvironment,
) : BitmapPainter<SunArcModel> {
    private val strokeWidth = environment.px(ARC_STROKE_DP)
    private val track = strokePaint()
    private val dashed =
        strokePaint().apply {
            pathEffect = DashPathEffect(floatArrayOf(DASH * strokeWidth, DASH * strokeWidth), 0f)
        }
    private val travelled = strokePaint().apply { strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = textPaint(environment)
    private val oval = RectF()

    override fun draw(
        canvas: Canvas,
        model: SunArcModel,
        width: Int,
        height: Int,
    ) {
        if (palette.background != 0) canvas.drawColor(palette.background)
        val labelSize = environment.textPx(LABEL_SP)
        val labelBand = labelSize * LABEL_LINE
        val arcBottom = (height - labelBand).coerceAtLeast(0f)
        val inset = environment.px(SUN_RADIUS_DP)
        val arcWidth = width - 2 * inset
        val arcHeight = (arcBottom - 2 * inset).coerceAtLeast(0f)
        val baseline = arcBottom - inset
        track.color = palette.outlineVariant
        dashed.color = palette.outlineVariant
        canvas.drawLine(0f, baseline, width.toFloat(), baseline, track)
        oval.set(inset, inset, inset + arcWidth, inset + 2 * arcHeight)
        canvas.drawArc(oval, LEFT_ANGLE, HALF_TURN_SWEEP, false, dashed)
        model.progress?.takeIf { SunArcGeometry.isAboveHorizon(it) }?.let { progress ->
            val (start, sweep) = SunArcGeometry.travelledArc(progress, environment.rtl)
            travelled.color = palette.primary
            canvas.drawArc(oval, start, sweep, false, travelled)
            val sun = SunArcGeometry.sunPosition(progress, arcWidth, arcHeight, arcHeight, environment.rtl)
            fill.color = palette.tertiary
            canvas.drawCircle(sun.x + inset, sun.y + inset, inset, fill)
        }
        drawLabels(canvas, model, width, arcBottom + labelBand / 2, labelSize)
    }

    private fun drawLabels(
        canvas: Canvas,
        model: SunArcModel,
        width: Int,
        centerY: Float,
        size: Float,
    ) {
        val half = width / 2f
        val (left, right) =
            if (environment.rtl) model.endLabel to model.startLabel else model.startLabel to model.endLabel
        text.textAlign = Paint.Align.LEFT
        text.textStyle(size, palette.onSurfaceVariant)
        canvas.drawFittedText(left, 0f, centerY, half, text)
        text.textAlign = Paint.Align.RIGHT
        text.textStyle(size, palette.onSurfaceVariant)
        canvas.drawFittedText(right, width.toFloat(), centerY, half, text)
    }

    private fun strokePaint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = this@SunArcBitmapPainter.strokeWidth
        }
}
