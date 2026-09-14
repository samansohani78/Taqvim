/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences

/** One option of a watch choice list. */
data class WearChoice(
    val key: String,
    val label: String,
)

/** A city the watch offers in its settings. */
data class CityOption(
    val id: Long,
    val name: String,
)

/** Changes the watch settings screen makes to the stored preferences (T-1600, standalone watch per ADR-0019). */
object WearSettingsModel {
    /** Cities offered on the watch: the most populous catalog cities with a known time zone. */
    const val CITY_OPTIONS: Int = 80

    /** Calendars a watch user can make primary. */
    val PRIMARY_CALENDARS: List<CalendarSystem> =
        listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)

    fun withLanguage(
        preferences: UserPreferences,
        languageCode: String,
    ): UserPreferences = preferences.copy(languageCode = languageCode)

    /** [system] first, then the user's other calendars in their order. */
    fun withPrimaryCalendar(
        preferences: UserPreferences,
        system: CalendarSystem,
    ): UserPreferences = preferences.copy(calendars = listOf(system) + (preferences.calendars - system))

    fun withPrayerMethod(
        preferences: UserPreferences,
        method: PrayerMethod,
    ): UserPreferences = preferences.copy(prayerMethod = method)

    /** [city] as the chosen place; unchanged when the city has no time zone. */
    fun withCity(
        preferences: UserPreferences,
        city: City,
    ): UserPreferences {
        val zone = city.timeZoneId ?: return preferences
        return preferences.copy(
            place = ChosenPlace(PlaceSource.CITY, city.id, city.englishName, city.coordinates, zone),
        )
    }

    /** The [limit] most populous cities of [catalog] with a time zone, named in [languageCode]. */
    fun cityOptions(
        catalog: CityCatalog,
        languageCode: String,
        limit: Int = CITY_OPTIONS,
    ): List<CityOption> =
        catalog.cities
            .filter { it.timeZoneId != null }
            .sortedByDescending { it.population ?: 0L }
            .take(limit)
            .map { CityOption(it.id, it.name(languageCode)) }
}
