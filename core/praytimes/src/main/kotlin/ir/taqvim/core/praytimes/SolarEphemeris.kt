/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.siderealTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

/** The apparent geocentric Sun of date: [rightAscension] and [declination] in degrees, [distanceAu] from the Earth. */
internal data class SunCoordinates(
    val rightAscension: Double,
    val declination: Double,
    val distanceAu: Double,
)

/**
 * The Sun behind prayer times (A-10, ADR-0029), from cosinekitty/astronomy 2.1.19 (MIT): VSOP87 Earth corrected for
 * light time and aberration, rotated to the true equator and equinox of date (precession and nutation), on the
 * library's ΔT model, and Greenwich apparent sidereal time. Library types stay inside this object.
 *
 * Moments are library UT days: days after 12:00 UTC on 1 January 2000. The ephemeris serves days within
 * [RANGE_CYCLES] 400-year Gregorian cycles of J2000 (civil years −2000…6000). A day further out is moved by whole
 * cycles into that range ([localMidnight]): that keeps its month, day and weekday and so its season, whereas the
 * library's own values drift without bound so far from the present (its ΔT is a parabola, and at ±100 000 years the
 * declination leaves ±24°).
 */
internal object SolarEphemeris {
    /** Days in 400 Gregorian years. */
    const val GREGORIAN_CYCLE_DAYS: Long = 146_097

    /** Whole Gregorian cycles on each side of J2000 served directly by the ephemeris. */
    const val RANGE_CYCLES: Long = 10

    private const val RANGE_DAYS: Long = RANGE_CYCLES * GREGORIAN_CYCLE_DAYS
    private const val J2000_JDN: Long = 2_451_545
    private const val HALF_DAY = 0.5
    private const val MINUTES_PER_DAY = 1_440.0
    private const val DEGREES_PER_HOUR = 15.0
    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0

    /**
     * Library UT day of local midnight starting the civil day [jdn] at a place [utcOffsetMinutes] ahead of UTC, after
     * moving the day by whole Gregorian cycles into the ephemeris range. Exact in Long arithmetic before the one
     * conversion to Double, so any day whose distance from J2000 fits a Long works.
     */
    fun localMidnight(
        jdn: Long,
        utcOffsetMinutes: Int,
    ): Double {
        val days = Math.subtractExact(jdn, J2000_JDN)
        val folded = days - cyclesOutOfRange(days) * GREGORIAN_CYCLE_DAYS
        return folded - HALF_DAY - utcOffsetMinutes / MINUTES_PER_DAY
    }

    /** Whole Gregorian cycles [days] (from J2000) lies outside the ephemeris range, signed; 0 inside it. */
    fun cyclesOutOfRange(days: Long): Long =
        when {
            days > RANGE_DAYS -> (days - RANGE_DAYS - 1) / GREGORIAN_CYCLE_DAYS + 1
            days < -RANGE_DAYS -> -((-RANGE_DAYS - days - 1) / GREGORIAN_CYCLE_DAYS + 1)
            else -> 0
        }

    /** The apparent Sun of date at library UT day [ut]. */
    fun sun(ut: Double): SunCoordinates {
        val time = Time(ut)
        val vector = geoVector(Body.Sun, time, Aberration.Corrected)
        val ofDate = rotationEqjEqd(time).rotate(vector)
        return SunCoordinates(
            rightAscension = degrees(atan2(ofDate.y, ofDate.x)),
            declination = degrees(atan2(ofDate.z, hypot(ofDate.x, ofDate.y))),
            distanceAu = vector.length(),
        )
    }

    /** Greenwich apparent sidereal time at library UT day [ut], in degrees. */
    fun siderealDegrees(ut: Double): Double = siderealTime(Time(ut)) * DEGREES_PER_HOUR

    /**
     * Equation of time at library UT day [ut] in minutes: apparent minus mean solar time, from the Greenwich hour angle
     * of the apparent Sun (sidereal time minus right ascension) against the mean Sun's (UT from noon).
     */
    fun equationOfTimeMinutes(ut: Double): Double {
        val meanHourAngle = (ut - floor(ut)) * FULL_TURN
        val apparentHourAngle = siderealDegrees(ut) - sun(ut).rightAscension
        return normalizeHalfTurn(apparentHourAngle - meanHourAngle) * MINUTES_PER_DAY / FULL_TURN
    }

    /** [angle] in degrees brought into −180°‥180°. */
    fun normalizeHalfTurn(angle: Double): Double = angle - FULL_TURN * floor((angle + HALF_TURN) / FULL_TURN)

    private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
}
