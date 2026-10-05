/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.compass.CompassPlace
import ir.taqvim.feature.compass.CompassSettings
import ir.taqvim.feature.compass.CompassSettingsSource
import ir.taqvim.feature.times.TimesSettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * [CompassSettingsSource] (T-1302/T-1303) from the app language and the Times tab's chosen place (T-1100); the place
 * is `null` while no city is chosen.
 */
internal class PreferencesCompassSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val times: TimesSettingsSource,
) : CompassSettingsSource {
    override fun settings(): Flow<CompassSettings> =
        combine(preferences.preferences, times.settings()) { preferences, times ->
            CompassSettings(
                language = preferences.languageSpec(),
                place = times?.let { CompassPlace(it.placeName, it.place, it.timeZone) },
                calendar = times?.calendar ?: GregorianCalendarSystem,
            )
        }.distinctUntilChanged()
}
