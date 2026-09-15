/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import kotlin.time.Instant

/** Bodies supported by the façade. */
public enum class CelestialBody {
    SUN,
    MOON,
}

/** The four astronomical seasons' starting events. */
public enum class Season {
    MARCH_EQUINOX,
    JUNE_SOLSTICE,
    SEPTEMBER_EQUINOX,
    DECEMBER_SOLSTICE,
}

/** Equinox and solstice instants of one Gregorian year. */
public data class SeasonInstants(
    public val marchEquinox: Instant,
    public val juneSolstice: Instant,
    public val septemberEquinox: Instant,
    public val decemberSolstice: Instant,
) {
    /** The instant of [season]. */
    public fun of(season: Season): Instant =
        when (season) {
            Season.MARCH_EQUINOX -> marchEquinox
            Season.JUNE_SOLSTICE -> juneSolstice
            Season.SEPTEMBER_EQUINOX -> septemberEquinox
            Season.DECEMBER_SOLSTICE -> decemberSolstice
        }
}

/** Principal Moon phases, in order through a lunation. */
public enum class MoonQuarter {
    NEW_MOON,
    FIRST_QUARTER,
    FULL_MOON,
    THIRD_QUARTER,
}

/** A principal Moon phase and when it occurs. */
public data class MoonPhaseEvent(
    public val quarter: MoonQuarter,
    public val instant: Instant,
)

/**
 * How the Moon looks: the [illuminatedFraction] of the disc, whether it is [waxing], and whether the bright limb is on
 * the observer's right. Seen from the southern hemisphere the image is mirrored left to right.
 */
public data class MoonAppearance(
    public val illuminatedFraction: Double,
    public val waxing: Boolean,
    public val brightLimbOnRight: Boolean,
)

/**
 * Orientation of the Moon's bright limb for an observer, in degrees. [positionAngleDegrees] counts from celestial north
 * through east (0°‥360°); [parallacticAngleDegrees] is the position angle of the zenith direction (−180°‥180°); and
 * [tiltDegrees] = position angle − parallactic angle is the bright limb's direction from straight up in the observer's
 * view, counter-clockwise positive (−90° is on the right, +90° on the left), in −180°‥180°.
 */
public data class MoonTilt(
    public val positionAngleDegrees: Double,
    public val parallacticAngleDegrees: Double,
    public val tiltDegrees: Double,
)

/** Rise, meridian transit and set within one day after a start instant; `null` when the event does not happen. */
public data class RiseSetTransit(
    public val rise: Instant?,
    public val transit: Instant?,
    public val set: Instant?,
)

/** Geocentric ecliptic coordinates of date, in degrees. */
public data class EclipticPosition(
    public val latitudeDegrees: Double,
    public val longitudeDegrees: Double,
)

/** Topocentric position: horizon coordinates (with normal refraction) and equatorial coordinates of date. */
public data class SkyPosition(
    public val azimuthDegrees: Double,
    public val altitudeDegrees: Double,
    public val rightAscensionHours: Double,
    public val declinationDegrees: Double,
)

/** Optical libration of the Moon and its geocentric distance and apparent diameter. */
public data class Libration(
    public val latitudeDegrees: Double,
    public val longitudeDegrees: Double,
    public val distanceKm: Double,
    public val diameterDegrees: Double,
)
