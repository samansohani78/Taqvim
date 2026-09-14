/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday

/** App theme choice. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    BLACK,
}

/** Typed user preferences (docs/PLAN.md §4.4), backed by the `UserPrefs` proto. */
data class UserPreferences(
    val languageCode: String,
    val calendars: List<CalendarSystem>,
    val numerals: NumeralSystem,
    val weekStart: Weekday,
    val weekend: Set<Weekday>,
    val prayerMethod: PrayerMethod,
    val asrJuristic: AsrJuristic,
    val islamicVariant: IslamicVariant,
    val themeMode: ThemeMode,
    val hijriOffsetDays: Int,
    val hijriOffsetSetAtEpochMillis: Long?,
    /** The chosen place (T-1502), or `null` until the user chooses one. */
    val place: ChosenPlace? = null,
    /** Athan settings (T-1101). */
    val athan: AthanPreferences = AthanPreferences.DEFAULT,
) {
    companion object {
        /** Language used when the device language is not one of the launch languages. */
        const val FALLBACK_LANGUAGE: String = "en"

        /** Language code whose users get the Iranian official lunar calendar by default. */
        private const val PERSIAN_IRAN = "fa"

        /**
         * First-run defaults for [languageCode] (T-600): calendars, numerals, week and prayer conventions come from the
         * language table (T-200); unknown languages use [FALLBACK_LANGUAGE].
         */
        fun defaultsFor(languageCode: String): UserPreferences = defaultsFor(specFor(languageCode))

        /** First-run defaults for [spec]. */
        fun defaultsFor(spec: LanguageSpec): UserPreferences =
            UserPreferences(
                languageCode = spec.code,
                calendars = spec.calendars,
                numerals = spec.numerals,
                weekStart = spec.weekStart,
                weekend = spec.weekend,
                prayerMethod = spec.prayerMethod,
                asrJuristic = spec.asrJuristic,
                islamicVariant =
                    if (spec.code ==
                        PERSIAN_IRAN
                    ) {
                        IslamicVariant.IRAN_OFFICIAL
                    } else {
                        IslamicVariant.UMM_AL_QURA
                    },
                themeMode = ThemeMode.SYSTEM,
                hijriOffsetDays = 0,
                hijriOffsetSetAtEpochMillis = null,
            )

        /** The language table entry for [languageCode], or the [FALLBACK_LANGUAGE] entry. */
        internal fun specFor(languageCode: String): LanguageSpec =
            LanguageTable.forCode(languageCode)
                ?: requireNotNull(
                    LanguageTable.forCode(FALLBACK_LANGUAGE),
                ) { "language table lacks $FALLBACK_LANGUAGE" }
    }
}
