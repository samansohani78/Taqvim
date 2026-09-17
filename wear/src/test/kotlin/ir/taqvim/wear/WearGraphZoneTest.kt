/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.data.location.CityCatalog
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith

/** Review I06: the watch setup follows the device time zone without being re-created. */
@RunWith(AndroidJUnit4::class)
class WearGraphZoneTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun theSetupFollowsDeviceZoneChanges(): Unit =
        runTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val zones = MutableStateFlow(WearFixtures.TEHRAN_ZONE)
            val graph =
                WearGraph(
                    preferences =
                        UserPreferencesRepository(
                            UserPreferencesRepository.createDataStore(application, scope) { "en" },
                        ),
                    clock = WearFixtures.fixedClock(WearFixtures.NOWRUZ_MORNING),
                    deviceZones = zones,
                    cityCatalog = lazy { CityCatalog.loadBundled() },
                    calculator = WearFixtures.calculator,
                )

            graph.setups.test {
                awaitItem().deviceZone shouldBe WearFixtures.TEHRAN_ZONE
                zones.value = TimeZone.of("Asia/Tokyo")
                awaitItem().deviceZone shouldBe TimeZone.of("Asia/Tokyo")
                cancelAndIgnoreRemainingEvents()
            }
            scope.cancel()
        }
}
