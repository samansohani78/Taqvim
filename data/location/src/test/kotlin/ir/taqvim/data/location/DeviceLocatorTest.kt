/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** T-603 (R): permission, disabled providers, timeout, first fix and cleanup of location requests. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DeviceLocatorTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val manager: LocationManager = checkNotNull(application.getSystemService(LocationManager::class.java))
    private val shadowManager = shadowOf(manager)
    private val locator = DeviceLocator(application)

    private fun grant(vararg permissions: String) {
        shadowOf(application).denyPermissions(FINE, COARSE)
        shadowOf(application).grantPermissions(*permissions)
    }

    private fun enable(vararg providers: String) {
        listOf(GPS, NETWORK).forEach { shadowManager.setProviderEnabled(it, it in providers) }
    }

    private fun requests(provider: String): Int = shadowManager.getLocationRequests(provider).size

    @Test
    fun withoutPermissionNothingIsRequested(): Unit =
        runTest {
            grant()
            enable(GPS, NETWORK)

            assertEquals(LocationFix.PermissionDenied, locator.currentLocation())
            assertEquals(0, requests(GPS) + requests(NETWORK))
        }

    @Test
    fun disabledProvidersAreReported(): Unit =
        runTest {
            grant(FINE)
            enable()
            assertEquals(LocationFix.LocationDisabled, locator.currentLocation())

            grant(COARSE)
            enable(GPS)
            assertEquals(LocationFix.LocationDisabled, locator.currentLocation())
        }

    @Test
    fun timeoutRemovesTheRequest(): Unit =
        runTest {
            grant(FINE)
            enable(GPS)

            assertEquals(LocationFix.TimedOut, locator.currentLocation(5.seconds))
            assertEquals(0, requests(GPS))
        }

    @Test
    fun firstFixIsReturnedAndEveryRequestRemoved(): Unit =
        runTest {
            grant(FINE)
            enable(GPS, NETWORK)
            val fix = async { locator.currentLocation(1.minutes) }
            runCurrent()
            assertEquals(1, requests(GPS))
            assertEquals(1, requests(NETWORK))

            val location =
                Location(NETWORK).apply {
                    latitude = 35.7
                    longitude = 51.4
                    accuracy = 12f
                }
            shadowManager.simulateLocation(location)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(LocationFix.Found(Coordinates(35.7, 51.4), 12f, null, NETWORK), fix.await())
            assertEquals(0, requests(GPS) + requests(NETWORK))
        }

    @Test
    fun coarsePermissionUsesOnlyTheNetworkProviderAndCancellationCleansUp(): Unit =
        runTest {
            grant(COARSE)
            enable(GPS, NETWORK)
            val fix = async { locator.currentLocation(1.minutes) }
            runCurrent()
            assertEquals(0, requests(GPS))
            assertEquals(1, requests(NETWORK))

            fix.cancel()
            runCurrent()
            assertEquals(0, requests(NETWORK))
        }

    private companion object {
        const val FINE = Manifest.permission.ACCESS_FINE_LOCATION
        const val COARSE = Manifest.permission.ACCESS_COARSE_LOCATION
        const val GPS = LocationManager.GPS_PROVIDER
        const val NETWORK = LocationManager.NETWORK_PROVIDER
    }
}
