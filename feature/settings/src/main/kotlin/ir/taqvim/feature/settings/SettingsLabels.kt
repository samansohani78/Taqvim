/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.HighLatitudeRule

/** String resources naming the values offered by the settings choices, in the order they are offered. */
internal object SettingsLabels {
    val themes: Map<ThemeChoice, Int> =
        mapOf(
            ThemeChoice.SYSTEM to R.string.settings_theme_system,
            ThemeChoice.LIGHT to R.string.settings_theme_light,
            ThemeChoice.DARK to R.string.settings_theme_dark,
            ThemeChoice.BLACK to R.string.settings_theme_black,
        )

    val numerals: Map<NumeralSystem, Int> =
        mapOf(
            NumeralSystem.LATIN to R.string.settings_numerals_latin,
            NumeralSystem.PERSIAN to R.string.settings_numerals_persian,
            NumeralSystem.EASTERN_ARABIC to R.string.settings_numerals_eastern_arabic,
            NumeralSystem.DEVANAGARI to R.string.settings_numerals_devanagari,
            NumeralSystem.TAMIL to R.string.settings_numerals_tamil,
        )

    val calendars: Map<CalendarSystem, Int> =
        mapOf(
            CalendarSystem.PERSIAN to R.string.settings_calendar_persian,
            CalendarSystem.ISLAMIC to R.string.settings_calendar_islamic,
            CalendarSystem.GREGORIAN to R.string.settings_calendar_gregorian,
            CalendarSystem.NEPALI to R.string.settings_calendar_nepali,
        )

    val weekdays: Map<Weekday, Int> =
        mapOf(
            Weekday.SATURDAY to R.string.settings_weekday_saturday,
            Weekday.SUNDAY to R.string.settings_weekday_sunday,
            Weekday.MONDAY to R.string.settings_weekday_monday,
            Weekday.TUESDAY to R.string.settings_weekday_tuesday,
            Weekday.WEDNESDAY to R.string.settings_weekday_wednesday,
            Weekday.THURSDAY to R.string.settings_weekday_thursday,
            Weekday.FRIDAY to R.string.settings_weekday_friday,
        )

    val islamicVariants: Map<IslamicVariant, Int> =
        mapOf(
            IslamicVariant.IRAN_OFFICIAL to R.string.settings_variant_iran_official,
            IslamicVariant.UMM_AL_QURA to R.string.settings_variant_umm_al_qura,
            IslamicVariant.TABULAR_16 to R.string.settings_variant_tabular_16,
            IslamicVariant.TABULAR_15 to R.string.settings_variant_tabular_15,
            IslamicVariant.CALCULATED_OBSERVATIONAL to R.string.settings_variant_observational,
        )

    val eventSources: Map<EventSource, Int> =
        mapOf(
            EventSource.IRAN_OFFICIAL to R.string.settings_source_iran,
            EventSource.AFGHANISTAN_OFFICIAL to R.string.settings_source_afghanistan,
            EventSource.NEPAL_OFFICIAL to R.string.settings_source_nepal,
            EventSource.INTERNATIONAL to R.string.settings_source_international,
            EventSource.ANCIENT_IRAN to R.string.settings_source_ancient_iran,
        )

    val prayerMethods: Map<PrayerMethod, Int> =
        mapOf(
            PrayerMethod.TEHRAN to R.string.settings_method_tehran,
            PrayerMethod.JAFARI to R.string.settings_method_jafari,
            PrayerMethod.MWL to R.string.settings_method_mwl,
            PrayerMethod.ISNA to R.string.settings_method_isna,
            PrayerMethod.EGYPT to R.string.settings_method_egypt,
            PrayerMethod.MAKKAH to R.string.settings_method_makkah,
            PrayerMethod.KARACHI to R.string.settings_method_karachi,
            PrayerMethod.SINGAPORE to R.string.settings_method_singapore,
            PrayerMethod.FRANCE to R.string.settings_method_france,
            PrayerMethod.RUSSIA to R.string.settings_method_russia,
            PrayerMethod.DIYANET to R.string.settings_method_diyanet,
        )

    val asrJuristics: Map<AsrJuristic, Int> =
        mapOf(
            AsrJuristic.STANDARD to R.string.settings_asr_standard,
            AsrJuristic.HANAFI to R.string.settings_asr_hanafi,
        )

    val highLatitudeRules: Map<HighLatitudeRule, Int> =
        mapOf(
            HighLatitudeRule.ANGLE_BASED to R.string.settings_high_latitude_angle,
            HighLatitudeRule.MIDDLE_OF_NIGHT to R.string.settings_high_latitude_middle,
            HighLatitudeRule.ONE_SEVENTH to R.string.settings_high_latitude_seventh,
            HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS to R.string.settings_high_latitude_white_nights,
            HighLatitudeRule.NEAREST_LATITUDE to R.string.settings_high_latitude_nearest,
            HighLatitudeRule.NONE to R.string.settings_high_latitude_none,
        )
}
