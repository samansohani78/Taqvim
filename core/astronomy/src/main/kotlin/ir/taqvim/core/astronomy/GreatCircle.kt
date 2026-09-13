/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle navigation on a spherical Earth (A-11). */
public object GreatCircle {
    /** IUGG mean Earth radius, kilometres. */
    public const val EARTH_MEAN_RADIUS_KM: Double = 6_371.0088

    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0
    private const val POLE = 90.0
    private const val DEGENERATE_RADIANS = 1e-7

    /** Central angle between [from] and [to] in radians (haversine formula), `0..π`. */
    public fun centralAngleRadians(
        from: Coordinates,
        to: Coordinates,
    ): Double {
        val latitudeDelta = radians(to.latitude - from.latitude)
        val longitudeDelta = radians(to.longitude - from.longitude)
        val haversine =
            sin(latitudeDelta / 2).pow(2) +
                cos(radians(from.latitude)) * cos(radians(to.latitude)) * sin(longitudeDelta / 2).pow(2)
        return 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    /** Surface distance between [from] and [to] in kilometres. */
    public fun distanceKm(
        from: Coordinates,
        to: Coordinates,
    ): Double = centralAngleRadians(from, to) * EARTH_MEAN_RADIUS_KM

    /**
     * Initial bearing from [from] towards [to] in degrees clockwise from true north, `0 ≤ bearing < 360`.
     * `null` where no single direction exists: coincident or antipodal points (every great circle joins them) or
     * [from] at a pole.
     */
    public fun initialBearingDegrees(
        from: Coordinates,
        to: Coordinates,
    ): Double? {
        val angle = centralAngleRadians(from, to)
        if (angle < DEGENERATE_RADIANS || PI - angle < DEGENERATE_RADIANS || abs(from.latitude) == POLE) return null
        val fromLatitude = radians(from.latitude)
        val toLatitude = radians(to.latitude)
        val longitudeDelta = radians(to.longitude - from.longitude)
        val y = sin(longitudeDelta) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
        return (atan2(y, x) * HALF_TURN / PI).mod(FULL_TURN)
    }

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN
}

/** Direction of prayer towards the Kaaba (A-11). */
public object Qibla {
    /** The Kaaba in Mecca, per docs/PLAN.md §6 A-11. */
    public val KAABA: Coordinates = Coordinates(latitude = 21.4225, longitude = 39.8262)

    /** Qibla bearing from [location] in degrees clockwise from true north; `null` at the Kaaba or its antipode. */
    public fun bearingDegrees(location: Coordinates): Double? = GreatCircle.initialBearingDegrees(location, KAABA)

    /** Great-circle distance from [location] to the Kaaba in kilometres. */
    public fun distanceKm(location: Coordinates): Double = GreatCircle.distanceKm(location, KAABA)
}
