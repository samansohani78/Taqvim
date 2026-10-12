/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.location.City
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.settings.CityOption
import ir.taqvim.feature.settings.CitySearch
import ir.taqvim.feature.settings.LocationSettings
import ir.taqvim.feature.settings.LocationSettingsStore
import ir.taqvim.feature.settings.PlaceChoice
import ir.taqvim.feature.settings.PlaceDescriber
import ir.taqvim.feature.settings.PlaceDescription
import ir.taqvim.feature.settings.PlaceKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/** The bundled city catalog (T-603), parsed on first use; every read runs on [ioDispatcher]. */
internal class CityCatalogProvider(
    load: () -> CityCatalog = CityCatalog::loadBundled,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val catalog by lazy(load)

    suspend fun <T> read(block: (CityCatalog) -> T): T = withContext(ioDispatcher) { block(catalog) }
}

/** The app language code of the stored preferences at the moment of the call. */
internal suspend fun UserPreferencesRepository.currentLanguage(): String = preferences.first().languageSpec().code

/** [LocationSettingsStore] (T-1502) over the stored preferences (T-600). */
internal class PreferencesLocationSettingsStore(
    private val preferences: UserPreferencesRepository,
) : LocationSettingsStore {
    override fun settings(): Flow<LocationSettings> =
        preferences.preferences
            .map { LocationSettings(it.languageSpec(), it.place?.toChoice()) }
            .distinctUntilChanged()

    override suspend fun choose(place: PlaceChoice) {
        val chosen = place.toChosenPlace()
        preferences.update { it.copy(place = chosen) }
    }
}

/** This stored place as the location settings show it. */
internal fun ChosenPlace.toChoice(): PlaceChoice =
    PlaceChoice(
        kind =
            when (source) {
                PlaceSource.CITY -> PlaceKind.CITY
                PlaceSource.COORDINATES -> PlaceKind.COORDINATES
            },
        cityId = cityId,
        name = name,
        coordinates = coordinates,
        timeZoneId = zoneId,
    )

/** This choice as a stored place; fails when the choice is inconsistent (a city id only for cities, a known zone). */
internal fun PlaceChoice.toChosenPlace(): ChosenPlace =
    ChosenPlace(
        source =
            when (kind) {
                PlaceKind.CITY -> PlaceSource.CITY
                PlaceKind.COORDINATES -> PlaceSource.COORDINATES
            },
        cityId = cityId,
        name = name,
        coordinates = coordinates,
        zoneId = timeZoneId,
    )

/** [CitySearch] (T-1502) over the bundled catalog, with names in the current app [language]. */
internal class CatalogCitySearch(
    private val catalog: CityCatalogProvider,
    private val language: suspend () -> String,
) : CitySearch {
    override suspend fun search(query: String): List<CityOption> {
        val language = language()
        return catalog.read { cities -> cities.search(query).map { it.toOption(language) } }
    }
}

/** This city as a search result: its name in [language] and its region and country code as detail. */
internal fun City.toOption(language: String): CityOption =
    CityOption(
        id = id,
        name = name(language),
        detail = listOfNotNull(region, countryCode).joinToString(DETAIL_SEPARATOR).ifEmpty { null },
        coordinates = coordinates,
        timeZoneId = timeZoneId,
    )

/** Separates region and country code; a neutral symbol that reads the same in every script and direction. */
private const val DETAIL_SEPARATOR = " · "

/**
 * [PlaceDescriber] (T-1502): the nearest bundled catalog city names the coordinates, in the app [language], and
 * lends them its time zone; [defaultZone] when the catalog has none near enough.
 *
 * Until ADR-0055 this asked the platform `Geocoder` first and fell back to the catalog. On most devices that
 * geocoder is a network service, so naming a point the user typed or tapped meant sending those coordinates off
 * the device. The catalog answers the same question offline, and a point far from any city is simply unnamed.
 */
internal class CatalogPlaceDescriber(
    private val catalog: CityCatalogProvider,
    private val language: suspend () -> String,
    private val defaultZone: () -> String = { TimeZone.currentSystemDefault().id },
) : PlaceDescriber {
    override suspend fun describe(coordinates: Coordinates): PlaceDescription {
        val nearest = catalog.read { it.nearest(coordinates) }
        return PlaceDescription(
            name = nearest?.name(language()),
            timeZoneId = nearest?.timeZoneId ?: defaultZone(),
        )
    }
}
