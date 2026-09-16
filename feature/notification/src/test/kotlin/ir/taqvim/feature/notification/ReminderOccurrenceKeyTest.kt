/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.MinuteOfDay
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * T-1001 with T-1003: reminders are keyed by the occurrence's day in its series, so an occurrence moved onto the day of
 * another one keeps its own reminder, and reminders delivered under the older display-day key are not shown again.
 */
class ReminderOccurrenceKeyTest {
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val start = ReminderFixtures.persian(1405, 1, 1)
    private val first = ReminderFixtures.jdnOf(start)

    /** A daily 22:00 series whose second occurrence moves to the third day at [movedMinute]. */
    private fun series(movedMinute: MinuteOfDay): ReminderSetup {
        val daily =
            ReminderFixtures
                .event(
                    start,
                    startMinute = MinuteOfDay.of(22, 0),
                    recurrence = RecurrenceRule(Frequency.DAILY, count = 4),
                    reminders = listOf(ReminderRule(9, 0)),
                ).copy(overrides = listOf(ReminderOverride(first + 1, first + 2, movedMinute, "Moved")))
        return ReminderFixtures.setup(listOf(daily))
    }

    private val setup = series(MinuteOfDay.of(0, 30))

    @Test
    fun `a moved occurrence and the regular one on its day are both planned with their own keys`() {
        val planned = ReminderPlanner.upcoming(now, setup)

        planned.map { it.occurrence to it.original } shouldBe
            listOf(first to first, first + 2 to first + 1, first + 2 to first + 2, first + 3 to first + 3)
        planned.map { it.key }.toSet().size shouldBe planned.size
        planned.map { it.key } shouldBe listOf(0, 1, 2, 3).map { "PERSONAL:9#${(first + it).value}" }
        planned.filter { it.moved }.map { it.title } shouldBe listOf("Moved")
        planned.map { ReminderNotifications.idOf(it) }.toSet().size shouldBe planned.size
    }

    @Test
    fun `both occurrences on the shared day are delivered once each, also after a restart`(): Unit =
        runTest {
            val log = HistoryDeliveryLog()
            val notifier = RecordingNotifier()
            val onShared = ReminderPlanner.upcoming(now, setup).filter { it.occurrence == first + 2 }

            onShared.forEach {
                ReminderAlarms({ setup }, log, notifier).onAlarm(9, it.at) shouldBe
                    AlarmDeliveryResult.DELIVERED
            }
            // A restarted process (e.g. after a reboot) replays both alarms: nothing is shown twice.
            val restarted = ReminderAlarms({ setup }, log, notifier)
            restarted.upcoming(now).map { it.at } shouldBe ReminderPlanner.upcoming(now, setup).map { it.at }
            onShared.forEach { restarted.onAlarm(9, it.at) shouldBe AlarmDeliveryResult.SKIPPED }
            notifier.shown.map { it.title } shouldContainExactlyInAnyOrder listOf("Moved", "Event 1")
        }

    @Test
    fun `an occurrence moved onto another one's start is delivered with it from one alarm`(): Unit =
        runTest {
            val sameStart = series(MinuteOfDay.of(22, 0))
            val notifier = RecordingNotifier()
            val alarms = ReminderAlarms({ sameStart }, HistoryDeliveryLog(), notifier)
            val shared = ReminderPlanner.allAt(9, ReminderPlanner.upcoming(now, sameStart)[1].at, sameStart)

            shared.map { it.original } shouldBe listOf(first + 1, first + 2)
            alarms.upcoming(now).size shouldBe 4
            alarms.onAlarm(9, shared.first().at) shouldBe AlarmDeliveryResult.DELIVERED
            alarms.onAlarm(9, shared.first().at) shouldBe AlarmDeliveryResult.SKIPPED
            notifier.shown.map { it.title } shouldBe listOf("Moved", "Event 1")
        }

    @Test
    fun `reminders delivered under the old display-day key are not shown again after an update`(): Unit =
        runTest {
            val log = HistoryDeliveryLog()
            // Before the update only the moved occurrence was planned for the shared day, under the day it takes place.
            log.record("PERSONAL:9@${(first + 2).value}", DeliveryState.DELIVERED)
            log.record("PERSONAL:9@${first.value}", DeliveryState.DELIVERED)
            val notifier = RecordingNotifier()
            val alarms = ReminderAlarms({ setup }, log, notifier)
            val planned = ReminderPlanner.upcoming(now, setup)

            planned.map { alarms.onAlarm(9, it.at) } shouldBe
                listOf(
                    AlarmDeliveryResult.SKIPPED,
                    AlarmDeliveryResult.SKIPPED,
                    AlarmDeliveryResult.DELIVERED,
                    AlarmDeliveryResult.DELIVERED,
                )
            notifier.shown.map { it.original } shouldBe listOf(first + 2, first + 3)
            planned.map { it.legacyKeyed } shouldBe listOf(true, true, false, true)
        }

    @Test
    fun `a snoozed moved occurrence is shown again and stays planned`(): Unit =
        runTest {
            val notifier = RecordingNotifier()
            val alarms = ReminderAlarms({ setup }, HistoryDeliveryLog(), notifier)
            val moved = ReminderPlanner.upcoming(now, setup).single { it.moved }

            alarms.onAlarm(9, moved.at) shouldBe AlarmDeliveryResult.DELIVERED
            alarms.onAlarm(9, moved.at, snoozed = true) shouldBe AlarmDeliveryResult.DELIVERED
            alarms.isPlanned(9, moved.at) shouldBe true
            notifier.shown.map { it.key } shouldBe listOf(moved.key, moved.key)
            val cancelled =
                setup.copy(
                    events =
                        setup.events.map { event ->
                            event.copy(overrides = event.overrides.map { it.copy(cancelled = true) })
                        },
                )
            ReminderAlarms({ cancelled }, HistoryDeliveryLog(), notifier).isPlanned(9, moved.at) shouldBe false
        }

    @Test
    fun `a failure of one of two reminders at the same instant is retried, the delivered one is not repeated`(): Unit =
        runTest {
            val sameStart = series(MinuteOfDay.of(22, 0))
            val log = HistoryDeliveryLog()
            val at = ReminderPlanner.upcoming(now, sameStart)[1].at
            val keys = ReminderPlanner.allAt(9, at, sameStart).map { it.key }

            ReminderAlarms({ sameStart }, log, RecordingNotifier(accept = false)).onAlarm(9, at) shouldBe
                AlarmDeliveryResult.FAILED
            log.record(keys.first(), DeliveryState.DELIVERED)
            val retry = RecordingNotifier()
            ReminderAlarms({ sameStart }, log, retry).onAlarm(9, at) shouldBe AlarmDeliveryResult.DELIVERED
            retry.shown.map { it.key } shouldBe keys.drop(1)
            ReminderAlarms({ sameStart }, log, retry).onGaveUp(9, at)
            keys.map { log.history.stateOf(it) } shouldBe listOf(DeliveryState.DELIVERED, DeliveryState.DELIVERED)
        }
}
