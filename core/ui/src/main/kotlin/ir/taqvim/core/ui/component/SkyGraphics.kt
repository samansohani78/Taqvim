/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val HALF = 0.5f
private const val HALF_TURN = 180f
private const val FULL_TURN = 360f

/** A fraction in 0‥1; NaN becomes 0. */
internal fun unitFraction(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)

/**
 * Geometry of [MoonDisc] (orthographic view of a lit sphere): the lit limb is a half circle and the terminator is a
 * half ellipse with the disc's vertical radius and a horizontal semi-axis of |1 − 2k| radii for illuminated
 * fraction k.
 */
internal object MoonGeometry {
    /** Horizontal semi-axis of the terminator as a fraction of the radius. */
    fun terminatorScale(fraction: Float): Float = abs(1f - 2f * unitFraction(fraction))

    /**
     * Sweep (degrees, clockwise positive) of the terminator from the bottom of the disc back to the top: through the
     * lit side for crescents (k < ½, the lit area is a lune) and through the dark side for gibbous phases.
     */
    fun terminatorSweep(fraction: Float): Float = if (unitFraction(fraction) < HALF) -HALF_TURN else HALF_TURN
}

/** Geometry of [SunArc]: the day's path as the upper half of an ellipse from the start edge to the end edge. */
internal object SunArcGeometry {
    /** Whether [progress] (0 at sunrise, 1 at sunset) puts the sun above the horizon. */
    fun isAboveHorizon(progress: Float?): Boolean = progress != null && !progress.isNaN() && progress in 0f..1f

    /**
     * The sun at [progress] on an arc [width] wide whose apex is [arcHeight] above [baseline]; sunrise is at the start
     * edge (left in LTR, right in RTL).
     */
    fun sunPosition(
        progress: Float,
        width: Float,
        baseline: Float,
        arcHeight: Float,
        rtl: Boolean,
    ): Offset {
        val angle = PI * (1 - unitFraction(progress))
        val halfWidth = width / 2
        val x = halfWidth + halfWidth * cos(angle).toFloat()
        return Offset(if (rtl) width - x else x, baseline - arcHeight * sin(angle).toFloat())
    }

    /** Start angle and sweep (degrees, as for `drawArc`) of the part of the arc the sun has travelled. */
    fun travelledArc(
        progress: Float,
        rtl: Boolean,
    ): Pair<Float, Float> {
        val sweep = HALF_TURN * unitFraction(progress)
        return if (rtl) 0f to -sweep else HALF_TURN to sweep
    }
}

/** Geometry of [ProgressRing]: clockwise from the top in LTR, counter-clockwise in RTL. */
internal object ProgressRingGeometry {
    /** Start angle of the indicator (the top of the ring). */
    const val START_ANGLE: Float = -90f

    /** Sweep (degrees) of the indicator for [progress]. */
    fun sweep(
        progress: Float,
        rtl: Boolean,
    ): Float {
        val sweep = FULL_TURN * unitFraction(progress)
        return if (rtl) -sweep else sweep
    }
}
