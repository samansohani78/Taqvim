/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/** What the map needs from the preferences and the chosen place (T-600, T-1502); bound in `:app`. */
data class MapSettings(
    /** App language: digits and dates. */
    val language: LanguageSpec,
    /** Zone of the shown date and time and of the time slider's day. */
    val timeZone: TimeZone,
    /** Calendar of the shown date. */
    val calendar: CalendarArithmetic = GregorianCalendarSystem,
    /** The chosen place (marker, Qibla and direct path), `null` while none is chosen. */
    val place: Coordinates? = null,
)

/** The current [MapSettings]. */
fun interface MapSettingsSource {
    fun settings(): Flow<MapSettings>
}

/**
 * The geomagnetic field at a place and time: [declinationDegrees] (positive east of true north), [inclinationDegrees]
 * (positive where the field points below the horizontal) and the total [fieldStrengthNanotesla].
 */
data class MagneticElements(
    val declinationDegrees: Double,
    val inclinationDegrees: Double,
    val fieldStrengthNanotesla: Double,
)

/** The geomagnetic field's elements at a place and time; the platform's World Magnetic Model. */
fun interface MagneticModel {
    fun elements(
        place: Coordinates,
        instant: Instant,
    ): MagneticElements
}

/** A city of the bundled catalog (T-603) shown as a map marker, named in the app language. */
@Immutable
data class MapCity(
    val id: Long,
    val name: String,
    val coordinates: Coordinates,
    val population: Long,
)

/** Catalog cities with a known population; bound in `:app`. */
fun interface MapCitySource {
    /** At most [limit] cities, most populous first, named in [languageCode]. */
    suspend fun citiesByPopulation(
        languageCode: String,
        limit: Int,
    ): List<MapCity>
}

/** Loads the world outline; bundled in this module's assets. */
fun interface WorldOutlineSource {
    suspend fun load(): WorldOutline
}

/** One-off events of the map screen. */
sealed interface MapEffect {
    /** The user picked [coordinates] on the map (e.g. for T-1502's map pick), or the marker of [city]. */
    data class LocationPicked(
        val coordinates: Coordinates,
        val city: MapCity? = null,
    ) : MapEffect
}
