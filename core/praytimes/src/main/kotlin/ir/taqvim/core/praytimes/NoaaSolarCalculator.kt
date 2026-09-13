/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Instant

/** Sun parameters at a moment (A-09): [declinationDegrees] and [equationOfTimeMinutes]. */
public data class SolarParameters(
    public val julianCentury: Double,
    public val declinationDegrees: Double,
    public val equationOfTimeMinutes: Double,
)

/**
 * The Sun seen from a place: [zenithDegrees] (geometric), [elevationDegrees] = 90° − zenith, the approximate
 * atmospheric [refractionDegrees], and [azimuthDegrees] clockwise from north.
 */
public data class HorizontalPosition(
    public val zenithDegrees: Double,
    public val elevationDegrees: Double,
    public val refractionDegrees: Double,
    public val azimuthDegrees: Double,
) {
    /** Elevation including refraction, as an observer sees it. */
    public val apparentElevationDegrees: Double
        get() = elevationDegrees + refractionDegrees
}

/**
 * Solar noon, sunrise and sunset of a day, in minutes after local midnight; sunrise and sunset are `null` on polar
 * days and nights.
 */
public data class SolarDay(
    public val solarNoonMinutes: Double,
    public val sunriseMinutes: Double?,
    public val sunsetMinutes: Double?,
)

/**
 * NOAA Solar Calculator equations (A-09), transcribed from NOAA's public-domain "NOAA Solar Calculations" spreadsheet
 * (after Meeus, *Astronomical Algorithms*); NOAA states they are valid for 1901–2099. See docs/PROVENANCE.md (A-09).
 */
public object NoaaSolarCalculator {
    /** Zenith used for sunrise and sunset: refraction and the solar disk's radius included. */
    public const val SUNRISE_ZENITH_DEGREES: Double = 90.833

    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_587.5
    private const val JULIAN_DAY_OF_J2000 = 2_451_545.0
    private const val DAYS_PER_JULIAN_CENTURY = 36_525.0
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val MINUTES_PER_DAY = 1_440.0
    private const val NOON_MINUTES = 720.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val MINUTES_PER_HOUR = 60.0
    private const val RIGHT_ANGLE = 90.0
    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0
    private const val THREE_HALF_TURNS = 540.0

    /** Julian Day (fractional) of [instant]. */
    public fun julianDay(instant: Instant): Double =
        instant.toEpochMilliseconds() / MILLIS_PER_DAY + JULIAN_DAY_OF_UNIX_EPOCH

