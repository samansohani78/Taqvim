/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import ir.taqvim.core.model.Coordinates
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A place returned by the platform geocoder; every field is optional because backends vary. */
data class GeocodedPlace(
    val locality: String?,
    val subAdminArea: String?,
    val adminArea: String?,
    val countryCode: String?,
    val countryName: String?,
    val coordinates: Coordinates?,
)

/** Outcome of a geocoder lookup. */
sealed interface GeocodeResult {
    data class Found(
        val places: List<GeocodedPlace>,
    ) : GeocodeResult

    /** The backend answered with no place. */
    data object NotFound : GeocodeResult

    /** This device has no geocoding backend. */
    data object Unavailable : GeocodeResult

    /** The backend failed (typically no network); the lookup can be retried. */
    data object Failed : GeocodeResult
}

/** Blocking address lookup behind [PlatformGeocoder]; replaceable in tests. */
internal fun interface AddressLookup {
    fun addressesAt(
        latitude: Double,
        longitude: Double,
        maxResults: Int,
    ): List<Address>
}

/** Reverse geocoding through the platform [Geocoder] on a background dispatcher, with typed results (T-603). */
class PlatformGeocoder internal constructor(
    private val lookup: AddressLookup,
    private val dispatcher: CoroutineDispatcher,
) {
    constructor(
        geocoder: Geocoder,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(geocoder.asLookup(), dispatcher)

    constructor(context: Context, locale: Locale) : this(Geocoder(context, locale))

    /** Up to [maxResults] places at [coordinates], most specific first. */
    suspend fun placesAt(
        coordinates: Coordinates,
        maxResults: Int = 1,
    ): GeocodeResult {
        require(maxResults > 0) { "maxResults must be positive (was $maxResults)" }
        if (!Geocoder.isPresent()) return GeocodeResult.Unavailable
        return withContext(dispatcher) {
            runCatching { lookup.addressesAt(coordinates.latitude, coordinates.longitude, maxResults) }.fold(
                onSuccess = { addresses ->
                    if (addresses.isEmpty()) GeocodeResult.NotFound else GeocodeResult.Found(addresses.map(::toPlace))
                },
                onFailure = { GeocodeResult.Failed },
            )
        }
    }

    private fun toPlace(address: Address): GeocodedPlace =
        GeocodedPlace(
            locality = address.locality,
            subAdminArea = address.subAdminArea,
            adminArea = address.adminArea,
            countryCode = address.countryCode,
            countryName = address.countryName,
            coordinates =
                if (address.hasLatitude() && address.hasLongitude()) {
                    runCatching { Coordinates(address.latitude, address.longitude) }.getOrNull()
                } else {
                    null
                },
        )
}

// The blocking overload works on every API level; PlatformGeocoder always calls it on a background dispatcher.
@Suppress("DEPRECATION")
private fun Geocoder.asLookup(): AddressLookup =
    AddressLookup { latitude, longitude, maxResults -> getFromLocation(latitude, longitude, maxResults).orEmpty() }
