/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.math.tan

/** Prayer times of one day in local time; `null` where a time is undefined under the chosen rules. */
public data class PrayerTimes(
    public val fajr: MinuteOfDay?,
    public val sunrise: MinuteOfDay,
    public val dhuhr: MinuteOfDay,
    public val asr: MinuteOfDay,
    public val sunset: MinuteOfDay,
    public val maghrib: MinuteOfDay?,
    public val isha: MinuteOfDay?,
    public val midnight: MinuteOfDay?,
)

/** Outcome of a prayer-time calculation: never NaN — either times, or why there are none. */
public sealed interface PrayerTimesResult {
    /** Times for the day. */
    public data class Available(
        public val times: PrayerTimes,
    ) : PrayerTimesResult

    /** The Sun neither rises nor sets that day. */
    public data class Unavailable(
        public val reason: Reason,
    ) : PrayerTimesResult

    /** Why no times exist. */
    public enum class Reason {
        POLAR_DAY,
        POLAR_NIGHT,
    }
}

/**
 * Prayer times (A-10) on the NOAA solar equations (A-09). Each event is refined by re-evaluating the Sun at the
 * event's own time; results are rounded to the nearest minute.
 */
public object PrayerTimesCalculator {
    private const val ITERATIONS = 3
    private const val MINUTES_PER_DAY = 1_440.0
    private const val MINUTES_PER_HOUR = 60.0
    private const val NOON = 720.0
    private const val QUARTER_DAY = 360.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val RIGHT_ANGLE = 90.0
    private const val HALF = 0.5
    private const val SEVENTH = 1.0 / 7.0
    private const val WHITE_NIGHT_IMSAK_AFTER_DHUHR = 12 * 60.0
    private const val WHITE_NIGHT_FAJR_AFTER_IMSAK = 30.0

    /** Times for the civil day [day] at [place], whose clocks are [utcOffsetMinutes] ahead of UTC. */
    public fun calculate(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings = PrayerSettings(),
    ): PrayerTimesResult {
        val sky = DaySky(day, place, utcOffsetMinutes)
        val sunrise = sky.event(NoaaSolarCalculator.SUNRISE_ZENITH_DEGREES, morning = true)
        val sunset = sky.event(NoaaSolarCalculator.SUNRISE_ZENITH_DEGREES, morning = false)
        if (sunrise == null || sunset == null) {
            val reason =
                if (sky.sunUpAtNoon()) PrayerTimesResult.Reason.POLAR_DAY else PrayerTimesResult.Reason.POLAR_NIGHT
            return PrayerTimesResult.Unavailable(reason)
        }
        val parameters = settings.method.parameters()
        val dhuhr = sky.noon()
        val maghrib = maghrib(sky, parameters.maghrib, sunset)
        val night = sunrise + MINUTES_PER_DAY - sunset
        val computedFajr = sky.event(RIGHT_ANGLE + parameters.fajrAngle, morning = true)
        val fajr = adjustFajr(computedFajr, sunrise, dhuhr, night, parameters, settings)
        val isha = isha(sky, parameters, settings, sunset, maghrib, night)
        val midnight = midnight(settings.midnight ?: parameters.midnight, sunrise, sunset, fajr, maghrib)
        val asr = sky.asr(settings.asr.shadowFactor) ?: dhuhr
        return PrayerTimesResult.Available(
            PrayerTimes(
                fajr = fajr?.let(::minute),
                sunrise = minute(sunrise),
                dhuhr = minute(dhuhr),
                asr = minute(asr),
                sunset = minute(sunset),
                maghrib = maghrib?.let(::minute),
                isha = isha?.let(::minute),
                midnight = midnight?.let(::minute),
            ),
        )
    }

    private fun maghrib(
        sky: DaySky,
        rule: MaghribRule,
        sunset: Double,
    ): Double? =
        when (rule) {
            MaghribRule.AtSunset -> sunset
            is MaghribRule.Angle -> sky.event(RIGHT_ANGLE + rule.degreesBelowHorizon, morning = false)
        }

    private fun adjustFajr(
        computed: Double?,
        sunrise: Double,
        dhuhr: Double,
        night: Double,
        parameters: MethodParameters,
        settings: PrayerSettings,
    ): Double? {
        val rule = settings.highLatitude
        if (rule == HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS) {
            return computed ?: (dhuhr + WHITE_NIGHT_IMSAK_AFTER_DHUHR + WHITE_NIGHT_FAJR_AFTER_IMSAK - MINUTES_PER_DAY)
        }
        val limit = portion(rule, parameters.fajrAngle)?.times(night) ?: return computed
        return if (computed == null || sunrise - computed > limit) sunrise - limit else computed
    }

