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

    /** A fixed number of [minutes] after Maghrib. */
    public data class MinutesAfterMaghrib(
        public val minutes: Int,
    ) : IshaRule
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
}

/** Angles and rules of a calculation method. */
public data class MethodParameters(
    public val fajrAngle: Double,
    public val isha: IshaRule,
    public val maghrib: MaghribRule,
    public val midnight: MidnightMode,
)

/**
 * User-facing prayer settings: [method], [asr] convention and [highLatitude] rule; [midnight] overrides the method's
 * midnight interval when set.
 */
public data class PrayerSettings(
    public val method: PrayerMethod = PrayerMethod.TEHRAN,
    public val asr: AsrJuristic = AsrJuristic.STANDARD,
    public val highLatitude: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    public val midnight: MidnightMode? = null,
)

/** Standard parameters of each method as listed in docs/PLAN.md §6 A-10. */
@Suppress("MagicNumber") // Angles and minutes are the published method definitions.
public fun PrayerMethod.parameters(): MethodParameters =
    when (this) {
        PrayerMethod.MWL -> sunni(18.0, IshaRule.Angle(17.0))
        PrayerMethod.ISNA -> sunni(15.0, IshaRule.Angle(15.0))
        PrayerMethod.EGYPT -> sunni(19.5, IshaRule.Angle(17.5))
        PrayerMethod.MAKKAH -> sunni(18.5, IshaRule.MinutesAfterMaghrib(90))
        PrayerMethod.KARACHI -> sunni(18.0, IshaRule.Angle(18.0))
        PrayerMethod.TEHRAN -> shia(17.7, 14.0, 4.5)
        PrayerMethod.JAFARI -> shia(16.0, 14.0, 4.0)
        PrayerMethod.SINGAPORE -> sunni(20.0, IshaRule.Angle(18.0))
        PrayerMethod.FRANCE -> sunni(12.0, IshaRule.Angle(12.0))
        PrayerMethod.RUSSIA -> sunni(16.0, IshaRule.Angle(15.0))
    }

private fun sunni(
    fajrAngle: Double,
    isha: IshaRule,
) = MethodParameters(fajrAngle, isha, MaghribRule.AtSunset, MidnightMode.SUNSET_TO_SUNRISE)

private fun shia(
    fajrAngle: Double,
    ishaAngle: Double,
    maghribAngle: Double,
) = MethodParameters(fajrAngle, IshaRule.Angle(ishaAngle), MaghribRule.Angle(maghribAngle), MidnightMode.SUNSET_TO_FAJR)
