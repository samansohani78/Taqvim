/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Angle arithmetic on the compass circle, in degrees. */
object Angles {
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    /** [degrees] wrapped into `0 ≤ result < 360`. */
    fun wrap360(degrees: Double): Double {
        val wrapped = degrees - FULL_TURN * floor(degrees / FULL_TURN)
        return if (wrapped >= FULL_TURN) 0.0 else wrapped
    }

    /** Shortest signed turn from [from] to [to]: `−180 ≤ result < 180`, positive clockwise. */
    fun delta(
        from: Double,
        to: Double,
    ): Double = wrap360(to - from + HALF_TURN) - HALF_TURN

    /**
     * One step of an exponential low-pass filter on the circle: the result moves [alpha] (0‥1] of the way from
     * [previous] towards [sample] along the shorter arc, so 359° and 1° average to 0°, not 180°.
     */
    fun lowPass(
        previous: Double?,
        sample: Double,
        alpha: Double,
    ): Double {
        require(alpha > 0.0 && alpha <= 1.0) { "alpha must be in (0, 1]" }
        return previous?.let { wrap360(it + alpha * delta(it, sample)) } ?: wrap360(sample)
    }

    /** Circular mean of [degrees]; `null` when empty or when the directions cancel out. */
    fun mean(degrees: List<Double>): Double? {
        val x = degrees.sumOf { cos(it * PI / HALF_TURN) }
        val y = degrees.sumOf { sin(it * PI / HALF_TURN) }
        return if (abs(x) < CANCELLED && abs(y) < CANCELLED) null else wrap360(atan2(y, x) * HALF_TURN / PI)
    }

    private const val CANCELLED = 1e-9
}

/**
 * TalkBack announcements every [STEP_DEGREES] (T-1302): the announced bucket changes only once the heading has moved
 * [HYSTERESIS_DEGREES] past a bucket boundary, so a heading jittering on a boundary is not announced repeatedly.
 */
object HeadingAnnouncements {
    const val STEP_DEGREES: Int = 15
    const val HYSTERESIS_DEGREES: Double = 3.0

    /** The bucket (a multiple of [STEP_DEGREES] in 0‥345) to announce for [heading] after [previous]. */
    fun next(
        previous: Int?,
        heading: Double,
    ): Int {
        val nearest = bucketOf(heading)
        if (previous == null || previous == nearest) return nearest
        val fromPreviousCentre = abs(Angles.delta(previous.toDouble(), heading))
        return if (fromPreviousCentre >= STEP_DEGREES / 2.0 + HYSTERESIS_DEGREES) nearest else previous
    }

    private fun bucketOf(heading: Double): Int {
        val index = floor((Angles.wrap360(heading) + STEP_DEGREES / 2.0) / STEP_DEGREES).toInt()
        return (index * STEP_DEGREES) % FULL_TURN_DEGREES
    }

    private const val FULL_TURN_DEGREES = 360
}
