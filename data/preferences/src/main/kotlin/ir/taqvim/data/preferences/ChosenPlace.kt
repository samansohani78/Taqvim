/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import ir.taqvim.core.model.Coordinates
import ir.taqvim.data.preferences.proto.ChosenPlaceProto
import ir.taqvim.data.preferences.proto.PlaceSourceProto
import java.time.ZoneId

/** How the user chose a [ChosenPlace]. */
enum class PlaceSource {
    /** A city of the bundled catalog (T-603). */
    CITY,

    /** The device's position. */
    DEVICE,

    /** Coordinates typed by the user. */
    COORDINATES,
}

/** The place chosen in the location settings (T-1502): where times, Qibla and sky positions are computed. */
data class ChosenPlace(
    val source: PlaceSource,
    /** Catalog id for [PlaceSource.CITY]; `null` otherwise. */
    val cityId: Long?,
    /** Display name known when the place was chosen, or `null`. */
    val name: String?,
    val coordinates: Coordinates,
    /** IANA time zone id of the place. */
    val zoneId: String,
) {
    init {
        require((source == PlaceSource.CITY) == (cityId != null)) { "cityId is required exactly for CITY places" }
        require(isKnownZone(zoneId)) { "unknown time zone '$zoneId'" }
    }

    companion object {
        /** Whether [zoneId] is a time zone id of the platform's IANA database. */
        fun isKnownZone(zoneId: String): Boolean = zoneId in ZoneId.getAvailableZoneIds()
    }
}

private const val PLACE_SOURCE = "PLACE_SOURCE_"
private const val NO_CITY = 0L

/** The stored place, or `null` when none is stored or the stored value is unusable (e.g. an unknown zone). */
internal fun ChosenPlaceProto.toDomainOrNull(): ChosenPlace? {
    val source = PlaceSource.entries.firstOrNull { PLACE_SOURCE + it.name == this.source.name } ?: return null
    return runCatching {
        ChosenPlace(
            source = source,
            cityId = cityId.takeIf { source == PlaceSource.CITY },
            name = name.ifBlank { null },
            coordinates = Coordinates(latitude, longitude),
            zoneId = zoneId,
        )
    }.getOrNull()
}

internal fun ChosenPlace.toProto(): ChosenPlaceProto =
    ChosenPlaceProto
        .newBuilder()
        .setSource(PlaceSourceProto.valueOf(PLACE_SOURCE + source.name))
        .setCityId(cityId ?: NO_CITY)
        .setName(name.orEmpty())
        .setLatitude(coordinates.latitude)
        .setLongitude(coordinates.longitude)
        .setZoneId(zoneId)
        .build()
