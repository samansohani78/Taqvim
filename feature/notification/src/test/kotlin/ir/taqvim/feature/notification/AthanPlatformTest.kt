/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf

/** T-1102 (R): requests in intents, the persistent delivery log, the service starter and the Koin module. */
@RunWith(AndroidJUnit4::class)
class AthanPlatformTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun requestsTravelInServiceIntents() {
        val request =
            AthanFixtures.request(
                AthanPrayer.MAGHRIB,
                AthanFixtures.PLAYBACK.copy(soundUri = "content://sounds/athan.mp3", bypassDndForFajr = true),
            )
        val intent = AthanIntents.of(context, AthanIntents.ACTION_PLAY, request)

        intent.component?.className shouldBe AthanService::class.java.name
        AthanIntents.requestOf(intent) shouldBe request
        AthanIntents.requestOf(AthanIntents.of(context, AthanIntents.ACTION_STOP)).shouldBeNull()

        fun played() = AthanIntents.of(context, AthanIntents.ACTION_PLAY, request)
        AthanIntents.requestOf(played().putExtra(VOLUME, 101)).shouldBeNull()
        AthanIntents.requestOf(played().putExtra(PRAYER, "SUNRISE")).shouldBeNull()
        AthanIntents.requestOf(played().removeExtraOf(DAY)).shouldBeNull()
        AthanIntents.requestOf(played().removeExtraOf(AT)).shouldBeNull()
    }

    @Test
    fun deliveriesAreRememberedAcrossInstances(): Unit =
        runTest {
            val day = Jdn(2_461_297)
            SharedPreferencesAthanDeliveryLog(context).claim(AthanPrayer.FAJR, day) shouldBe true
            SharedPreferencesAthanDeliveryLog(context).claim(AthanPrayer.FAJR, day) shouldBe false
            SharedPreferencesAthanDeliveryLog(context).claim(AthanPrayer.FAJR, day + 1) shouldBe true
            SharedPreferencesAthanDeliveryLog(context).claim(AthanPrayer.ISHA, day) shouldBe true

            val log = SharedPreferencesAthanDeliveryLog(context)
            (1..AthanDeliveryHistory.DEFAULT_CAPACITY).forEach {
                log.claim(AthanPrayer.DHUHR, day + 10 + it) shouldBe true
            }
            log.claim(AthanPrayer.FAJR, day) shouldBe true
            log.claim(AthanPrayer.DHUHR, day + 10 + AthanDeliveryHistory.DEFAULT_CAPACITY) shouldBe false
        }

    @Test
    fun theStarterStartsTheServiceInTheForeground() {
        val request = AthanFixtures.request()

        ServiceAthanPlaybackStarter(context).start(request) shouldBe true

        val application = ApplicationProvider.getApplicationContext<Application>()
        val started = shadowOf(application).nextStartedService.shouldNotBeNull()
        started.component?.className shouldBe AthanService::class.java.name
        started.action shouldBe AthanIntents.ACTION_PLAY
        AthanIntents.requestOf(started) shouldBe request
    }

    @Test
    fun theKoinModuleProvidesTheAlarms() {
        val koin =
            koinApplication {
                androidContext(context)
                modules(notificationFeatureModule, module { single<AthanSetupSource> { AthanSetupSource { null } } })
            }.koin

        koin.get<AthanAlarms>().shouldNotBeNull()
        koin.get<AthanDeliveryLog>().shouldNotBeNull()
    }

    private fun android.content.Intent.removeExtraOf(name: String) = apply { removeExtra(name) }

    private companion object {
        const val PRAYER = "ir.taqvim.feature.notification.extra.PRAYER"
        const val DAY = "ir.taqvim.feature.notification.extra.DAY"
        const val AT = "ir.taqvim.feature.notification.extra.AT"
        const val VOLUME = "ir.taqvim.feature.notification.extra.VOLUME"
    }
}
