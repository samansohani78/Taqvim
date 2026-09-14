/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
            val alarms = ReminderAlarms({ setup }, HistoryReminderLog(), RecordingNotifier()).upcoming(now)

            alarms.map { it.sourceId }.take(3) shouldContainExactly listOf(9L, 9L, -4L)
            alarms.map { it.at } shouldBe ReminderPlanner.upcoming(now, setup).map { it.at }
        }

    @Test
    fun `a fired alarm shows its reminder once and nothing is shown for other instants`(): Unit =
        runTest {
            val notifier = RecordingNotifier()
            val alarms = ReminderAlarms({ setup }, HistoryReminderLog(), notifier)
            val first = alarms.upcoming(now).first()

            alarms.onAlarm(first.sourceId, first.at).shouldNotBeNull().title shouldBe "Event 3"
            alarms.onAlarm(first.sourceId, first.at).shouldBeNull()
            alarms.onAlarm(first.sourceId, first.at + 1.minutes).shouldBeNull()
            alarms.onAlarm(-first.sourceId, first.at).shouldBeNull()
            notifier.shown.map { it.key } shouldBe listOf("PERSONAL:9@${ReminderFixtures.jdnOf(event.start).value}")
        }

    @Test
    fun `a reminder that cannot be posted is not reported as shown`(): Unit =
        runTest {
            val alarms = ReminderAlarms({ setup }, HistoryReminderLog(), RecordingNotifier(accept = false))
            val first = alarms.upcoming(now).first()

            alarms.onAlarm(first.sourceId, first.at).shouldBeNull()
        }

    @Test
    fun `the delivery history keeps the newest keys within its capacity`() {
        val history =
            (1..5).fold(
                ReminderDeliveryHistory(emptyList(), capacity = 3),
            ) { acc, index -> acc.plus("k$index") }

        history.entries shouldBe listOf("k3", "k4", "k5")
        history.contains("k1") shouldBe false
        history.plus("k3").entries shouldBe listOf("k4", "k5", "k3")
    }
}
