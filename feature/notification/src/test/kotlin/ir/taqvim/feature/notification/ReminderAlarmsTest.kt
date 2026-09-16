/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1001/T-1002 (U): reminder alarms for the scheduler and their once-per-occurrence delivery. */
class ReminderAlarmsTest {
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val event =
        ReminderFixtures.event(
            ReminderFixtures.persian(1404, 12, 11),
            id = 3,
            recurrence = RecurrenceRule(Frequency.DAILY, count = 2),
            reminders = listOf(ReminderRule(9, 30)),
        )
    private val setup =
        ReminderFixtures.setup(
            events = listOf(event),
            officials = listOf(OfficialReminder(4, ReminderFixtures.NOWRUZ.id, 0)),
            schedule = CalculatorOfficialEventSchedule(listOf(ReminderFixtures.NOWRUZ), "en"),
        )

    @Test
    fun `alarms carry personal reminder ids and negated official ids`(): Unit =
        runTest {
            val alarms = ReminderAlarms({ setup }, HistoryDeliveryLog(), RecordingNotifier()).upcoming(now)

            alarms.map { it.sourceId }.take(3) shouldContainExactly listOf(9L, 9L, -4L)
            alarms.map { it.at } shouldBe ReminderPlanner.upcoming(now, setup).map { it.at }
        }

    @Test
    fun `a fired alarm shows its reminder once and nothing is shown for other instants`(): Unit =
        runTest {
            val notifier = RecordingNotifier()
            val log = HistoryDeliveryLog()
            val alarms = ReminderAlarms({ setup }, log, notifier)
            val first = alarms.upcoming(now).first()
            val key = "PERSONAL:9@${ReminderFixtures.jdnOf(event.start).value}"

            alarms.onAlarm(first.sourceId, first.at) shouldBe AlarmDeliveryResult.DELIVERED
            alarms.onAlarm(first.sourceId, first.at) shouldBe AlarmDeliveryResult.SKIPPED
            alarms.onAlarm(first.sourceId, first.at + 1.minutes) shouldBe AlarmDeliveryResult.SKIPPED
            alarms.onAlarm(-first.sourceId, first.at) shouldBe AlarmDeliveryResult.SKIPPED
            notifier.shown.map { it.key } shouldBe listOf(key)
            notifier.shown.single().title shouldBe "Event 3"
            log.history.stateOf(key) shouldBe DeliveryState.DELIVERED
        }

    @Test
    fun `a snoozed reminder is shown again and follows its event`(): Unit =
        runTest {
            val notifier = RecordingNotifier()
            val alarms = ReminderAlarms({ setup }, HistoryDeliveryLog(), notifier)
            val first = alarms.upcoming(now).first()
            alarms.onAlarm(first.sourceId, first.at)

            alarms.onAlarm(first.sourceId, first.at, snoozed = true) shouldBe AlarmDeliveryResult.DELIVERED
            notifier.shown.size shouldBe 2
            alarms.isPlanned(first.sourceId, first.at) shouldBe true
            alarms.isPlanned(first.sourceId + 100, first.at) shouldBe false
            val deleted =
                ReminderAlarms({ ReminderFixtures.setup(events = emptyList()) }, HistoryDeliveryLog(), notifier)
            deleted.onAlarm(first.sourceId, first.at, snoozed = true) shouldBe AlarmDeliveryResult.SKIPPED
        }

    @Test
    fun `a reminder that cannot be posted stays pending and is recorded as failed when given up`(): Unit =
        runTest {
            val log = HistoryDeliveryLog()
            val refusing = RecordingNotifier(accept = false)
            val alarms = ReminderAlarms({ setup }, log, refusing)
            val first = alarms.upcoming(now).first()
            val key = "PERSONAL:9@${ReminderFixtures.jdnOf(event.start).value}"

            alarms.onAlarm(first.sourceId, first.at) shouldBe AlarmDeliveryResult.FAILED
            log.history.stateOf(key) shouldBe null

            // Retried after posting became possible: shown once.
            val accepting = RecordingNotifier()
            ReminderAlarms({ setup }, log, accepting).onAlarm(first.sourceId, first.at) shouldBe
                AlarmDeliveryResult.DELIVERED
            accepting.shown.size shouldBe 1

            val other = HistoryDeliveryLog()
            ReminderAlarms({ setup }, other, refusing).onGaveUp(first.sourceId, first.at)
            other.history.stateOf(key) shouldBe DeliveryState.FAILED
            ReminderAlarms({ setup }, other, refusing).onGaveUp(first.sourceId, first.at + 1.minutes)
            other.history.entries.size shouldBe 1
        }

    @Test
    fun `the delivery history keeps the newest states within its capacity and reads old entries as delivered`() {
        val history =
            (1..5).fold(DeliveryHistory(emptyList(), capacity = 3)) { acc, index ->
                acc.with("k$index", DeliveryState.DELIVERED)
            }

        history.entries shouldBe listOf("k3\tDELIVERED", "k4\tDELIVERED", "k5\tDELIVERED")
        history.stateOf("k1") shouldBe null
        history.with("k3", DeliveryState.FAILED).entries shouldBe listOf("k4\tDELIVERED", "k5\tDELIVERED", "k3\tFAILED")
        DeliveryHistory(listOf("old", "bad\tUNKNOWN"), capacity = 3).stateOf("old") shouldBe DeliveryState.DELIVERED
        DeliveryHistory(listOf("bad\tUNKNOWN"), capacity = 3).stateOf("bad") shouldBe null
        shouldThrow<IllegalArgumentException> { DeliveryHistory(emptyList(), capacity = 0) }
    }
}
