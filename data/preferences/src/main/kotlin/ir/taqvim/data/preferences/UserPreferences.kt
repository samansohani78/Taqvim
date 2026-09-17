/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.IslamicMonthTable
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

/** Where the optional official Islamic month starts came from (ADR-0037). */
enum class IslamicOverrideOrigin {
    /** No override: every Islamic date is computed. */
    NONE,

    /** The official Iranian months bundled with the app, switched on by the user. */
    OFFICIAL_BUNDLED,

    /** A file the user imported. */
    IMPORTED,
}

/**
 * The optional official Islamic month starts (ADR-0037): [origin] and the file [json]. [overrides] is the parsed file,
 * or `null` when there is none or it no longer parses — the app then shows computed dates.
 */
data class IslamicOverrideSetting(
    val origin: IslamicOverrideOrigin = IslamicOverrideOrigin.NONE,
    val json: String = "",
) {
    /** The parsed override, or `null`. */
    val overrides: IslamicMonthOverrides? by lazy {
        if (origin == IslamicOverrideOrigin.NONE) null else IslamicMonthOverrides.parse(json).getOrNull()
    }

    /** The month table to apply, or `null` for computed dates. */
    val table: IslamicMonthTable?
        get() = overrides?.table

    /** Whether an override was chosen but its text does not parse (shown as a notice; dates stay computed). */
    val isBroken: Boolean
        get() = origin != IslamicOverrideOrigin.NONE && overrides == null

    companion object {
        /** No override. */
        val NONE: IslamicOverrideSetting = IslamicOverrideSetting()
    }
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
    /** Settings of the settings screens (T-1500). */
    val app: AppSettings = AppSettings.DEFAULT,
    /** Whether the first-run onboarding (T-1501) was completed or skipped on this device. */
    val onboardingCompleted: Boolean = false,
    /** Optional official Islamic month starts (ADR-0037); by default every Islamic date is computed. */
    val islamicOverride: IslamicOverrideSetting = IslamicOverrideSetting.NONE,
) {
    /**
     * These preferences switched to [languageCode] (T-1501, ADR-0023). A language-derived value (calendars, numerals,
     * week start, weekend, prayer method, Asr convention, Islamic variant) that still equals the current language's
     * default counts as not chosen and takes the new language's default; a value the user changed is kept. Event
     * sources follow the new language until the user chose them ([AppSettings.eventSourcesChosen]). Unknown codes
     * switch to [FALLBACK_LANGUAGE].
     */
    fun withLanguage(languageCode: String): UserPreferences {
        val old = defaultsFor(this.languageCode)
        val new = defaultsFor(languageCode)
        return copy(
            languageCode = new.languageCode,
            calendars = keptOrDefault(calendars, old.calendars, new.calendars),
            numerals = keptOrDefault(numerals, old.numerals, new.numerals),
            weekStart = keptOrDefault(weekStart, old.weekStart, new.weekStart),
            weekend = keptOrDefault(weekend, old.weekend, new.weekend),
            prayerMethod = keptOrDefault(prayerMethod, old.prayerMethod, new.prayerMethod),
            asrJuristic = keptOrDefault(asrJuristic, old.asrJuristic, new.asrJuristic),
            islamicVariant = keptOrDefault(islamicVariant, old.islamicVariant, new.islamicVariant),
            app = app.withEventSourcesFor(new.languageCode),
        )
    }

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
                app = AppSettings.defaultsFor(spec.code),
            )

        /** The language table entry for [languageCode], or the [FALLBACK_LANGUAGE] entry. */
        internal fun specFor(languageCode: String): LanguageSpec =
            LanguageTable.forCode(languageCode)
                ?: requireNotNull(
                    LanguageTable.forCode(FALLBACK_LANGUAGE),
                ) { "language table lacks $FALLBACK_LANGUAGE" }
    }
}

/** [current] when the user changed it from [oldDefault], otherwise [newDefault]. */
private fun <T> keptOrDefault(
    current: T,
    oldDefault: T,
    newDefault: T,
): T = if (current == oldDefault) newDefault else current
