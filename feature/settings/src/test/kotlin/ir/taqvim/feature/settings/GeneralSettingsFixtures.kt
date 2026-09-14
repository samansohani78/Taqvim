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
            subscriptionsNetworkAllowed = true,
            persistentNotification = false,
            rememberRecentSearches = true,
            hasRecentSearches = true,
        )

    val calendarFeed =
        SubscriptionItem(1, "Holidays feed", "https://example.org/holidays.ics", true, 1_789_000_000_000)
    val teamFeed = SubscriptionItem(2, "Team", "webcal://example.org/team.ics", false, null)
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

/** Subscriptions kept in memory; [outcome] answers every add and refresh. */
internal class FakeSubscriptionsStore(
    items: List<SubscriptionItem> = listOf(GeneralSettingsFixtures.calendarFeed, GeneralSettingsFixtures.teamFeed),
    var outcome: SubscriptionOutcome = SubscriptionOutcome.DONE,
) : SubscriptionsStore {
    private val state = MutableStateFlow(items)
    val added = mutableListOf<String>()
    val refreshed = mutableListOf<Long>()

    val current: List<SubscriptionItem>
        get() = state.value

    override fun subscriptions(): Flow<List<SubscriptionItem>> = state

    override suspend fun add(url: String): SubscriptionOutcome {
        added += url
        if (outcome == SubscriptionOutcome.DONE) {
            state.update {
                it +
                    SubscriptionItem(
                        (it.maxOfOrNull(SubscriptionItem::id) ?: 0) + 1,
                        url,
                        url,
                        true,
                        null,
                    )
            }
        }
        return outcome
    }

    override suspend fun remove(id: Long) {
        state.update { items -> items.filterNot { it.id == id } }
    }

    override suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    ) {
        state.update { items -> items.map { if (it.id == id) it.copy(enabled = enabled) else it } }
    }

    override suspend fun refresh(id: Long): SubscriptionOutcome {
        refreshed += id
        return outcome
    }
}
