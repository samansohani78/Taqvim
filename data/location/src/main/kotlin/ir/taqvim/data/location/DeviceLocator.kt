/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import ir.taqvim.core.model.Coordinates
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Outcome of a one-shot device location request. */
sealed interface LocationFix {
    /** A position from [provider]; [altitudeMeters] is above the WGS 84 ellipsoid when the provider reports it. */
    data class Found(
        val coordinates: Coordinates,
        val accuracyMeters: Float?,
        val altitudeMeters: Double?,
        val provider: String?,
    ) : LocationFix

    /** Neither fine nor coarse location permission is granted; nothing was requested. */
    data object PermissionDenied : LocationFix

    /** Every provider usable with the granted permission is switched off. */
    data object LocationDisabled : LocationFix

    /** No position arrived before the timeout; the request has been removed. */
    data object TimedOut : LocationFix

    /** The platform refused the request (for example, the permission was revoked meanwhile). */
    data object Unavailable : LocationFix
}

/**
 * One-shot position from the platform [LocationManager] (no Play services), permission-aware and bounded by a timeout
 * (T-603). With fine permission GPS and network providers race; with coarse permission only the network provider is
 * used. Updates are removed as soon as a fix arrives, on timeout and on cancellation.
 */
class DeviceLocator(
    private val context: Context,
    private val locationManager: LocationManager =
        requireNotNull(context.getSystemService(LocationManager::class.java)),
) {
    suspend fun currentLocation(timeout: Duration = DEFAULT_TIMEOUT): LocationFix {
        val fine = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = fine || isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        val candidates = if (fine) FINE_PROVIDERS else COARSE_PROVIDERS
        val providers = candidates.filter(locationManager::isProviderEnabled)
        return when {
            !coarse -> LocationFix.PermissionDenied
            providers.isEmpty() -> LocationFix.LocationDisabled
            else -> withTimeoutOrNull(timeout) { firstFix(providers) } ?: LocationFix.TimedOut
        }
    }

    private fun isGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // currentLocation() checks the permission before requesting updates.
    private suspend fun firstFix(providers: List<String>): LocationFix =
        suspendCancellableCoroutine { continuation ->
            val listener =
                SingleFixListener(locationManager) { fix -> if (continuation.isActive) continuation.resume(fix) }
            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            runCatching {
                providers.forEach { provider ->
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }
            }.onFailure {
                locationManager.removeUpdates(listener)
                if (continuation.isActive) continuation.resume(LocationFix.Unavailable)
            }
        }

    companion object {
        val DEFAULT_TIMEOUT: Duration = 30.seconds
        private val FINE_PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        private val COARSE_PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER)
    }
}

/** Delivers the first location once and unregisters itself from every provider. */
private class SingleFixListener(
    private val locationManager: LocationManager,
    private val onFix: (LocationFix) -> Unit,
) : LocationListener {
    override fun onLocationChanged(location: Location) {
        locationManager.removeUpdates(this)
        onFix(
            LocationFix.Found(
                coordinates = Coordinates(location.latitude, location.longitude),
                accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
                altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
                provider = location.provider,
            ),
        )
    }

    // Before API 30 these callbacks have no default implementation in the platform interface.
    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    @Deprecated("Deprecated in the platform since API 29; never called on API 29 and later.")
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: Bundle?,
    ) = Unit
}
