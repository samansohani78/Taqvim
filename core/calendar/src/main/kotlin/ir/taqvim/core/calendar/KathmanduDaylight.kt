/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise and sunset at Kathmandu in Nepal Standard Time, for the night rules of Makara and Karka sankranti (A-07).
 *
 * Low-precision solar coordinates and the equation of time from J. Meeus, *Astronomical Algorithms* 2nd ed., ch. 25
 * and 28 (eq. 28.3), with the standard −0°50′ horizon (ch. 15). Within a few thousand years of J2000 this matches the
 * national panchang's printed times to about two minutes. The polynomials are only evaluated within
 * ±[MAX_CENTURIES] centuries of J2000; farther out that epoch's values are used, so every day still gets a sunrise.
 */
internal object KathmanduDaylight {
    private const val LATITUDE = 27.7172
    private const val LONGITUDE = 85.3240
    private const val NEPAL_OFFSET_MINUTES = 345.0
    private const val HORIZON_ALTITUDE = -0.8333
    private const val MINUTES_PER_DAY = 1_440.0
    private const val NOON_MINUTES = 720.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val J2000_JDN = 2_451_545L
    private const val DAYS_PER_CENTURY = 36_525.0
    private const val MAX_CENTURIES = 200.0
    private const val ITERATIONS = 3
    private const val FULL_TURN = 360.0
    private const val HALF_TURN = 180.0

    /** Sunrise on civil day [jdn], as the fraction of that Nepal day elapsed. */
    fun sunrise(jdn: Long): Double = event(jdn, rising = true)

    /** Sunset on civil day [jdn], as the fraction of that Nepal day elapsed. */
    fun sunset(jdn: Long): Double = event(jdn, rising = false)

    private fun event(
        jdn: Long,
        rising: Boolean,
    ): Double {
        var minutesUt = NOON_MINUTES - NEPAL_OFFSET_MINUTES
        repeat(ITERATIONS) {
            val sun = solar(jdn, minutesUt / MINUTES_PER_DAY)
            val hourAngle = hourAngle(sun.declination)
            val transit = NOON_MINUTES - MINUTES_PER_DEGREE * LONGITUDE - sun.equationOfTimeMinutes
            minutesUt = transit + if (rising) -MINUTES_PER_DEGREE * hourAngle else MINUTES_PER_DEGREE * hourAngle
        }
        return (minutesUt + NEPAL_OFFSET_MINUTES) / MINUTES_PER_DAY
    }

    private fun hourAngle(declination: Double): Double {
        val latitude = radians(LATITUDE)
        val cosine =
            (sin(radians(HORIZON_ALTITUDE)) - sin(latitude) * sin(declination)) / (cos(latitude) * cos(declination))
        return degrees(acos(cosine.coerceIn(-1.0, 1.0)))
    }

    /** Declination (radians) and equation of time (minutes) at [dayFraction] of UT day [jdn]. */
    @Suppress("MagicNumber") // Coefficients of Meeus ch. 25 and eq. 28.3.
    private fun solar(
        jdn: Long,
        dayFraction: Double,
    ): SolarState {
        val t = ((jdn - J2000_JDN - 0.5 + dayFraction) / DAYS_PER_CENTURY).coerceIn(-MAX_CENTURIES, MAX_CENTURIES)
        val meanLongitude = radians((280.46646 + 36_000.76983 * t + 0.0003032 * t * t).mod(FULL_TURN))
        val anomaly = radians((357.52911 + 35_999.05029 * t - 0.0001537 * t * t).mod(FULL_TURN))
        val eccentricity = 0.016708634 - 0.000042037 * t
        val centre =
            (1.914602 - 0.004817 * t) * sin(anomaly) + (0.019993 - 0.000101 * t) * sin(2 * anomaly) +
                0.000289 * sin(3 * anomaly)
        val node = radians(125.04 - 1_934.136 * t)
        val apparent = meanLongitude + radians(centre - 0.00569 - 0.00478 * sin(node))
        val obliquity = radians(meanObliquity(t) + 0.00256 * cos(node))
        val y = tan(obliquity / 2).let { it * it }
        val equation =
            y * sin(2 * meanLongitude) - 2 * eccentricity * sin(anomaly) +
                4 * eccentricity * y * sin(anomaly) * cos(2 * meanLongitude) - 0.5 * y * y * sin(4 * meanLongitude) -
                1.25 * eccentricity * eccentricity * sin(2 * anomaly)
        return SolarState(asin(sin(obliquity) * sin(apparent)), degrees(equation) * MINUTES_PER_DEGREE)
    }

    /** Mean obliquity in degrees (Meeus eq. 22.2). */
    @Suppress("MagicNumber") // Coefficients of Meeus eq. 22.2.
    private fun meanObliquity(t: Double): Double =
        23.0 + (26.0 + (21.448 - 46.8150 * t - 0.00059 * t * t + 0.001813 * t * t * t) / 60.0) / 60.0

    private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI

    private class SolarState(
        val declination: Double,
        val equationOfTimeMinutes: Double,
    )
}