    private fun isha(
        sky: DaySky,
        parameters: MethodParameters,
        settings: PrayerSettings,
        sunset: Double,
        maghrib: Double?,
        night: Double,
    ): Double? =
        when (val rule = parameters.isha) {
            is IshaRule.MinutesAfterMaghrib -> {
                (maghrib ?: sunset) + rule.minutes
            }

            is IshaRule.Angle -> {
                val computed = sky.event(RIGHT_ANGLE + rule.degreesBelowHorizon, morning = false)
                val limit = portion(settings.highLatitude, rule.degreesBelowHorizon)?.times(night)
                when {
                    limit == null -> computed
                    computed == null || computed - sunset > limit -> sunset + limit
                    else -> computed
                }
            }
        }

    private fun midnight(
        mode: MidnightMode,
        sunrise: Double,
        sunset: Double,
        fajr: Double?,
        maghrib: Double?,
    ): Double? {
        val fromSunset = mode == MidnightMode.SUNSET_TO_SUNRISE || mode == MidnightMode.SUNSET_TO_FAJR
        val toSunrise = mode == MidnightMode.SUNSET_TO_SUNRISE || mode == MidnightMode.MAGHRIB_TO_SUNRISE
        val start = if (fromSunset) sunset else maghrib
        val end = if (toSunrise) sunrise else fajr
        return if (start == null || end == null) null else (start + end + MINUTES_PER_DAY) / 2
    }

    private fun portion(
        rule: HighLatitudeRule,
        angle: Double,
    ): Double? =
        when (rule) {
            HighLatitudeRule.NONE, HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS -> null
            HighLatitudeRule.MIDDLE_OF_NIGHT -> HALF
            HighLatitudeRule.ONE_SEVENTH -> SEVENTH
            HighLatitudeRule.ANGLE_BASED -> angle / MINUTES_PER_HOUR
        }

    private fun minute(localMinutes: Double): MinuteOfDay =
        MinuteOfDay(Math.floorMod(localMinutes.roundToInt(), MINUTES_PER_DAY.toInt()))

    /** The Sun over one civil day at one place, in local minutes after midnight. */
    private class DaySky(
        day: Jdn,
        private val place: Coordinates,
        private val utcOffsetMinutes: Int,
    ) {
        private val julianDayAtUtcMidnight = day.value - HALF

        private fun sunAt(localMinutes: Double): SolarParameters =
            NoaaSolarCalculator.parameters(julianDayAtUtcMidnight + (localMinutes - utcOffsetMinutes) / MINUTES_PER_DAY)

        private fun noonAt(sun: SolarParameters): Double =
            NOON - MINUTES_PER_DEGREE * place.longitude - sun.equationOfTimeMinutes + utcOffsetMinutes

        fun noon(): Double = (1..ITERATIONS).fold(NOON) { time, _ -> noonAt(sunAt(time)) }

        fun sunUpAtNoon(): Boolean = abs(place.latitude - sunAt(noon()).declinationDegrees) < RIGHT_ANGLE

        /** Time when the Sun is at [zenithDegrees] in the morning or the evening; `null` if it never is that day. */
        fun event(
            zenithDegrees: Double,
            morning: Boolean,
        ): Double? {
            val sign = if (morning) -1 else 1
            var time: Double? = NOON + sign * QUARTER_DAY
            repeat(ITERATIONS) {
                time =
                    time?.let { current ->
                        val sun = sunAt(current)
                        NoaaSolarCalculator
                            .hourAngleDegrees(place.latitude, sun.declinationDegrees, zenithDegrees)
                            ?.let { noonAt(sun) + sign * MINUTES_PER_DEGREE * it }
                    }
            }
            return time
        }

        /** Asr: the shadow equals [shadowFactor] object heights plus the noon shadow. */
        fun asr(shadowFactor: Int): Double? {
            val declination = sunAt(noon()).declinationDegrees
            val noonShadow = tan(Math.toRadians(abs(place.latitude - declination)))
            val elevation = Math.toDegrees(atan(1 / (shadowFactor + noonShadow)))
            return event(RIGHT_ANGLE - elevation, morning = false)
        }
    }
}
