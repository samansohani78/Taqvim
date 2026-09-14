/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.map.MapSettings
import ir.taqvim.feature.map.MapSettingsSource
import ir.taqvim.feature.settings.LocationSettingsStore
import ir.taqvim.feature.settings.PlaceChoice
import ir.taqvim.feature.settings.PlaceDescriber
import ir.taqvim.feature.settings.PlaceKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

/** The world map's settings (T-1301) from the stored preferences and place (T-600, T-1502). */
internal class PreferencesMapSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val deviceZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : MapSettingsSource {
    override fun settings(): Flow<MapSettings> =
        preferences.preferences
            .map { mapSettings(it, deviceZone()) }
            .distinctUntilChanged()
}

/**
 * Map settings under [preferences]: the app language, the stored place and its zone ([deviceZone] while no place is
 * stored or its zone is unknown here), and the first of the user's calendars that is available.
 */
internal fun mapSettings(
    preferences: UserPreferences,
    deviceZone: TimeZone,
): MapSettings {
    val place = preferences.place
    return MapSettings(
        language = preferences.languageSpec(),
        timeZone = place?.let { runCatching { TimeZone.of(it.zoneId) }.getOrNull() } ?: deviceZone,
        calendar = preferences.availableCalendars().firstOrNull() ?: PersianCalendarSystem,
        place = place?.coordinates,
    )
}

/**
 * Saves a point picked on the map as the chosen place (T-1301 → T-1502), after the user confirms it: a coordinates
 * place named and zoned by [describer], stored through [store].
 */
internal class MapPlacePicker(
    private val describer: PlaceDescriber,
    private val store: LocationSettingsStore,
) {
    /** Stores [coordinates] as the chosen place; `false` when it could not be described or stored. */
    suspend fun useAsPlace(coordinates: Coordinates): Boolean =
        runCatching {
            val description = describer.describe(coordinates)
            val choice = PlaceChoice(PlaceKind.COORDINATES, null, description.name, coordinates, description.timeZoneId)
            store.choose(choice)
        }.isSuccess
}

/** The map's settings source and the picked-point saver over the preferences and the location settings ports. */
val mapPortsModule =
    module {
        single<MapSettingsSource> { PreferencesMapSettingsSource(get()) }
        single { MapPlacePicker(get(), get()) }
    }
