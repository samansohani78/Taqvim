/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.siderealTime as librarySiderealTime
import ir.taqvim.core.model.Coordinates
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Instant

/** A chart's angles and Placidus cusps; all values are ecliptic longitudes in degrees (0‥360). */
public data class HouseCusps(
    public val ascendant: Double,
    public val midheaven: Double,
    /** Cusps 1‥12; cusp 1 is the ascendant and cusp 10 the midheaven. */
    public val cusps: List<Double>,
)

/** Arabic parts (lots) of a chart, in ecliptic longitude degrees. */
public data class Lots(
    public val fortune: Double,
    public val spirit: Double,
    /** Whether the Sun was above the horizon, which decides the formulas. */
    public val dayChart: Boolean,
)

/**
 * Chart angles, Placidus houses and lots (A-14). Local sidereal time comes from the astronomy engine; ecliptic ↔
 * equatorial conversions and the ascendant/midheaven formulas follow J. Meeus, *Astronomical Algorithms*; Placidus
 * cusps divide each point's semi-arc in thirds, as R. Plantiko, *On Dividing the Sky* (2004), definition VI and §5.5
 * define it. See docs/PROVENANCE.md (A-14).
 */
public object Houses {
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0
    private const val RIGHT_ANGLE = 90.0
    private const val DEGREES_PER_HOUR = 15.0
    private const val THIRD = 1.0 / 3.0
    private const val PLACIDUS_ITERATIONS = 30
    private const val CUSPS = 12
    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_587.5
    private const val JULIAN_DAY_OF_J2000 = 2_451_545.0
    private const val DAYS_PER_CENTURY = 36_525.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** True obliquity of the ecliptic at [instant] in degrees (mean obliquity plus the main nutation term). */
    @Suppress("MagicNumber") // Published series coefficients (NOAA Solar Calculations, after Meeus).
    public fun obliquityDegrees(instant: Instant): Double {
        val julianDay = instant.toEpochMilliseconds() / MILLIS_PER_DAY + JULIAN_DAY_OF_UNIX_EPOCH
        val t = (julianDay - JULIAN_DAY_OF_J2000) / DAYS_PER_CENTURY
        val mean = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
        return mean + 0.00256 * cos(radians(125.04 - 1934.136 * t))
    }

    /** Right ascension of the midheaven (local apparent sidereal time as an angle) at [place] and [instant]. */
    public fun ramcDegrees(
        instant: Instant,
        place: Coordinates,
    ): Double = wrap(librarySiderealTime(instant.toAstronomyTime()) * DEGREES_PER_HOUR + place.longitude)

    /** Angles and Placidus cusps; `null` inside the polar circles, where Placidus houses are undefined. */
    public fun placidus(
        instant: Instant,
        place: Coordinates,
    ): HouseCusps? {
        val obliquity = radians(obliquityDegrees(instant))
        if (abs(place.latitude) >= RIGHT_ANGLE - degrees(obliquity)) return null
        val ramc = ramcDegrees(instant, place)
        val latitude = radians(place.latitude)
        val midheaven = longitudeOfRightAscension(ramc, obliquity)
        val ascendant = ascendant(ramc, latitude, obliquity)
        val cusp11 = placidusCusp(ramc, THIRD, above = true, latitude, obliquity)
        val cusp12 = placidusCusp(ramc, 2 * THIRD, above = true, latitude, obliquity)
        val cusp2 = placidusCusp(ramc, 2 * THIRD, above = false, latitude, obliquity)
        val cusp3 = placidusCusp(ramc, THIRD, above = false, latitude, obliquity)
        val firstSix =
            listOf(
                ascendant,
                cusp2,
                cusp3,
                wrap(midheaven + HALF_TURN),
                wrap(cusp11 + HALF_TURN),
                wrap(cusp12 + HALF_TURN),
            )
        val cusps = firstSix + firstSix.map { wrap(it + HALF_TURN) }
        return HouseCusps(ascendant, midheaven, cusps.take(CUSPS))
    }

    /** Part of Fortune and Part of Spirit for an [ascendant] and the Sun's and Moon's longitudes. */
    public fun lots(
        ascendant: Double,
        sunLongitude: Double,
        moonLongitude: Double,
        dayChart: Boolean,
    ): Lots {
        val moonMinusSun = moonLongitude - sunLongitude
        val fortune = if (dayChart) ascendant + moonMinusSun else ascendant - moonMinusSun
        val spirit = if (dayChart) ascendant - moonMinusSun else ascendant + moonMinusSun
        return Lots(wrap(fortune), wrap(spirit), dayChart)
    }

    /** Lots for the chart of [instant] at [place]; day or night from the Sun's apparent altitude. */
    public fun lots(
        instant: Instant,
        place: Coordinates,
        ascendant: Double,
    ): Lots =
        lots(
            ascendant = ascendant,
            sunLongitude = Sky.eclipticPosition(CelestialBody.SUN, instant).longitudeDegrees,
            moonLongitude = Sky.eclipticPosition(CelestialBody.MOON, instant).longitudeDegrees,
            dayChart = Sky.skyPosition(CelestialBody.SUN, instant, place).altitudeDegrees > 0,
        )

    /** Ecliptic longitude of the ecliptic point with right ascension [rightAscension] (degrees). */
    internal fun longitudeOfRightAscension(
        rightAscension: Double,
        obliquity: Double,
    ): Double = wrap(degrees(atan2(sin(radians(rightAscension)), cos(radians(rightAscension)) * cos(obliquity))))

    private fun ascendant(
        ramc: Double,
        latitude: Double,
        obliquity: Double,
    ): Double {
        val theta = radians(ramc)
        val y = -cos(theta)
        val x = sin(obliquity) * tan(latitude) + cos(obliquity) * sin(theta)
        return wrap(degrees(atan2(y, x)) + HALF_TURN)
    }

    /**
     * Placidus cusp whose point is [fraction] of its diurnal (above the horizon, houses 11–12) or nocturnal (houses
     * 2–3) semi-arc away from the meridian, found by fixed-point iteration on the point's declination. Equivalently,
     * the point whose temporal mundane position is a whole multiple of 30° — the form `HousesTest` asserts against.
     */
    private fun placidusCusp(
        ramc: Double,
        fraction: Double,
        above: Boolean,
        latitude: Double,
        obliquity: Double,
    ): Double {
        var longitude = wrap(ramc + (if (above) 1 else 2) * RIGHT_ANGLE * fraction * 2)
        repeat(PLACIDUS_ITERATIONS) {
            val declination = asin(sin(obliquity) * sin(radians(longitude)))
            val diurnalSemiArc = degrees(acos((-tan(latitude) * tan(declination)).coerceIn(-1.0, 1.0)))
            val rightAscension =
                if (above) {
                    ramc + fraction * diurnalSemiArc
                } else {
                    ramc + HALF_TURN - fraction * (HALF_TURN - diurnalSemiArc)
                }
            longitude = longitudeOfRightAscension(rightAscension, obliquity)
        }
        return longitude
    }

    private fun wrap(degrees: Double): Double = degrees - FULL_TURN * floor(degrees / FULL_TURN)

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
