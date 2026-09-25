/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.PrayerMethod

/** How Isha is found. */
public sealed interface IshaRule {
    /** When the Sun is [degreesBelowHorizon] below the horizon in the evening. */
    public data class Angle(
        public val degreesBelowHorizon: Double,
    ) : IshaRule

    /**
     * A fixed number of [minutes] after Maghrib, lengthened to [ramadanMinutes] during Ramadan where the authority
     * publishes a longer interval for that month (Umm al-Qura: 90 minutes, 120 in Ramadan — see docs/PROVENANCE.md
     * A-10). The Islamic month is resolved by the calculator, so no caller can forget to pass it.
     */
    public data class MinutesAfterMaghrib(
        public val minutes: Int,
        public val ramadanMinutes: Int = minutes,
    ) : IshaRule {
        /** The interval to use when the evening falls in [islamicMonth] (1‥12). */
        public fun minutesIn(islamicMonth: Int): Int = if (islamicMonth == RAMADAN) ramadanMinutes else minutes

        private companion object {
            const val RAMADAN = 9
        }
    }
}

/** How Maghrib is found. */
public sealed interface MaghribRule {
    /** At sunset. */
    public data object AtSunset : MaghribRule

    /** When the Sun is [degreesBelowHorizon] below the horizon after sunset. */
    public data class Angle(
        public val degreesBelowHorizon: Double,
    ) : MaghribRule
}

/** The interval whose middle is (religious) midnight. */
public enum class MidnightMode {
    SUNSET_TO_SUNRISE,
    SUNSET_TO_FAJR,
    MAGHRIB_TO_SUNRISE,
    MAGHRIB_TO_FAJR,
}

/**
 * What to do when Fajr or Isha cannot be computed from their angle, or fall too far into the night (A-10).
 * Portions are fractions of the night from sunset to the next sunrise.
 */
public enum class HighLatitudeRule {
    /** No adjustment: an unreachable angle leaves the time undefined. */
    NONE,

    /** Fajr no earlier than half the night before sunrise; Isha no later than half the night after sunset. */
    MIDDLE_OF_NIGHT,

    /** As [MIDDLE_OF_NIGHT] with a seventh of the night. */
    ONE_SEVENTH,

    /** As [MIDDLE_OF_NIGHT] with angle/60 of the night. */
    ANGLE_BASED,

    /**
     * The Institute of Geophysics rule for "white nights": where Fajr cannot be computed, Imsak is 12 hours after
     * Dhuhr and Fajr half an hour after Imsak (docs/sources, Calendar Center note on Fajr in white nights).
     */
    GEOPHYSICS_WHITE_NIGHTS,

    /**
     * Resolution 6 of the Islamic Fiqh Council's ninth session (Makkah, 1406 AH), as endorsed by the European Council
     * for Fatwa and Research: where the twilight never reaches the method's angle, Fajr and Isha follow the nearest
     * latitude where it does, on that day and meridian, and beyond latitude 66° the parallel at 45°. The time is
     * carried over as the same fraction of the night. Days with a computable twilight are unchanged.
     */
    NEAREST_LATITUDE,
}

/** How exact times become whole minutes. */
public enum class MinuteRounding {
    /** To the nearest minute; halves go up. */
    NEAREST,

    /** Down to the minute. */
    FLOOR,

    /** Up to the minute. */
    CEILING,
}

/** Whole minutes an authority adds to computed times (its temkin or ihtiyat); negative values move a time earlier. */
public data class TimeAdjustments(
    public val fajr: Int = 0,
    public val sunrise: Int = 0,
    public val dhuhr: Int = 0,
    public val asr: Int = 0,
    public val maghrib: Int = 0,
    public val isha: Int = 0,
)

/** Angles, rules, published minute [adjustments] and [rounding] of a calculation method. */
public data class MethodParameters(
    public val fajrAngle: Double,
    public val isha: IshaRule,
    public val maghrib: MaghribRule,
    public val midnight: MidnightMode,
    public val adjustments: TimeAdjustments = TimeAdjustments(),
    public val rounding: MinuteRounding = MinuteRounding.NEAREST,
)

/**
 * User-facing prayer settings: [method], [asr] convention, [highLatitude] rule and [horizon] for sunrise and sunset;
 * [midnight] overrides the method's midnight interval when set.
 */
public data class PrayerSettings(
    public val method: PrayerMethod = PrayerMethod.TEHRAN,
    public val asr: AsrJuristic = AsrJuristic.STANDARD,
    public val highLatitude: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    public val midnight: MidnightMode? = null,
    public val horizon: HorizonSettings = HorizonSettings(),
)

/**
 * Standard parameters of each method as listed in docs/PLAN.md §6 A-10. Diyanet (Türkiye): Fajr 18° and Isha 17°
 * (press statement of 17 July 2013, https://www.diyanet.gov.tr/tr-TR/Content/PrintDetail/2921) with its temkin —
 * sunrise 7 minutes earlier, Dhuhr 5 and Asr 4 minutes later, Maghrib 7 minutes after sunset, none for Fajr and Isha
 * (https://vakithesaplama.diyanet.gov.tr/temkin.php). Diyanet publishes no Asr school, high-latitude rule or rounding,
 * so those follow the user's settings and the nearest minute.
 */
public fun PrayerMethod.parameters(): MethodParameters = METHOD_PARAMETERS.getValue(this)

/** Every method's definition; a test checks that each [PrayerMethod] has one. */
@Suppress("MagicNumber") // Angles and minutes are the published method definitions.
private val METHOD_PARAMETERS: Map<PrayerMethod, MethodParameters> =
    mapOf(
        PrayerMethod.MWL to sunni(18.0, IshaRule.Angle(17.0)),
        PrayerMethod.ISNA to sunni(15.0, IshaRule.Angle(15.0)),
        PrayerMethod.EGYPT to sunni(19.5, IshaRule.Angle(17.5)),
        PrayerMethod.MAKKAH to sunni(18.5, IshaRule.MinutesAfterMaghrib(90, ramadanMinutes = 120)),
        PrayerMethod.KARACHI to sunni(18.0, IshaRule.Angle(18.0)),
        PrayerMethod.TEHRAN to shia(17.7, 14.0, 4.5),
        PrayerMethod.JAFARI to shia(16.0, 14.0, 4.0),
        PrayerMethod.SINGAPORE to sunni(20.0, IshaRule.Angle(18.0)),
        PrayerMethod.FRANCE to sunni(12.0, IshaRule.Angle(12.0)),
        PrayerMethod.RUSSIA to sunni(16.0, IshaRule.Angle(15.0)),
        PrayerMethod.DIYANET to
            sunni(18.0, IshaRule.Angle(17.0))
                .copy(adjustments = TimeAdjustments(sunrise = -7, dhuhr = 5, asr = 4, maghrib = 7)),
    )

private fun sunni(
    fajrAngle: Double,
    isha: IshaRule,
) = MethodParameters(fajrAngle, isha, MaghribRule.AtSunset, MidnightMode.SUNSET_TO_SUNRISE)

private fun shia(
    fajrAngle: Double,
    ishaAngle: Double,
    maghribAngle: Double,
) = MethodParameters(fajrAngle, IshaRule.Angle(ishaAngle), MaghribRule.Angle(maghribAngle), MidnightMode.SUNSET_TO_FAJR)
