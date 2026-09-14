/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.AlarmKey
import ir.taqvim.feature.notification.AthanAlertRule
import ir.taqvim.feature.notification.AthanPlayback
import ir.taqvim.feature.notification.AthanPrayer as PlannedPrayer
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1102 wiring: the athan setup comes from the preferences, and athans reach the scheduler as prayer alarms. */
class NotificationAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")

    @Test
    fun `there is no athan setup without a chosen place`() {
        athanSetup(persian).shouldBeNull()
    }

    @Test
    fun `the setup carries the place, the prayer conventions and the athan settings`() {
        val athan =
            persian.athan.copy(
                alerts = AthanPrayer.entries.associateWith { AthanAlert(it == AthanPrayer.FAJR, -5) },
                sound = AthanSound("content://sounds/2", "Adhan"),
                vibrate = false,
                bypassDndForFajr = true,
                volumePercent = 70,
                useIranTime = true,
            )

        val setup = athanSetup(persian.copy(place = tehran, athan = athan)).shouldNotBeNull()

        setup.plan.place shouldBe tehran.coordinates
        setup.plan.timeZone shouldBe TimeZone.of("Asia/Tehran")
        setup.plan.prayer shouldBe PrayerSettings(method = persian.prayerMethod, asr = persian.asrJuristic)
        setup.plan.useIranTime shouldBe true
        setup.plan.alerts.getValue(PlannedPrayer.FAJR) shouldBe AthanAlertRule(enabled = true, gapMinutes = -5)
        setup.plan.alerts.getValue(PlannedPrayer.ISHA) shouldBe AthanAlertRule(enabled = false, gapMinutes = -5)
        setup.plan.alerts.keys shouldBe PlannedPrayer.entries.toSet()
        setup.playback shouldBe AthanPlayback("content://sounds/2", 70, vibrate = false, bypassDndForFajr = true)
    }

    @Test
    fun `athans become prayer alarms and fired prayer alarms play the athan at their instant`(): Unit =
        runTest {
            val now = Instant.parse("2026-03-21T00:00:00Z")
            val fajr = Instant.parse("2026-03-21T01:30:00Z")
            val source = AthanAlarmSource { if (it == now) listOf(fajr) else emptyList() }

            source.kind shouldBe AlarmKind.PRAYER
            source.upcomingAlarms(now) shouldBe listOf(AlarmKey(AlarmKind.PRAYER, null, fajr))

            val played = mutableListOf<Instant>()
            val delivery = AthanAlarmDelivery { played += it }
            delivery.kind shouldBe AlarmKind.PRAYER
            delivery.deliver(
                ScheduledAlarmEntity(
                    kind = AlarmKind.PRAYER,
                    sourceId = null,
                    triggerAtEpochMillis = fajr.toEpochMilliseconds(),
                ),
            )
            played shouldBe listOf(fajr)
        }
}
