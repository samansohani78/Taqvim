/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.CalendarCalendars
import ir.taqvim.feature.times.TimesSettings
import ir.taqvim.feature.times.TimesSettingsSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone

/** The id of the city the user chose for times, or `null` while none is chosen. */
fun interface ChosenCitySource {
    fun cityId(): Flow<Long?>
}

/**
 * [TimesSettingsSource] (T-1100) from the stored preferences and the chosen city of the bundled catalog (T-603). Emits
 * `null` while no city is chosen or the chosen city cannot be used (unknown id or no time zone in the source).
 */
internal class PreferencesTimesSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val chosenCity: ChosenCitySource,
    loadCatalog: () -> CityCatalog = CityCatalog::loadBundled,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TimesSettingsSource {
    private val catalog by lazy(loadCatalog)

    override fun settings(): Flow<TimesSettings?> =
        combine(preferences.preferences, chosenCity.cityId().distinctUntilChanged()) { preferences, cityId ->
            cityId?.let(catalog::city)?.let { timesSettings(preferences, it) }
        }.distinctUntilChanged().flowOn(ioDispatcher)
}

/**
 * Times settings for [city] under [preferences]: the city's name in the app language, its coordinates and time zone,
 * the prayer conventions, and the first of the user's calendars that is available. `null` when the city has no time
 * zone, since prayer times cannot be dated without one.
 */
internal fun timesSettings(
    preferences: UserPreferences,
    city: City,
): TimesSettings? {
    val zone = city.timeZoneId?.let { id -> runCatching { TimeZone.of(id) }.getOrNull() } ?: return null
    val language =
        LanguageTable.forCode(preferences.languageCode)
            ?: LanguageTable.forCode(UserPreferences.FALLBACK_LANGUAGE)
            ?: return null
    val calendar =
        preferences.calendars.firstNotNullOfOrNull { CalendarCalendars.arithmeticFor(it, preferences.islamicVariant) }
    return TimesSettings(
        placeName = city.name(language.code),
        place = city.coordinates,
        timeZone = zone,
        prayer = PrayerSettings(method = preferences.prayerMethod, asr = preferences.asrJuristic),
        language = language,
        calendar = calendar ?: PersianCalendarSystem,
    )
}
