/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.tan

/** Prayer times of one day in local time; `null` where a time is undefined under the chosen rules. */
public data class PrayerTimes(
    public val fajr: MinuteOfDay?,
    public val sunrise: MinuteOfDay,
    public val dhuhr: MinuteOfDay,
    public val asr: MinuteOfDay?,
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
 * Exact prayer times of one day in minutes after local midnight, before the method's minute adjustments and rounding;
 * values may leave 0‥1440 when a time falls on a neighbouring civil day.
 */
internal data class ExactPrayerTimes(
    val fajr: Double?,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double?,
    val sunset: Double,
    val maghrib: Double?,
    val isha: Double?,
    val midnight: Double?,
)

/** Exact times, or why the day has none. */
internal sealed interface ExactResult {
    data class Times(
        val times: ExactPrayerTimes,
    ) : ExactResult

    data class Missing(
        val reason: PrayerTimesResult.Reason,
    ) : ExactResult
}

/**
 * Prayer times (A-10) on the apparent Sun of a high-precision ephemeris (ADR-0029, [SolarEphemeris]): sunrise and
 * sunset against [HorizonSettings], twilights by depression below the geometric horizon, Asr by the shadow rule on the
 * declination at transit, the high-latitude rules of [HighLatitude], then the method's minute adjustments and
 * rounding.
 */
public object PrayerTimesCalculator {
    private const val MINUTES_PER_DAY = 1_440.0
    private const val HALF_TURN = 180.0
    private const val HALF = 0.5
    private const val NOISE = 1e-9
    private const val RIGHT_ANGLE = 90.0

    /** Times for the civil day [day] at [place], whose clocks are [utcOffsetMinutes] ahead of UTC. */
    public fun calculate(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings = PrayerSettings(),
    ): PrayerTimesResult {
        val parameters = settings.method.parameters()
        return when (val exact = exact(day, place, utcOffsetMinutes, settings, parameters)) {
            is ExactResult.Missing -> PrayerTimesResult.Unavailable(exact.reason)
            is ExactResult.Times -> PrayerTimesResult.Available(rounded(exact.times, parameters))
        }
    }

    /**
     * Exact times under explicit [parameters] (tests and method comparisons). Nights run from a sunset to the next
     * sunrise: Fajr's high-latitude limit uses the night ending this morning, Isha's the night starting this evening,
     * and midnight ends at the next morning's sunrise or Fajr. Where a neighbouring day has no sunrise or sunset (the
     * edge of a polar season), this day's own event stands in for it.
     */
    internal fun exact(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings,
        parameters: MethodParameters,
    ): ExactResult {
        val sky = SunDay.of(day, place, utcOffsetMinutes)
        val horizon = settings.horizon.sunriseAltitude(sky.distanceAu, place.elevationMeters)
        val sunrise = sky.altitudeEvent(horizon, morning = true)
        val sunset = sky.altitudeEvent(horizon, morning = false)
        if (sunrise == null || sunset == null) {
            val polarDay = sky.altitudeAt(sky.transit) > horizon
            return ExactResult.Missing(
                if (polarDay) PrayerTimesResult.Reason.POLAR_DAY else PrayerTimesResult.Reason.POLAR_NIGHT,
            )
        }
        val rule = settings.highLatitude
        val previousSunset =
            SunDay.of(day + -1L, place, utcOffsetMinutes).altitudeEvent(horizon, morning = false) ?: sunset
        val next = SunDay.of(day + 1L, place, utcOffsetMinutes)
        val nextSunrise = next.altitudeEvent(horizon, morning = true) ?: sunrise
        val dhuhr = sky.transit
        val maghrib = maghrib(sky, parameters.maghrib, sunset)
        val morningNight = Night(sky, previousSunset - MINUTES_PER_DAY, sunrise, horizon)
        val fajr = HighLatitude.fajr(rule, parameters.fajrAngle, morningNight, dhuhr)
        val eveningNight = Night(sky, sunset, nextSunrise + MINUTES_PER_DAY, horizon)
        val isha = isha(parameters.isha, rule, eveningNight, maghrib ?: sunset, day)
        val nextNight = Night(next, sunset - MINUTES_PER_DAY, nextSunrise, horizon)
        val nextFajr = HighLatitude.fajr(rule, parameters.fajrAngle, nextNight, next.transit)
        val mode = settings.midnight ?: parameters.midnight
        val midnight = midnight(mode, sunset, maghrib, nextSunrise, nextFajr)
        val asr = asr(sky, settings.asr.shadowFactor, dhuhr, sunset)
        return ExactResult.Times(ExactPrayerTimes(fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, midnight))
    }

    /**
     * Isha after [maghrib] (the observed Maghrib, or sunset when the method's Maghrib is at sunset or unreached).
     *
     * A high-latitude rule estimates an unreached Isha angle as a portion of the night measured from sunset. When the
     * method's Maghrib is itself a twilight (the Tehran and Jafari methods, 4–4.5° below the horizon), a shallow sunset
     * path near the Arctic Circle can put that Maghrib later than the estimate, so the estimate never precedes it: Isha
     * is then at Maghrib (R05). Maghrib stays the observed time, since the Sun does reach its angle. A computed Isha is
     * unaffected — a deeper depression is always reached later the same evening.
     */
    private fun isha(
        rule: IshaRule,
        highLatitude: HighLatitudeRule,
        night: Night,
        maghrib: Double,
        day: Jdn,
    ): Double? =
        when (rule) {
            is IshaRule.MinutesAfterMaghrib -> {
                // Umm al-Qura lengthens the interval in Ramadan, so the month decides it. The evening of a civil day
                // belongs to the Islamic day that began at the preceding sunset, which is the month of `day` itself:
                // the Islamic date rolls over at sunset, and this evening is that date's own night.
                maghrib + rule.minutesIn(UmmAlQuraCalendar.fromJdn(day).month)
            }

            is IshaRule.Angle -> {
                HighLatitude
                    .isha(
                        highLatitude,
                        rule.degreesBelowHorizon,
                        night,
                    )?.coerceAtLeast(maghrib)
            }
        }

    private fun maghrib(
        sky: SunDay,
        rule: MaghribRule,
        sunset: Double,
    ): Double? =
        when (rule) {
            MaghribRule.AtSunset -> sunset
            is MaghribRule.Angle -> sky.altitudeEvent(-rule.degreesBelowHorizon, morning = false)
        }

    /** The middle of the interval [mode] names, from this evening to the next morning ([nextSunrise], [nextFajr]). */
    private fun midnight(
        mode: MidnightMode,
        sunset: Double,
        maghrib: Double?,
        nextSunrise: Double,
        nextFajr: Double?,
    ): Double? {
        val fromSunset = mode == MidnightMode.SUNSET_TO_SUNRISE || mode == MidnightMode.SUNSET_TO_FAJR
        val toSunrise = mode == MidnightMode.SUNSET_TO_SUNRISE || mode == MidnightMode.MAGHRIB_TO_SUNRISE
        val start = if (fromSunset) sunset else maghrib
        val end = if (toSunrise) nextSunrise else nextFajr
        return if (start == null || end == null) null else (start + end + MINUTES_PER_DAY) / 2
    }

    /**
     * Asr: the afternoon moment when an object's shadow is [shadowFactor] heights longer than at transit, i.e. the
     * Sun's altitude is atan(1 / (factor + tan|φ − δ|)) with δ the declination at transit.
     *
     * `null` when that moment does not exist between [dhuhr] and [sunset] (R05). When the noon zenith distance
     * |φ − δ| reaches 90° the Sun at transit is not above the true horizon — it is seen only through refraction — so
     * there is no noon shadow to lengthen, and the formula's tangent turns negative and asks for an altitude the Sun
     * passes after sunset. The bounds are compared as continuous minutes after this day's midnight, so a sunset that
     * falls after the next midnight (values beyond 1440) is still a valid upper bound.
     */
    private fun asr(
        sky: SunDay,
        shadowFactor: Int,
        dhuhr: Double,
        sunset: Double,
    ): Double? {
        val noonZenith = abs(sky.latitude - sky.declinationAt(sky.transit))
        if (noonZenith >= RIGHT_ANGLE) return null
        val noonShadow = tan(noonZenith * PI / HALF_TURN)
        val altitude = atan(1 / (shadowFactor + noonShadow)) * HALF_TURN / PI
        return sky.altitudeEvent(altitude, morning = false)?.takeIf { it in dhuhr..sunset }
    }

    private fun rounded(
        exact: ExactPrayerTimes,
        parameters: MethodParameters,
    ): PrayerTimes {
        val adjust = parameters.adjustments
        val rounding = parameters.rounding

        fun minute(
            value: Double,
            minutes: Int,
        ): MinuteOfDay = minuteOf(value + minutes, rounding)
        return PrayerTimes(
            fajr = exact.fajr?.let { minute(it, adjust.fajr) },
            sunrise = minute(exact.sunrise, adjust.sunrise),
            dhuhr = minute(exact.dhuhr, adjust.dhuhr),
            asr = exact.asr?.let { minute(it, adjust.asr) },
            sunset = minute(exact.sunset, 0),
            maghrib = exact.maghrib?.let { minute(it, adjust.maghrib) },
            isha = exact.isha?.let { minute(it, adjust.isha) },
            midnight = exact.midnight?.let { minute(it, 0) },
        )
    }

    /** [localMinutes] as a minute of the day under [rounding]; a whisker of floating-point noise is ignored. */
    internal fun minuteOf(
        localMinutes: Double,
        rounding: MinuteRounding,
    ): MinuteOfDay {
        val whole =
            when (rounding) {
                MinuteRounding.NEAREST -> floor(localMinutes + HALF)
                MinuteRounding.FLOOR -> floor(localMinutes + NOISE)
                MinuteRounding.CEILING -> ceil(localMinutes - NOISE)
            }
        return MinuteOfDay(Math.floorMod(whole.toLong(), MINUTES_PER_DAY.toLong()).toInt())
    }
}
