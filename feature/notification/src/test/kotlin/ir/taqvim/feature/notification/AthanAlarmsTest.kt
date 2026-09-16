/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1102 (U): the scheduler-facing side plans athan instants and plays each prayer of a day once. */
class AthanAlarmsTest {
    private val now = Instant.parse("2026-09-13T00:00:00Z")
    private val setup = AthanSetup(AthanFixtures.tehran(), AthanFixtures.PLAYBACK)

    private class Recorder(
        var result: Boolean = true,
    ) : AthanPlaybackStarter,
        AthanEventHook {
        val started = mutableListOf<AthanRequest>()
        val announced = mutableListOf<PlannedAthan>()

        override fun start(request: AthanRequest): Boolean = result.also { if (it) started += request }

        override fun onAthanStarted(athan: PlannedAthan) {
            announced += athan
        }
    }

    @Test
    fun `alarms are the planned instants and a fired one plays once per prayer and day`(): Unit =
        runTest {
            val recorder = Recorder()
            val log = HistoryDeliveryLog()
            val alarms = AthanAlarms({ setup }, log, recorder, recorder)

            val instants = alarms.upcoming(now)
            instants shouldBe AthanPlanner.upcoming(now, setup.plan).map { it.at }.distinct()
            val athan = AthanPlanner.upcoming(now, setup.plan).first()

            alarms.onAlarm(instants.first()) shouldBe AlarmDeliveryResult.DELIVERED
            alarms.onAlarm(instants.first()) shouldBe AlarmDeliveryResult.SKIPPED
            recorder.started shouldBe listOf(AthanRequest(athan, AthanFixtures.PLAYBACK))
            recorder.announced shouldBe listOf(athan)
            log.history.stateOf(athanKey(athan)) shouldBe DeliveryState.DELIVERED
            alarms.isPlanned(instants.first()) shouldBe true
        }

    @Test
    fun `a snoozed athan plays again`(): Unit =
        runTest {
            val recorder = Recorder()
            val alarms = AthanAlarms({ setup }, HistoryDeliveryLog(), recorder)
            val instant = alarms.upcoming(now).first()
            alarms.onAlarm(instant)

            alarms.onAlarm(instant, snoozed = true) shouldBe AlarmDeliveryResult.DELIVERED
            recorder.started.size shouldBe 2
            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).onAlarm(instant, snoozed = true) shouldBe
                AlarmDeliveryResult.SKIPPED
        }

    @Test
    fun `nothing plays without a setup, off-plan instants or a refused start`(): Unit =
        runTest {
            val recorder = Recorder()
            val athan = AthanPlanner.upcoming(now, setup.plan).first()
            val instant = athan.at

            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).upcoming(now).shouldBeEmpty()
            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).onAlarm(instant) shouldBe AlarmDeliveryResult.SKIPPED
            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).isPlanned(instant) shouldBe false
            AthanAlarms({ setup }, HistoryDeliveryLog(), recorder).onAlarm(instant + 1.minutes) shouldBe
                AlarmDeliveryResult.SKIPPED
            recorder.started.shouldBeEmpty()

            recorder.result = false
            val log = HistoryDeliveryLog()
            val refused = AthanAlarms({ setup }, log, recorder, recorder)
            refused.onAlarm(instant) shouldBe AlarmDeliveryResult.FAILED
            recorder.announced.shouldBeEmpty()
            log.history.stateOf(athanKey(athan)) shouldBe null
            refused.onGaveUp(instant)
            log.history.stateOf(athanKey(athan)) shouldBe DeliveryState.FAILED
            refused.onGaveUp(instant + 1.minutes)
            log.history.entries.size shouldBe 1
            AthanEventHook.NONE.onAthanStarted(athan)
        }
}
