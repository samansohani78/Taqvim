/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.times.TimesSettings
import ir.taqvim.feature.times.TimesSettingsSource
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [TimesSettingsSource] (T-1100) from the stored preferences and the place chosen in the location settings (T-1502).
 * Catalog cities are named in the app language; device and typed places keep their stored name. Emits `null` while no
 * place is chosen. The Astronomy (T-1300), compass (T-1302) and calendar (T-802) places follow this source.
 */
internal class PreferencesTimesSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val catalog: CityCatalogProvider,
) : TimesSettingsSource {
    override fun settings(): Flow<TimesSettings?> =
        preferences.preferences
            .map { preferences ->
                preferences.place?.let { place -> timesSettings(preferences, place, placeName(preferences, place)) }
            }.distinctUntilChanged()

    private suspend fun placeName(
        preferences: UserPreferences,
        place: ChosenPlace,
    ): String {
        val language = preferences.languageSpec().code
        val cityName = place.cityId?.let { id -> catalog.read { it.city(id)?.name(language) } }
        return cityName ?: place.name ?: coordinatesLabel(place.coordinates)
    }
}

/**
 * Times settings for [place] under [preferences]: [placeName], the stored coordinates and time zone, the prayer
 * conventions, the app language and the first of the user's calendars that is available. `null` when the stored zone
 * is unknown to the time zone database of this device.
 */
internal fun timesSettings(
    preferences: UserPreferences,
    place: ChosenPlace,
    placeName: String,
): TimesSettings? {
    val zone = runCatching { TimeZone.of(place.zoneId) }.getOrNull() ?: return null
    return TimesSettings(
        placeName = placeName,
        place = place.coordinates,
        timeZone = zone,
        prayer = PrayerSettings(method = preferences.prayerMethod, asr = preferences.asrJuristic),
        language = preferences.languageSpec(),
        calendar = preferences.availableCalendars().firstOrNull() ?: PersianCalendarSystem,
    )
}

/** A label for a place without a name: its coordinates to two decimals ("35.69, 51.42"). */
internal fun coordinatesLabel(coordinates: Coordinates): String =
    String.format(Locale.ROOT, "%.2f, %.2f", coordinates.latitude, coordinates.longitude)
