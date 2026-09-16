/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Sun over one civil day at one place, after Meeus, *Astronomical Algorithms* 2nd ed., ch. 15 ("Rising,
 * Transit, and Setting"): the apparent right ascension and declination at local midnight, noon and the next midnight
 * are interpolated quadratically, and every event is refined until the Sun's hour angle matches it. Times are minutes
 * after local midnight and may leave 0‥1440 when an event belongs to the neighbouring civil day (e.g. a late Isha).
 *
 * [latitude] and the meridian are degrees (east positive); [at] reuses the day's ephemeris at another latitude.
 */
internal class SunDay private constructor(
    private val nodes: DayNodes,
    val latitude: Double,
    private val longitude: Double,
) {
    /** The Sun's distance from the Earth at local noon, in astronomical units. */
    val distanceAu: Double
        get() = nodes.distanceAu

    /** Local apparent noon: the Sun on the meridian (upper transit), in minutes after local midnight. */
    val transit: Double = findTransit() * MINUTES_PER_DAY

    /** The same day and meridian at [latitude]. */
    fun at(latitude: Double): SunDay = SunDay(nodes, latitude, longitude)

    /** The Sun's apparent declination at [minutes] after local midnight, in degrees. */
    fun declinationAt(minutes: Double): Double = nodes.declination(minutes / MINUTES_PER_DAY)

    /** The Sun's geometric (airless) altitude at [minutes] after local midnight, in degrees. */
    fun altitudeAt(minutes: Double): Double {
        val m = minutes / MINUTES_PER_DAY
        val phi = radians(latitude)
        val delta = radians(nodes.declination(m))
        val sine = sin(phi) * sin(delta) + cos(phi) * cos(delta) * cos(radians(hourAngle(m)))
        return degrees(asin(sine.coerceIn(-1.0, 1.0)))
    }

    /**
     * When the Sun's geometric altitude is [altitude] degrees in the morning (before [transit]) or the evening, in
     * minutes after local midnight; `null` when the Sun never reaches that altitude on its way.
     */
    fun altitudeEvent(
        altitude: Double,
        morning: Boolean,
    ): Double? {
        val phi = radians(latitude)
        val direction = if (morning) -1.0 else 1.0
        var m = transit / MINUTES_PER_DAY
        repeat(MAX_ITERATIONS) {
            val delta = radians(nodes.declination(m))
            val cosine = (sin(radians(altitude)) - sin(phi) * sin(delta)) / (cos(phi) * cos(delta))
            if (!cosine.isFinite() || cosine !in -1.0..1.0) return null
            val step =
                SolarEphemeris.normalizeHalfTurn(direction * degrees(acos(cosine)) - hourAngle(m)) / SIDEREAL_RATE
            m += step
            if (abs(step) < TOLERANCE_DAYS) return m * MINUTES_PER_DAY
        }
        return m * MINUTES_PER_DAY
    }

    private fun findTransit(): Double {
        var m = HALF
        repeat(MAX_ITERATIONS) {
            val step = -hourAngle(m) / SIDEREAL_RATE
            m += step
            if (abs(step) < TOLERANCE_DAYS) return m
        }
        return m
    }

    /** Local hour angle of the Sun [m] days after local midnight, in −180°‥180°. */
    private fun hourAngle(m: Double): Double =
        SolarEphemeris.normalizeHalfTurn(nodes.sidereal + SIDEREAL_RATE * m + longitude - nodes.rightAscension(m))

    /** Apparent Sun at local midnight, noon and the next midnight, with sidereal time at the first. */
    private class DayNodes(
        start: Double,
    ) {
        private val suns = listOf(0.0, HALF, 1.0).map { SolarEphemeris.sun(start + it) }
        private val ascensions = unwrapped(suns.map(SunCoordinates::rightAscension))
        private val declinations = suns.map(SunCoordinates::declination)
        val distanceAu: Double = suns[1].distanceAu
        val sidereal: Double = SolarEphemeris.siderealDegrees(start)

        fun rightAscension(m: Double): Double = interpolate(ascensions, m)

        fun declination(m: Double): Double = interpolate(declinations, m)

        /** Lagrange interpolation through nodes at 0, ½ and 1 day. */
        private fun interpolate(
            values: List<Double>,
            m: Double,
        ): Double = (values[0] * (m - HALF) * (m - 1) - values[1] * 2 * m * (m - 1) + values[2] * m * (m - HALF)) * 2

        /** Right ascensions made continuous across 360°. */
        private fun unwrapped(angles: List<Double>): List<Double> =
            angles.drop(1).fold(listOf(angles[0])) { acc, angle ->
                acc + (acc.last() + SolarEphemeris.normalizeHalfTurn(angle - acc.last()))
            }
    }

    companion object {
        private const val MINUTES_PER_DAY = 1_440.0
        private const val HALF = 0.5
        private const val HALF_TURN = 180.0

        /** Degrees the Earth turns per day against the stars (Meeus eq. 15.2). */
        private const val SIDEREAL_RATE = 360.985647
        private const val MAX_ITERATIONS = 12

        /** Refinement stops below about 0.1 ms. */
        private const val TOLERANCE_DAYS = 1e-9

        /** The civil day [day] at [place], whose clocks are [utcOffsetMinutes] ahead of UTC. */
        fun of(
            day: Jdn,
            place: Coordinates,
            utcOffsetMinutes: Int,
        ): SunDay =
            SunDay(DayNodes(SolarEphemeris.localMidnight(day.value, utcOffsetMinutes)), place.latitude, place.longitude)

        private fun radians(degrees: Double): Double = degrees * PI / HALF_TURN

        private fun degrees(radians: Double): Double = radians * HALF_TURN / PI
    }
}
