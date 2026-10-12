/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.praytimes.HighLatitudeRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Synthetic settings and fakes of the settings home and subscriptions ports. */
internal object GeneralSettingsFixtures {
    fun settingsFor(language: LanguageSpec): GeneralSettings =
        GeneralSettings(
            languageCode = language.code,
            theme = ThemeChoice.SYSTEM,
            dynamicColor = true,
            highContrast = false,
            boldText = false,
            gradient = false,
            numerals = language.numerals,
            calendars = language.calendars.filter { it in SettingsCatalog.CALENDARS },
            weekStart = language.weekStart,
            weekend = language.weekend,
            islamicVariant = IslamicVariant.IRAN_OFFICIAL,
            showWeekNumbers = false,
            enabledEventSources = setOf(EventSource.IRAN_OFFICIAL, EventSource.INTERNATIONAL),
            prayerMethod = language.prayerMethod,
            asrJuristic = language.asrJuristic,
            highLatitudeRule = HighLatitudeRule.ANGLE_BASED,
            persistentNotification = false,
            rememberRecentSearches = true,
            hasRecentSearches = true,
        )
}

/** Remembers the settings after every update and emits them back. */
internal class FakeGeneralSettingsStore(
    language: LanguageSpec,
    settings: GeneralSettings = GeneralSettingsFixtures.settingsFor(language),
) : GeneralSettingsStore {
    private val state = MutableStateFlow(GeneralSettingsData(language, settings))
    var clears: Int = 0
        private set

    val current: GeneralSettings
        get() = state.value.settings

    override fun settings(): Flow<GeneralSettingsData> = state

    override suspend fun update(transform: (GeneralSettings) -> GeneralSettings) {
        state.update { it.copy(settings = transform(it.settings)) }
    }

    override suspend fun clearRecentSearches() {
        clears++
        state.update { it.copy(settings = it.settings.copy(hasRecentSearches = false)) }
    }
}
