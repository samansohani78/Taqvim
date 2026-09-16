/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1102 (U): the athan never sounds twice for a prayer of a day; silent mode and Do Not Disturb are respected. */
class AthanPoliciesTest {
    @Test
    fun `each prayer of each day is recorded once and stays delivered`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.list(Arb.enum<AthanPrayer>(), 1..60),
                Arb.list(Arb.long(2_461_000L..2_461_010L), 60..60),
            ) { prayers, days ->
                val log = HistoryDeliveryLog()
                val keys =
                    prayers.mapIndexed {
                        index,
                        prayer,
                        ->
                        athanKey(PlannedAthan(prayer, Jdn(days[index]), NOON))
                    }
                val pending =
                    keys.map { key -> (log.state(key) == null).also { log.record(key, DeliveryState.DELIVERED) } }

                keys.filterIndexed { index, _ -> pending[index] } shouldBe keys.distinct()
                keys.distinct().forEach { log.state(it) shouldBe DeliveryState.DELIVERED }
            }
        }

    @Test
    fun `the history keeps the newest entries within its capacity`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(1..20), Arb.int(0..60)) { capacity, count ->
                val history =
                    (0 until count).fold(DeliveryHistory(emptyList(), capacity)) { acc, index ->
                        acc.with("DHUHR@$index", DeliveryState.DELIVERED)
                    }

                history.entries.size shouldBe minOf(capacity, count)
                history.entries shouldBe (maxOf(0, count - capacity) until count).map { "DHUHR@$it\tDELIVERED" }
            }
            athanKey(PlannedAthan(AthanPrayer.ISHA, Jdn(2_461_297), NOON)) shouldBe "ISHA@2461297"
            shouldThrow<IllegalArgumentException> { DeliveryHistory(emptyList(), capacity = 0) }
        }

    @Test
    fun `silent mode and Do Not Disturb mute the athan unless the Fajr bypass applies`() {
        val normal = DeviceAudioState(RingerState.NORMAL, doNotDisturb = false, policyAccessGranted = false)
        val vibrateMode = normal.copy(ringer = RingerState.VIBRATE)
        val silent = normal.copy(ringer = RingerState.SILENT)
        val dnd = normal.copy(doNotDisturb = true)
        val playback = AthanFixtures.PLAYBACK
        val bypass = playback.copy(bypassDndForFajr = true)

        fun decide(
            prayer: AthanPrayer,
            with: AthanPlayback,
            state: DeviceAudioState,
        ) = AthanAudibility.decide(prayer, with, state)

        decide(AthanPrayer.DHUHR, playback, normal) shouldBe AthanOutput.SOUND
        decide(AthanPrayer.DHUHR, playback, vibrateMode) shouldBe AthanOutput.VIBRATION_ONLY
        decide(AthanPrayer.DHUHR, playback.copy(vibrate = false), vibrateMode) shouldBe AthanOutput.NONE
        decide(AthanPrayer.DHUHR, playback, silent) shouldBe AthanOutput.NONE
        decide(AthanPrayer.DHUHR, playback, dnd) shouldBe AthanOutput.NONE
        decide(AthanPrayer.DHUHR, bypass, dnd.copy(policyAccessGranted = true)) shouldBe AthanOutput.NONE
        decide(AthanPrayer.FAJR, bypass, dnd) shouldBe AthanOutput.NONE
        decide(AthanPrayer.FAJR, bypass, dnd.copy(policyAccessGranted = true)) shouldBe AthanOutput.SOUND
        decide(AthanPrayer.FAJR, bypass, silent.copy(policyAccessGranted = true)) shouldBe AthanOutput.SOUND
        decide(AthanPrayer.FAJR, playback, silent.copy(policyAccessGranted = true)) shouldBe AthanOutput.NONE
        decide(AthanPrayer.ASR, playback.copy(volumePercent = 0), normal) shouldBe AthanOutput.VIBRATION_ONLY
        decide(AthanPrayer.FAJR, bypass.copy(volumePercent = 0), dnd.copy(policyAccessGranted = true)) shouldBe
            AthanOutput.VIBRATION_ONLY
        shouldThrow<IllegalArgumentException> { playback.copy(volumePercent = 101) }
    }

    private companion object {
        val NOON: Instant = Instant.parse("2026-09-13T08:50:00Z")
    }
}
