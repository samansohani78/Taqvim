/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Coordinates
import kotlinx.coroutines.flow.Flow

/** How a place was chosen. */
enum class PlaceKind {
    CITY,
    COORDINATES,
}

/** A chosen place (T-1502); stored by [LocationSettingsStore]. */
data class PlaceChoice(
    val kind: PlaceKind,
    /** Catalog id for [PlaceKind.CITY]; `null` otherwise. */
    val cityId: Long?,
    /** Display name, or `null` when none is known. */
    val name: String?,
    val coordinates: Coordinates,
    /** IANA time zone id. */
    val timeZoneId: String,
)

/** What the location settings show: the app language and the stored place. */
data class LocationSettings(
    val language: LanguageSpec,
    val place: PlaceChoice?,
)

/** Reads and stores the chosen place; bound in `:app` over the user preferences. */
interface LocationSettingsStore {
    fun settings(): Flow<LocationSettings>

    suspend fun choose(place: PlaceChoice)
}

/** A city offered by [CitySearch], with its name already in the app language. */
data class CityOption(
    val id: Long,
    val name: String,
    /** Region and country, or `null`. */
    val detail: String?,
    val coordinates: Coordinates,
    /** IANA time zone id, or `null` when the catalog has none (such a city cannot be chosen). */
    val timeZoneId: String?,
)

/** Searches the city catalog; results come ranked and at most a page long. */
fun interface CitySearch {
    suspend fun search(query: String): List<CityOption>
}

/** A name and time zone for coordinates. */
data class PlaceDescription(
    /** Name of the nearest city in the bundled catalog, or `null` when none is near enough to name. */
    val name: String?,
    /** IANA time zone id suggested for the coordinates. */
    val timeZoneId: String,
)

/**
 * Describes coordinates from the bundled city catalog (ADR-0055).
 *
 * This used to ask the platform geocoder first, which on most devices means sending the coordinates to a network
 * service. Naming a point the user typed or tapped is not worth that, so the nearest bundled city names it.
 */
fun interface PlaceDescriber {
    suspend fun describe(coordinates: Coordinates): PlaceDescription
}

/** Checks IANA time zone ids. */
fun interface TimeZoneIds {
    fun isKnown(id: String): Boolean
}
