/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoMoon as libraryGeoMoon
import io.github.cosinekitty.astronomy.geoVector as libraryGeoVector
import io.github.cosinekitty.astronomy.rotationEqjEqd as libraryRotationEqjEqd
import kotlin.math.abs
import kotlin.math.sqrt

/** Earth's equatorial radius (IERS Conventions 2010). */
private const val EARTH_EQUATORIAL_RADIUS_KM = 6378.1366

/** Earth's flattening (IERS Conventions 2010). */
private const val EARTH_FLATTENING = 1.0 / 298.25642

/** Stretch of z coordinates that maps Earth's ellipsoid onto a sphere of equatorial radius. */
private const val POLAR_STRETCH = 1.0 / (1.0 - EARTH_FLATTENING)

/** Nominal solar radius (IAU 2015 Resolution B3). */
private const val SUN_RADIUS_KM = 695_700.0

/** Lunar radius of the Besselian elements, k = 0.2725076 Earth equatorial radii. */
private const val MOON_RADIUS_KM = 0.2725076 * EARTH_EQUATORIAL_RADIUS_KM

/** A geocentric position in kilometres. */
internal data class Km3(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    operator fun plus(other: Km3): Km3 = Km3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Km3): Km3 = Km3(x - other.x, y - other.y, z - other.z)

    operator fun times(factor: Double): Km3 = Km3(x * factor, y * factor, z * factor)

    infix fun dot(other: Km3): Double = x * other.x + y * other.y + z * other.z

    fun length(): Double = sqrt(this dot this)
}

/**
 * The Moon's shadow axis at an instant, in the terms of Meeus, *Astronomical Algorithms* (2nd ed.), ch. 54: [gamma]
 * is the least distance of the axis from Earth's centre and [umbraRadius] the signed radius of the shadow cone at the
 * point of Earth's surface closest to the axis (positive: umbra, the eclipse is total there; negative: antumbra,
 * annular), both in Earth equatorial radii.
 *
 * Positions are geocentric, equator of date, with z stretched by a/b so that Earth's ellipsoid becomes a sphere of
 * equatorial radius. The stretch is affine, so whether a line meets the ellipsoid is unchanged (Meeus approximates the
 * same with his factor 0.9972); the cone radius is distorted by at most the flattening (0.3 %).
 */
internal class ShadowAxis(
    val gamma: Double,
    val umbraRadius: Double,
    private val time: Time,
    private val sun: Km3,
    private val moon: Km3,
    private val surfacePoint: Km3,
) {
    /** Geodetic latitude and longitude of the point of Earth's surface closest to the axis. */
    fun surfacePlace(): Observer =
        Vector(
            surfacePoint.x / KM_PER_AU,
            surfacePoint.y / KM_PER_AU,
            surfacePoint.z / POLAR_STRETCH / KM_PER_AU,
            time,
        ).toObserver(EquatorEpoch.OfDate)

    /**
     * Fraction of the Sun's disk area that the Moon covers at the surface point: 1 inside the umbra, the squared ratio
     * of the apparent radii inside the antumbra.
     */
    fun obscuration(): Double {
        val moonRadius = MOON_RADIUS_KM / (surfacePoint - moon).length()
        val sunRadius = SUN_RADIUS_KM / (surfacePoint - sun).length()
        val ratio = moonRadius / sunRadius
        return (ratio * ratio).coerceAtMost(1.0)
    }
}

/** Shadow geometry of solar eclipses (A-13), used where the library only reports whether the axis meets Earth. */
internal object SolarEclipseShadow {
    /** The shadow axis at [time] from the library's geocentric Sun and Moon positions. */
    fun at(time: Time): ShadowAxis {
        val rotation = libraryRotationEqjEqd(time)
        val sun = rotation.rotate(libraryGeoVector(Body.Sun, time, Aberration.None)).toStretchedKm()
        val moon = rotation.rotate(libraryGeoMoon(time)).toStretchedKm()
        return fromPositions(time, sun, moon)
    }

    /** The shadow axis for stretched geocentric [sun] and [moon] positions (see [ShadowAxis]). */
    fun fromPositions(
        time: Time,
        sun: Km3,
        moon: Km3,
    ): ShadowAxis {
        val sunToMoon = moon - sun
        val separation = sunToMoon.length()
        val axis = sunToMoon * (1.0 / separation)
        val closest = moon + axis * -(moon dot axis)
        val distance = closest.length()
        // An axis through Earth's centre has no closest surface point; the cone is then measured at the centre.
        val surface = if (distance > 0.0) closest * (EARTH_EQUATORIAL_RADIUS_KM / distance) else closest
        val alongAxis = (surface - moon) dot axis
        val cone = MOON_RADIUS_KM - alongAxis * (SUN_RADIUS_KM - MOON_RADIUS_KM) / separation
        return ShadowAxis(
            gamma = distance / EARTH_EQUATORIAL_RADIUS_KM,
            umbraRadius = cone / EARTH_EQUATORIAL_RADIUS_KM,
            time = time,
            sun = sun,
            moon = moon,
            surfacePoint = surface,
        )
    }

    /**
     * The kind of an eclipse from its shadow axis (Meeus ch. 54): total or annular when the cone still reaches Earth
     * (`gamma − 1 ≤ |umbraRadius|`; an umbra gives total, an antumbra or a cone vertex on the surface annular), partial
     * otherwise.
     */
    fun kind(
        gamma: Double,
        umbraRadius: Double,
    ): EclipseKind =
        when {
            gamma - 1.0 > abs(umbraRadius) -> EclipseKind.PARTIAL
            umbraRadius > 0.0 -> EclipseKind.TOTAL
            else -> EclipseKind.ANNULAR
        }

    private fun Vector.toStretchedKm(): Km3 = Km3(x * KM_PER_AU, y * KM_PER_AU, z * KM_PER_AU * POLAR_STRETCH)
}
