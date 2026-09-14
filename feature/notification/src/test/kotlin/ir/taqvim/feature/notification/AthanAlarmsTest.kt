/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
            val alarms = AthanAlarms({ setup }, HistoryDeliveryLog(), recorder, recorder)

            val instants = alarms.upcoming(now)
            instants shouldBe AthanPlanner.upcoming(now, setup.plan).map { it.at }.distinct()

            val first = alarms.onAlarm(instants.first()).shouldNotBeNull()
            first.athan shouldBe AthanPlanner.upcoming(now, setup.plan).first()
            first.playback shouldBe AthanFixtures.PLAYBACK
            alarms.onAlarm(instants.first()).shouldBeNull()
            recorder.started shouldBe listOf(first)
            recorder.announced shouldBe listOf(first.athan)
        }

    @Test
    fun `nothing plays without a setup, off-plan instants or a refused start`(): Unit =
        runTest {
            val recorder = Recorder()
            val instant = AthanPlanner.upcoming(now, setup.plan).first().at

            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).upcoming(now).shouldBeEmpty()
            AthanAlarms({ null }, HistoryDeliveryLog(), recorder).onAlarm(instant).shouldBeNull()
            AthanAlarms({ setup }, HistoryDeliveryLog(), recorder).onAlarm(instant + 1.minutes).shouldBeNull()
            recorder.started.shouldBeEmpty()

            recorder.result = false
            AthanAlarms({ setup }, HistoryDeliveryLog(), recorder, recorder).onAlarm(instant).shouldBeNull()
            recorder.announced.shouldBeEmpty()
            AthanEventHook.NONE.onAthanStarted(AthanPlanner.upcoming(now, setup.plan).first())
        }
}