    /** Declination and equation of time at [julianDay]. */
    @Suppress("MagicNumber") // Published series coefficients; see the class KDoc.
    public fun parameters(julianDay: Double): SolarParameters {
        val t = (julianDay - JULIAN_DAY_OF_J2000) / DAYS_PER_JULIAN_CENTURY
        val meanLongitude = wrap(280.46646 + t * (36_000.76983 + t * 0.0003032), FULL_TURN)
        val meanAnomaly = radians(357.52911 + t * (35_999.05029 - 0.0001537 * t))
        val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val equationOfCenter =
            sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) + sin(3 * meanAnomaly) * 0.000289
        val node = radians(125.04 - 1934.136 * t)
        val apparentLongitude = radians(meanLongitude + equationOfCenter - 0.00569 - 0.00478 * sin(node))
        val meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
        val obliquity = radians(meanObliquity + 0.00256 * cos(node))
        val y = tan(obliquity / 2).pow(2)
        val l0 = radians(meanLongitude)
        val e = eccentricity
        val equationOfTime =
            MINUTES_PER_DEGREE *
                degrees(
                    y * sin(2 * l0) - 2 * e * sin(meanAnomaly) + 4 * e * y * sin(meanAnomaly) * cos(2 * l0) -
                        0.5 * y * y * sin(4 * l0) - 1.25 * e * e * sin(2 * meanAnomaly),
                )
        return SolarParameters(t, degrees(asin(sin(obliquity) * sin(apparentLongitude))), equationOfTime)
    }

    /** Hour angle (degrees, positive) at which the Sun reaches [zenithDegrees]; `null` if it never does that day. */
    public fun hourAngleDegrees(
        latitudeDegrees: Double,
        declinationDegrees: Double,
        zenithDegrees: Double = SUNRISE_ZENITH_DEGREES,
    ): Double? {
        val latitude = radians(latitudeDegrees)
        val declination = radians(declinationDegrees)
        val cosine = cos(radians(zenithDegrees)) / (cos(latitude) * cos(declination)) - tan(latitude) * tan(declination)
        return if (cosine in -1.0..1.0) degrees(acos(cosine)) else null
    }

    /** Solar noon, sunrise and sunset for the day containing [julianDay], at a place [utcOffsetHours] from UTC. */
    public fun solarDay(
        julianDay: Double,
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        utcOffsetHours: Double,
    ): SolarDay {
        val sun = parameters(julianDay)
        val noon =
            NOON_MINUTES - MINUTES_PER_DEGREE * longitudeDegrees - sun.equationOfTimeMinutes +
                utcOffsetHours * MINUTES_PER_HOUR
        val halfDay = hourAngleDegrees(latitudeDegrees, sun.declinationDegrees)?.let { it * MINUTES_PER_DEGREE }
        return SolarDay(noon, halfDay?.let { noon - it }, halfDay?.let { noon + it })
    }

    /** Where the Sun is at [instant] as seen from [latitudeDegrees], [longitudeDegrees]. Never NaN. */
    public fun horizontal(
        instant: Instant,
        latitudeDegrees: Double,
        longitudeDegrees: Double,
    ): HorizontalPosition {
        val sun = parameters(julianDay(instant))
        val minutesUtc = instant.toEpochMilliseconds().mod(MILLIS_PER_DAY.toLong()) / MILLIS_PER_MINUTE
        val trueSolarTime =
            wrap(minutesUtc + sun.equationOfTimeMinutes + MINUTES_PER_DEGREE * longitudeDegrees, MINUTES_PER_DAY)
        val hourAngle = trueSolarTime / MINUTES_PER_DEGREE - HALF_TURN
        val latitude = radians(latitudeDegrees)
        val declination = radians(sun.declinationDegrees)
        val zenith =
            degrees(
                acosClamped(
                    sin(latitude) * sin(declination) + cos(latitude) * cos(declination) * cos(radians(hourAngle)),
                ),
            )
        val elevation = RIGHT_ANGLE - zenith
        return HorizontalPosition(
            zenith,
            elevation,
            refractionDegrees(elevation),
            azimuth(latitude, declination, zenith, hourAngle),
        )
    }

    private fun azimuth(
        latitude: Double,
        declination: Double,
        zenithDegrees: Double,
        hourAngleDegrees: Double,
    ): Double {
        val denominator = cos(latitude) * sin(radians(zenithDegrees))
        if (abs(denominator) < DEGENERATE) return 0.0
        val cosZenith = cos(radians(zenithDegrees))
        val angle = degrees(acosClamped((sin(latitude) * cosZenith - sin(declination)) / denominator))
        val afternoon = hourAngleDegrees > 0
        return wrap(if (afternoon) angle + HALF_TURN else THREE_HALF_TURNS - angle, FULL_TURN)
    }

    /** NOAA's piecewise approximation of atmospheric refraction, in degrees, for a geometric [elevation]. */
    @Suppress("MagicNumber") // Published coefficients.
    private fun refractionDegrees(elevation: Double): Double {
        val arcSeconds =
            when {
                elevation > 85 -> {
                    0.0
                }

                elevation > 5 -> {
                    val t = tan(radians(elevation))
                    58.1 / t - 0.07 / t.pow(3) + 0.000086 / t.pow(5)
                }

                elevation > -0.575 -> {
                    1735 + elevation * (-518.2 + elevation * (103.4 + elevation * (-12.79 + elevation * 0.711)))
                }

                else -> {
                    -20.772 / tan(radians(elevation))
                }
            }
        return arcSeconds / ARC_SECONDS_PER_DEGREE
    }

    private const val DEGENERATE = 1e-12
    private const val ARC_SECONDS_PER_DEGREE = 3_600.0

    private fun acosClamped(value: Double): Double = acos(value.coerceIn(-1.0, 1.0))

    /** [value] reduced into `[0, period)`, like the spreadsheet's MOD (floor modulo). */
    private fun wrap(
        value: Double,
        period: Double,
    ): Double = value - period * floor(value / period)

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
