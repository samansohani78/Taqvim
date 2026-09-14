/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanPreferences
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.ExactAlarmStatus
import ir.taqvim.feature.settings.AthanPrayerKind
import ir.taqvim.feature.settings.AthanSoundChoice
import ir.taqvim.feature.settings.PrayerAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1101 wiring: athan settings round-trip through the preferences, and exact-alarm access follows the scheduler. */
class AthanAdaptersTest {
    private val custom =
        AthanPreferences(
            alerts =
                AthanPrayer.entries.associateWith { prayer ->
                    val gap = if (prayer == AthanPrayer.ISHA) -10 else 5
                    AthanAlert(enabled = prayer == AthanPrayer.FAJR, gapMinutes = gap)
                },
            sound = AthanSound("content://sounds/1", "Athan"),
            vibrate = false,
            bypassDndForFajr = true,
            volumePercent = 40,
            useIranTime = true,
        )

    @Test
    fun `stored settings round-trip through the screen model`() {
        val feature = custom.toFeature()

        feature.alerts.keys shouldBe AthanPrayerKind.entries.toSet()
        feature.alerts.getValue(AthanPrayerKind.FAJR) shouldBe PrayerAlert(enabled = true, gapMinutes = 5)
        feature.sound shouldBe AthanSoundChoice("content://sounds/1", "Athan")
        feature.toData() shouldBe custom
        AthanPreferences.DEFAULT.toFeature().toData() shouldBe AthanPreferences.DEFAULT
    }

    @Test
    fun `screen values are made valid before they are stored`() {
        val feature =
            custom.toFeature().copy(
                alerts = mapOf(AthanPrayerKind.ASR to PrayerAlert(enabled = true, gapMinutes = 500)),
                sound = AthanSoundChoice(" ", null),
                volumePercent = 150,
            )

        val stored = feature.toData()

        stored.alerts.getValue(AthanPrayer.ASR) shouldBe AthanAlert(true, AthanAlert.GAP_RANGE.last)
        stored.alerts.getValue(AthanPrayer.FAJR) shouldBe AthanAlert.OFF
        stored.sound shouldBe null
        stored.volumePercent shouldBe 100
    }

    @Test
    fun `the store reads the language and updates the stored settings`(): Unit =
        runTest {
            val preferences = repositoryOf(UserPreferences.defaultsFor("fa"))
            val store = PreferencesAthanSettingsStore(preferences)

            store
                .settings()
                .first()
                .language.code shouldBe "fa"
            store.update { it.copy(vibrate = false, volumePercent = 55) }

            val athan = preferences.preferences.first().athan
            athan.vibrate shouldBe false
            athan.volumePercent shouldBe 55
            store.settings().first().settings shouldBe athan.toFeature()
        }

    @Test
    fun `exact alarm access follows the scheduler status`(): Unit =
        runTest {
            val status = MutableStateFlow(ExactAlarmStatus.INEXACT)
            val access = SchedulerExactAlarmAccess(status)

            access.allowed().first() shouldBe false
            status.value = ExactAlarmStatus.EXACT
            access.allowed().first() shouldBe true
        }
}
