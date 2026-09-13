/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/** How the device is held (T-1303), from the dominant axis of gravity. */
enum class DeviceOrientation {
    /** Lying flat, screen up or down: a two-axis bubble. */
    FLAT,

    /** Upright on its short edge. */
    PORTRAIT,

    /** Upright on its long edge. */
    LANDSCAPE,
}

/** Tilt away from level in degrees: [x] across the screen, [y] along it (or towards/away from the viewer). */
data class Tilt(
    val x: Double,
    val y: Double,
) {
    operator fun minus(other: Tilt): Tilt = Tilt(x - other.x, y - other.y)
}

/** Bubble-level geometry on a gravity vector in device coordinates (own derivation; docs/PROVENANCE.md). */
object LevelMath {
    /** A reading within this many degrees of zero on both axes counts as level. */
    const val LEVEL_TOLERANCE_DEGREES: Double = 0.5

    private const val HALF_TURN = 180.0
    private const val NO_GRAVITY = 1e-6

    /** The orientation whose axis carries most of [gravity]; `null` when there is no gravity (free fall). */
    fun classify(gravity: Vector3): DeviceOrientation? {
        val ax = abs(gravity.x)
        val ay = abs(gravity.y)
        val az = abs(gravity.z)
        return when {
            gravity.norm < NO_GRAVITY -> null
            az >= ax && az >= ay -> DeviceOrientation.FLAT
            ay >= ax -> DeviceOrientation.PORTRAIT
            else -> DeviceOrientation.LANDSCAPE
        }
    }

    /**
     * The raw tilt of [gravity] held in [orientation]: flat → angles of the screen plane against the horizon along x
     * and y; portrait/landscape → the rotation of the upright edge away from vertical (x) and the lean towards or away
     * from the viewer (y). Screen-up and screen-down (or upside-down) read the same.
     */
    fun tilt(
        gravity: Vector3,
        orientation: DeviceOrientation,
    ): Tilt {
        val norm = gravity.norm.coerceAtLeast(NO_GRAVITY)

        fun elevation(component: Double) = degrees(asin((component / norm).coerceIn(-1.0, 1.0)))
        val lean = elevation(gravity.z)
        return when (orientation) {
            DeviceOrientation.FLAT -> Tilt(elevation(gravity.x), elevation(gravity.y))
            DeviceOrientation.PORTRAIT -> Tilt(degrees(atan2(gravity.x, abs(gravity.y))), lean)
            DeviceOrientation.LANDSCAPE -> Tilt(degrees(atan2(gravity.y, abs(gravity.x))), lean)
        }
    }

    /** [raw] tilt corrected by the stored offset of [orientation]. */
    fun calibrated(
        raw: Tilt,
        orientation: DeviceOrientation,
        calibration: LevelCalibration,
    ): Tilt = calibration.offsets[orientation]?.let { raw - it } ?: raw

    /** Whether [tilt] is within [LEVEL_TOLERANCE_DEGREES] on both axes (only x matters when upright). */
    fun isLevel(
        tilt: Tilt,
        orientation: DeviceOrientation,
    ): Boolean =
        abs(tilt.x) <= LEVEL_TOLERANCE_DEGREES &&
            (orientation != DeviceOrientation.FLAT || abs(tilt.y) <= LEVEL_TOLERANCE_DEGREES)

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}

/** Units of the on-screen ruler. */
enum class RulerUnit {
    CENTIMETERS,
    INCHES,
}

/** How long a ruler tick is drawn. */
enum class TickKind {
    MAJOR,
    MEDIUM,
    MINOR,
}

/** A ruler tick [positionPx] from the ruler's start; major ticks carry the whole-unit [label]. */
data class RulerTick(
    val positionPx: Float,
    val kind: TickKind,
    val label: Int?,
)

/** Ruler ticks from the screen's physical density (T-1303). */
object RulerMath {
    const val MILLIMETERS_PER_INCH: Float = 25.4f
    private const val MM_PER_CM = 10
    private const val MEDIUM_MM = 5
    private const val SIXTEENTHS_PER_INCH = 16
    private const val SIXTEENTHS_PER_HALF_INCH = 8

    /** Ticks over [lengthPx] at [dotsPerInch]: millimetres (centimetre labels) or sixteenths of an inch. */
    fun ticks(
        lengthPx: Float,
        dotsPerInch: Float,
        unit: RulerUnit,
    ): List<RulerTick> {
        require(dotsPerInch > 0f) { "dotsPerInch must be positive" }
        val divisionsPerUnit = if (unit == RulerUnit.CENTIMETERS) MM_PER_CM else SIXTEENTHS_PER_INCH
        val unitPx = if (unit == RulerUnit.CENTIMETERS) dotsPerInch * MM_PER_CM / MILLIMETERS_PER_INCH else dotsPerInch
        val stepPx = unitPx / divisionsPerUnit
        val count = (lengthPx / stepPx).toInt()
        return (0..count).map { index ->
            val kind =
                when {
                    index % divisionsPerUnit == 0 -> {
                        TickKind.MAJOR
                    }

                    index % (if (unit == RulerUnit.CENTIMETERS) MEDIUM_MM else SIXTEENTHS_PER_HALF_INCH) == 0 -> {
                        TickKind.MEDIUM
                    }

                    else -> {
                        TickKind.MINOR
                    }
                }
            RulerTick(index * stepPx, kind, (index / divisionsPerUnit).takeIf { kind == TickKind.MAJOR })
        }
    }
}
