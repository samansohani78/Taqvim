/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Coordinates
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowGeocoder

/** T-603 (R): the geocoder wrapper maps addresses and reports missing backends and failures as typed results. */
@RunWith(AndroidJUnit4::class)
class PlatformGeocoderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val geocoder = Geocoder(context, Locale.US)
    private val tehran = Coordinates(35.6892, 51.389)

    @After
    fun restoreBackend() {
        ShadowGeocoder.setIsPresent(true)
    }

    private fun address(
        locality: String,
        latitude: Double? = null,
    ) = Address(Locale.US).apply {
        this.locality = locality
        subAdminArea = "Tehran County"
        adminArea = "Tehran Province"
        countryCode = "IR"
        countryName = "Iran"
        latitude?.let {
            this.latitude = it
            longitude = 51.389
        }
    }

    @Test
    fun addressesAreMappedToPlaces(): Unit =
        runTest {
            shadowOf(geocoder).setFromLocation(listOf(address("Tehran", 35.6892), address("Shemiran")))

            val result = PlatformGeocoder(geocoder, StandardTestDispatcher(testScheduler)).placesAt(tehran, 2)

            val expected =
                listOf(
                    GeocodedPlace("Tehran", "Tehran County", "Tehran Province", "IR", "Iran", tehran),
                    GeocodedPlace("Shemiran", "Tehran County", "Tehran Province", "IR", "Iran", null),
                )
            assertEquals(GeocodeResult.Found(expected), result)
        }

    @Test
    fun anEmptyAnswerIsNotFound(): Unit =
        runBlocking {
            assertEquals(GeocodeResult.NotFound, PlatformGeocoder(context, Locale.US).placesAt(tehran))
        }

    @Test
    fun aDeviceWithoutBackendIsUnavailable(): Unit =
        runTest {
            ShadowGeocoder.setIsPresent(false)

            assertEquals(GeocodeResult.Unavailable, PlatformGeocoder(geocoder).placesAt(tehran))
        }

    @Test
    fun aBackendErrorIsFailed(): Unit =
        runTest {
            val offline = AddressLookup { _, _, _ -> throw IOException("service not available") }

            val result = PlatformGeocoder(offline, StandardTestDispatcher(testScheduler)).placesAt(tehran)

            assertEquals(GeocodeResult.Failed, result)
        }

    @Test
    fun maxResultsMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { PlatformGeocoder(geocoder).placesAt(tehran, 0) }
        }
    }
}
