/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import kotlin.math.abs
import kotlin.math.sign

/**
 * A night under [sky]: from the sunset at [start] to the sunrise at [end], both in minutes after the sky's local
 * midnight (a previous evening's sunset is negative, a next morning's sunrise beyond 1440), with [horizon] the sunrise
 * altitude used (A-10).
 */
internal class Night(
    val sky: SunDay,
    val start: Double,
    val end: Double,
    val horizon: Double,
) {
    val length: Double = end - start
}

/** Fajr and Isha under the high-latitude rules (A-10, ADR-0029). */
internal object HighLatitude {
    private const val MINUTES_PER_DAY = 1_440.0
    private const val MINUTES_PER_HOUR = 60.0
    private const val RIGHT_ANGLE = 90.0
    private const val HALF = 0.5
    private const val SEVENTH = 1.0 / 7.0
    private const val HALF_DAY_MINUTES = 720.0
    private const val WHITE_NIGHT_FAJR_AFTER_DHUHR = 12 * 60.0 + 30.0

    /** Beyond this latitude Resolution 6 takes the parallel at [FAR_REFERENCE_LATITUDE]. */
    private const val POLAR_LATITUDE = 66.0
    private const val FAR_REFERENCE_LATITUDE = 45.0

    /** Fajr for [rule] and the method's [angle]; [night] ends at this morning's sunrise, [dhuhr] is this day's. */
    fun fajr(
        rule: HighLatitudeRule,
        angle: Double,
        night: Night,
        dhuhr: Double,
    ): Double? {
        val computed = night.sky.altitudeEvent(-angle, morning = true)
        val whiteNightFajr = dhuhr + WHITE_NIGHT_FAJR_AFTER_DHUHR - MINUTES_PER_DAY
        return when (rule) {
            HighLatitudeRule.NONE -> {
                computed
            }

            HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS -> {
                computed ?: whiteNightFajr
            }

            HighLatitudeRule.NEAREST_LATITUDE -> {
                computed ?: nearestPortion(angle, night, morning = true)?.let { night.end - it * night.length }
            }

            else -> {
                val limit = portion(rule, angle) * night.length
                if (computed == null || night.end - computed > limit) night.end - limit else computed
            }
        }
    }

    /** Isha by angle for [rule] and the method's [angle]; [night] starts at this evening's sunset. */
    fun isha(
        rule: HighLatitudeRule,
        angle: Double,
        night: Night,
    ): Double? {
        val computed = night.sky.altitudeEvent(-angle, morning = false)
        return when (rule) {
            HighLatitudeRule.NONE, HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS -> {
                computed
            }

            HighLatitudeRule.NEAREST_LATITUDE -> {
                computed ?: nearestPortion(angle, night, morning = false)?.let { night.start + it * night.length }
            }

            else -> {
                val limit = portion(rule, angle) * night.length
                if (computed == null || computed - night.start > limit) night.start + limit else computed
            }
        }
    }

    private fun portion(
        rule: HighLatitudeRule,
        angle: Double,
    ): Double =
        when (rule) {
            HighLatitudeRule.MIDDLE_OF_NIGHT -> HALF
            HighLatitudeRule.ONE_SEVENTH -> SEVENTH
            else -> angle / MINUTES_PER_HOUR
        }

    /**
     * The fraction of the night between the twilight at [angle] and sunrise ([morning]) or between sunset and the
     * twilight, at the nearest latitude of the same meridian where the Sun gets that deep on this day. At that latitude
     * the Sun reaches the angle exactly at lower culmination, taken as half a day from transit (within about two
     * minutes), which keeps the time smooth from day to day; beyond 66° the 45° parallel's own twilight is used.
     */
    private fun nearestPortion(
        angle: Double,
        night: Night,
        morning: Boolean,
    ): Double? {
        val sky = night.sky
        val reference = sky.at(referenceLatitude(sky.latitude, sky.declinationAt(sky.transit), angle))
        val sunrise = reference.altitudeEvent(night.horizon, morning = true)
        val sunset = reference.altitudeEvent(night.horizon, morning = false)
        val twilight =
            if (abs(sky.latitude) > POLAR_LATITUDE) {
                reference.altitudeEvent(-angle, morning)
            } else {
                reference.transit + if (morning) -HALF_DAY_MINUTES else HALF_DAY_MINUTES
            }
        if (sunrise == null || sunset == null || twilight == null) return null
        val gap = if (morning) sunrise - twilight else twilight - sunset
        return (gap / (sunrise + MINUTES_PER_DAY - sunset)).takeIf { it > 0 && it < 1 }
    }

    /**
     * The latitude closest to [latitude] where the Sun, at [declination], sinks [angle] degrees below the horizon at
     * lower culmination (whose altitude is |φ + δ| − 90°); the 45° parallel beyond 66°.
     */
    internal fun referenceLatitude(
        latitude: Double,
        declination: Double,
        angle: Double,
    ): Double {
        if (abs(latitude) > POLAR_LATITUDE) return sign(latitude) * FAR_REFERENCE_LATITUDE
        val side = if (latitude + declination >= 0) 1.0 else -1.0
        return (side * (RIGHT_ANGLE - angle) - declination).coerceIn(-RIGHT_ANGLE, RIGHT_ANGLE)
    }
}
