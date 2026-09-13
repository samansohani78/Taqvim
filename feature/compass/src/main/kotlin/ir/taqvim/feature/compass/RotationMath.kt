/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/** A vector in device or world coordinates. */
data class Vector3(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    val norm: Double get() = sqrt(x * x + y * y + z * z)

    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(factor: Double): Vector3 = Vector3(x * factor, y * factor, z * factor)

    infix fun cross(other: Vector3): Vector3 =
        Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    /** One step of an exponential low-pass filter from [previous] towards this sample. */
    fun lowPass(
        previous: Vector3?,
        alpha: Double,
    ): Vector3 = previous?.let { it + (this - it) * alpha } ?: this
}

/**
 * Rotation from device coordinates to world coordinates (x east, y magnetic north, z up), row by row as in the
 * Android `SensorManager` coordinate system: `world = R · device`.
 */
data class RotationMatrix(
    val rows: List<Double>,
) {
    init {
        require(rows.size == SIZE) { "a rotation matrix has 9 entries" }
    }

    operator fun get(index: Int): Double = rows[index]

    private companion object {
        const val SIZE = 9
    }
}

/** Orientation angles of a [RotationMatrix], in degrees. */
data class DeviceAngles(
    /** Bearing of the device's top edge (its y axis) from magnetic north, clockwise, 0‥360. */
    val azimuthDegrees: Double,
    /** Rotation about the x axis (top edge up is negative), −90‥90. */
    val pitchDegrees: Double,
    /** Rotation about the y axis, −180‥180. */
    val rollDegrees: Double,
)

/** Rotation matrices from sensor readings and the angles they imply (own derivation; see docs/PROVENANCE.md). */
object RotationMath {
    private const val HALF_TURN = 180.0
    private const val DEGENERATE = 1e-6

    /**
     * The rotation of a unit quaternion given as a rotation-vector reading `(x, y, z)` = axis · sin(θ/2); the scalar
     * part is recomputed as `√(1 − x² − y² − z²)`.
     */
    @Suppress("MagicNumber") // Entries of the standard quaternion-to-matrix formula.
    fun fromRotationVector(vector: Vector3): RotationMatrix {
        val (x, y, z) = vector
        val w = sqrt((1.0 - x * x - y * y - z * z).coerceAtLeast(0.0))
        return RotationMatrix(
            listOf(
                1 - 2 * (y * y + z * z),
                2 * (x * y - z * w),
                2 * (x * z + y * w),
                2 * (x * y + z * w),
                1 - 2 * (x * x + z * z),
                2 * (y * z - x * w),
                2 * (x * z - y * w),
                2 * (y * z + x * w),
                1 - 2 * (x * x + y * y),
            ),
        )
    }

    /**
     * The rotation implied by [gravity] (an accelerometer reading, pointing up at rest) and the [geomagnetic] field:
     * east = field × up, north = up × east. `null` when either is zero or they are parallel (free fall, magnetic pole,
     * strong interference).
     */
    fun fromGravityAndField(
        gravity: Vector3,
        geomagnetic: Vector3,
    ): RotationMatrix? {
        val east = geomagnetic cross gravity
        if (gravity.norm < DEGENERATE || east.norm < DEGENERATE * geomagnetic.norm.coerceAtLeast(1.0)) return null
        val up = gravity * (1.0 / gravity.norm)
        val eastUnit = east * (1.0 / east.norm)
        val north = up cross eastUnit
        return RotationMatrix(
            listOf(eastUnit.x, eastUnit.y, eastUnit.z, north.x, north.y, north.z, up.x, up.y, up.z),
        )
    }

    /** Azimuth, pitch and roll of [rotation]. */
    @Suppress("MagicNumber") // Matrix entry indices.
    fun angles(rotation: RotationMatrix): DeviceAngles =
        DeviceAngles(
            azimuthDegrees = Angles.wrap360(degrees(atan2(rotation[1], rotation[4]))),
            pitchDegrees = degrees(asin((-rotation[7]).coerceIn(-1.0, 1.0))),
            rollDegrees = degrees(atan2(-rotation[6], rotation[8])),
        )

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
