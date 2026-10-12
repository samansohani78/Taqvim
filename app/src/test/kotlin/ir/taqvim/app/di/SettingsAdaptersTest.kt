/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.core.ui.theme.ThemeMode as UiThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.LevelOffset
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.compass.DeviceOrientation
import ir.taqvim.feature.compass.LevelCalibration
import ir.taqvim.feature.compass.Tilt
import ir.taqvim.feature.settings.ThemeChoice
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1500 wiring: the settings screens, search history, level calibration, theme and prayer rule. */
class SettingsAdaptersTest {
    private val persian = UserPreferences.defaultsFor("fa")

    @Test
    fun `settings round-trip and changes reach the stored preferences`() {
        persian.withGeneralSettings(persian.toGeneralSettings()) shouldBe persian

        val remembered = persian.copy(app = persian.app.copy(recentSearches = listOf("nowruz")))
        val rule = HighLatitudeRule.entries.last()
        val changed =
            remembered.toGeneralSettings().copy(
                theme = ThemeChoice.DARK,
                dynamicColor = false,
                showWeekNumbers = true,
                enabledEventSources =
                    AppSettings.SELECTABLE_SOURCES.take(1).toSet() + ir.taqvim.core.events.EventSource.USER,
                highLatitudeRule = rule,
                rememberRecentSearches = false,
                allDayReminderMinute = 450,
                persistentNotificationLargeNumber = true,
                dynamicLauncherIcon = true,
            )
        val stored = remembered.withGeneralSettings(changed)

        stored.app.persistentNotificationLargeNumber shouldBe true
        stored.app.dynamicLauncherIcon shouldBe true
        stored.toGeneralSettings().dynamicLauncherIcon shouldBe true

        stored.themeMode.name shouldBe "DARK"
        stored.app.dynamicColor shouldBe false
        stored.app.showWeekNumbers shouldBe true
        stored.app.enabledEventSources shouldBe AppSettings.SELECTABLE_SOURCES.take(1).toSet()
        stored.app.highLatitudeRule shouldBe rule
        stored.app.recentSearches shouldBe emptyList()
        stored.app.allDayReminderMinute shouldBe 450
        stored.withGeneralSettings(changed.copy(allDayReminderMinute = 5_000)).app.allDayReminderMinute shouldBe 450
        stored.prayerSettings().highLatitude shouldBe rule
        stored.toGeneralSettings().hasRecentSearches shouldBe false
        remembered.toGeneralSettings().hasRecentSearches shouldBe true
    }

    @Test
    fun `event sources follow the language until chosen and a choice survives a language change`() {
        val dari = persian.withGeneralSettings(persian.toGeneralSettings().copy(languageCode = "prs"))
        dari.app.enabledEventSources shouldBe setOf(EventSource.AFGHANISTAN_OFFICIAL, EventSource.INTERNATIONAL)
        dari.app.eventSourcesChosen shouldBe false

        val international = persian.toGeneralSettings().copy(enabledEventSources = setOf(EventSource.INTERNATIONAL))
        val chosen = persian.withGeneralSettings(international)
        chosen.app.eventSourcesChosen shouldBe true
        chosen
            .withGeneralSettings(chosen.toGeneralSettings().copy(languageCode = "prs"))
            .app.enabledEventSources shouldBe setOf(EventSource.INTERNATIONAL)
    }

    @Test
    fun `the theme follows the stored appearance`() {
        val stored =
            persian.copy(
                themeMode = ir.taqvim.data.preferences.ThemeMode.BLACK,
                app = persian.app.copy(dynamicColor = false, highContrast = true, boldText = true, gradient = true),
            )

        stored.themeSettings() shouldBe
            ThemeSettings(
                mode = UiThemeMode.BLACK,
                dynamicColor = false,
                gradient = true,
                highContrast = true,
                boldText = true,
            )
    }

    @Test
    fun `the store saves settings and clears the search history`(): Unit =
        runTest {
            val preferences = repositoryOf(persian.copy(app = persian.app.copy(recentSearches = listOf("a"))))
            val store = PreferencesGeneralSettingsStore(preferences)

            store
                .settings()
                .first()
                .language.code shouldBe "fa"
            store.update { it.copy(showWeekNumbers = true) }
            preferences.preferences
                .first()
                .app.showWeekNumbers shouldBe true

            store.clearRecentSearches()
            preferences.preferences
                .first()
                .app.recentSearches shouldBe emptyList()
        }

    @Test
    fun `recent searches are newest first, distinct, capped and only kept while remembered`(): Unit =
        runTest {
            val preferences = repositoryOf(persian)
            val store = PreferencesRecentQueriesStore(preferences)

            store.add("Nowruz")
            store.add("Yalda")
            store.add(" Nowruz ")
            store.add("   ")
            store.queries().first() shouldBe listOf("Nowruz", "Yalda")

            (1..12).forEach { store.add("query $it") }
            store.queries().first().size shouldBe AppSettings.MAX_RECENT_SEARCHES
            store.queries().first().first() shouldBe "query 12"

            store.clear()
            preferences.update { it.copy(app = it.app.copy(rememberRecentSearches = false)) }
            store.add("Nowruz")
            store.queries().first() shouldBe emptyList()
        }

    @Test
    fun `level calibration is stored per orientation within the stored range`(): Unit =
        runTest {
            val preferences = repositoryOf(persian)
            val store = PreferencesLevelCalibrationStore(preferences)
            val calibration =
                LevelCalibration(
                    persistentMapOf(
                        DeviceOrientation.FLAT to Tilt(1.5, -2.0),
                        DeviceOrientation.PORTRAIT to Tilt(60.0, 0.0),
                    ),
                )

            store.save(calibration)

            store.calibration().first() shouldBe
                LevelCalibration(
                    persistentMapOf(
                        DeviceOrientation.FLAT to Tilt(1.5, -2.0),
                        DeviceOrientation.PORTRAIT to Tilt(45.0, 0.0),
                    ),
                )
            mapOf("SIDEWAYS" to LevelOffset(1.0, 1.0)).toCalibration() shouldBe LevelCalibration()
        }
}
