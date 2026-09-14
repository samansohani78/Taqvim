/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.preferences.proto.AthanPrayerAlertProto
import ir.taqvim.data.preferences.proto.AthanPrayerProto
import ir.taqvim.data.preferences.proto.AthanSettingsProto
import ir.taqvim.data.preferences.proto.UserPrefs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1101: athan settings survive the proto, older stores read as the defaults and stored oddities are repaired. */
class AthanPreferencesMappingTest {
    private val alertArb =
        Arb.list(Arb.int(-60..60), AthanPrayer.entries.size..AthanPrayer.entries.size).map { gaps ->
            AthanPrayer.entries.zip(gaps).associate { (prayer, gap) -> prayer to AthanAlert(gap % 2 == 0, gap) }
        }

    @Test
    fun `defaults are the same for every language and have every prayer off`() {
        LanguageTable.languages.forEach { spec ->
            UserPreferences.defaultsFor(spec.code).athan shouldBe AthanPreferences.DEFAULT
        }
        AthanPreferences.DEFAULT.alerts.values
            .all { !it.enabled && it.gapMinutes == 0 } shouldBe true
        AthanPreferences.DEFAULT.alerts.keys shouldBe AthanPrayer.entries.toSet()
    }

    @Test
    fun `random athan settings round-trip through the proto and the serializer`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                alertArb,
                Arb.int(0..100),
                Arb.boolean(),
                Arb.boolean(),
                Arb.boolean(),
            ) { alerts, volume, vibrate, bypass, iranTime ->
                val athan =
                    AthanPreferences(
                        alerts = alerts,
                        sound = if (vibrate) AthanSound("content://media/audio/$volume", "Athan $volume") else null,
                        vibrate = vibrate,
                        bypassDndForFajr = bypass,
                        volumePercent = volume,
                        useIranTime = iranTime,
                    )
                val prefs = UserPreferences.defaultsFor("fa").copy(athan = athan)
                val bytes = ByteArrayOutputStream().also { UserPrefsSerializer.writeTo(prefs.toProto(), it) }
                UserPrefsSerializer.readFrom(ByteArrayInputStream(bytes.toByteArray())).toDomain() shouldBe prefs
            }
        }

    @Test
    fun `stores written before T-1101 read as the athan defaults`() {
        val older =
            UserPreferences
                .defaultsFor("fa")
                .toProto()
                .toBuilder()
                .clearAthan()
                .build()

        older.hasAthan() shouldBe false
        older.toDomain().athan shouldBe AthanPreferences.DEFAULT
        UserPrefs
            .getDefaultInstance()
            .toDomain()
            .athan shouldBe AthanPreferences.DEFAULT
    }

    @Test
    fun `stored oddities are repaired`() {
        fun alert(
            prayer: AthanPrayerProto,
            enabled: Boolean,
            gap: Int,
        ) = AthanPrayerAlertProto
            .newBuilder()
            .setPrayer(prayer)
            .setEnabled(enabled)
            .setGapMinutes(gap)
            .build()

        val stored =
            AthanSettingsProto
                .newBuilder()
                .addAlerts(alert(AthanPrayerProto.ATHAN_PRAYER_FAJR, true, -500))
                .addAlerts(alert(AthanPrayerProto.ATHAN_PRAYER_FAJR, false, 10))
                .addAlerts(alert(AthanPrayerProto.ATHAN_PRAYER_UNSPECIFIED, true, 5))
                .addAlerts(alert(AthanPrayerProto.ATHAN_PRAYER_ISHA, true, 900))
                .addAlerts(
                    AthanPrayerAlertProto
                        .newBuilder()
                        .setPrayerValue(42)
                        .setEnabled(true)
                        .build(),
                ).setSoundUri(" ")
                .setSoundName("Ignored")
                .setVolumePercent(250)
                .build()

        val athan = stored.toDomain()

        athan.alerts.getValue(AthanPrayer.FAJR) shouldBe AthanAlert(true, -60)
        athan.alerts.getValue(AthanPrayer.ISHA) shouldBe AthanAlert(true, 60)
        athan.alerts.getValue(AthanPrayer.DHUHR) shouldBe AthanAlert.OFF
        athan.sound shouldBe null
        athan.volumePercent shouldBe 100
        stored
            .toBuilder()
            .setSoundUri("content://a")
            .setSoundName("")
            .setVolumePercent(-3)
            .build()
            .toDomain()
            .let { repaired ->
                repaired.sound shouldBe AthanSound("content://a", null)
                repaired.volumePercent shouldBe 0
            }
    }

    @Test
    fun `athan values are validated`() {
        shouldThrow<IllegalArgumentException> { AthanAlert(true, 61) }
        shouldThrow<IllegalArgumentException> { AthanSound(" ", null) }
        shouldThrow<IllegalArgumentException> { AthanPreferences.DEFAULT.copy(volumePercent = 101) }
        shouldThrow<IllegalArgumentException> {
            AthanPreferences.DEFAULT.copy(alerts = AthanPreferences.DEFAULT.alerts - AthanPrayer.ASR)
        }
    }
}
