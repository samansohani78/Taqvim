/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** ADR-0033 (U, fault injection): fired alarms stay pending until delivered, failures retry, snoozes persist. */
class AlarmDeliveryStateTest {
    private val clock = FakeClock(Instant.parse("2026-09-13T12:00:00Z"))
    private val start = clock.now()
    private val store = FakeAlarmStore()
    private val alarmClock = FakeAlarmClock()
    private val scheduler = AlarmScheduler(store, alarmClock, clock)
    private val reminders = FakeSource(AlarmKind.REMINDER, listOf(start + 1.hours, start + 2.hours))
    private val prayers = FakeSource(AlarmKind.PRAYER, listOf(start + 1.hours))
    private val reminderDelivery = FakeDelivery(AlarmKind.REMINDER)
    private val prayerDelivery = FakeDelivery(AlarmKind.PRAYER)
    private val coordinator =
        RescheduleCoordinator(
            scheduler,
            listOf(reminders, prayers),
            listOf(reminderDelivery, prayerDelivery),
            clock,
        )

    private suspend fun first(kind: AlarmKind): ScheduledAlarmEntity = store.alarms().first { it.kind == kind }

    private fun registeredAt(alarm: ScheduledAlarmEntity): Instant? = alarmClock.registered[alarm.id.toInt()]?.triggerAt

    @Test
    fun `a delivered alarm is removed and the next one scheduled`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.REMINDER)
            val due = first(AlarmKind.REMINDER)
            clock.advanceBy(1.hours)

            coordinator.onAlarmFired(due.id)

            reminderDelivery.delivered shouldBe listOf(due)
            store.alarms().map { it.toKey().triggerAt } shouldBe listOf(start + 2.hours)
            registeredAt(due) shouldBe null
        }

    @Test
    fun `a failed delivery stays pending, is retried and given up after the last attempt`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.REMINDER)
            val due = first(AlarmKind.REMINDER)
            reminderDelivery.outcome = DeliveryOutcome.FAILED
            clock.advanceBy(1.hours)

            coordinator.onAlarmFired(due.id)
            val retrying = store.alarms().first { it.id == due.id }
            retrying.attempts shouldBe 1
            registeredAt(due) shouldBe start + 1.hours + 1.minutes

            clock.advanceBy(1.minutes)
            coordinator.onAlarmFired(due.id)
            store.alarms().first { it.id == due.id }.attempts shouldBe 2

            clock.advanceBy(1.minutes)
            coordinator.onAlarmFired(due.id)
            store.alarms().none { it.id == due.id } shouldBe true
            reminderDelivery.delivered.size shouldBe 3
            reminderDelivery.gaveUp.map { it.id } shouldBe listOf(due.id)
            registeredAt(due) shouldBe null
        }

    @Test
    fun `a retry that would fire too late is given up at once`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.REMINDER)
            val due = first(AlarmKind.REMINDER)
            reminderDelivery.outcome = DeliveryOutcome.FAILED
            clock.advanceBy(1.hours + 14.minutes + 30.seconds)

            coordinator.onAlarmFired(due.id)

            store.alarms().none { it.id == due.id } shouldBe true
            reminderDelivery.gaveUp.map { it.id } shouldBe listOf(due.id)
        }

    @Test
    fun `a delivery interrupted by process death fires again from its lease`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.REMINDER)
            val due = first(AlarmKind.REMINDER)
            clock.advanceBy(1.hours)

            // The process dies after the scheduler leased the alarm but before the delivery reported back.
            scheduler.onFired(due.id).shouldBeInstanceOf<FiredAlarm>()
            registeredAt(due) shouldBe start + 1.hours + 1.minutes

            // A new process: recomputing keeps the in-flight alarm, and the lease fires it again.
            coordinator.recompute(AlarmKind.REMINDER)
            store.alarms().any { it.id == due.id } shouldBe true
            clock.advanceBy(1.minutes)
            coordinator.onAlarmFired(due.id)

            reminderDelivery.delivered.map { it.id } shouldBe listOf(due.id)
            store.alarms().none { it.id == due.id } shouldBe true
        }

    @Test
    fun `a pending lease is registered again after a reboot`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.REMINDER)
            val due = first(AlarmKind.REMINDER)
            clock.advanceBy(1.hours)
            scheduler.onFired(due.id)
            alarmClock.registered.clear()

            coordinator.handle(RescheduleEvent.BootCompleted)

            registeredAt(due) shouldBe start + 1.hours + 1.minutes
        }

    @Test
    fun `snoozes are stored, survive a reboot and are delivered like their alarm without recomputing`(): Unit =
        runTest {
            val planned = start + 1.hours
            val snooze = scheduler.snooze(AlarmKind.REMINDER_SNOOZE, 7, planned, planned + 10.minutes)
            scheduler.snooze(AlarmKind.REMINDER_SNOOZE, 7, planned, planned + 20.minutes)
            store.alarms().filter { it.kind == AlarmKind.REMINDER_SNOOZE }.map { it.toKey().triggerAt } shouldBe
                listOf(planned + 20.minutes)
            val latest = first(AlarmKind.REMINDER_SNOOZE)
            latest.snoozedFrom() shouldBe planned
            registeredAt(snooze) shouldBe null

            alarmClock.registered.clear()
            coordinator.handle(RescheduleEvent.BootCompleted)
            registeredAt(latest) shouldBe planned + 20.minutes

            val queries = reminders.queries
            clock.advanceBy(1.hours + 20.minutes)
            coordinator.onAlarmFired(latest.id)

            reminderDelivery.delivered shouldBe listOf(latest)
            store.alarms().none { it.kind == AlarmKind.REMINDER_SNOOZE } shouldBe true
            reminders.queries shouldBe queries
        }

    @Test
    fun `snoozes of deleted events are dropped when their kind is recomputed`(): Unit =
        runTest {
            val kept = scheduler.snooze(AlarmKind.REMINDER_SNOOZE, 1, start, start + 10.minutes)
            val deleted = scheduler.snooze(AlarmKind.REMINDER_SNOOZE, 2, start, start + 10.minutes)
            val athan = scheduler.snooze(AlarmKind.PRAYER_SNOOZE, null, start, start + 10.minutes)
            reminders.snoozable = { it.sourceId == 1L }

            coordinator.handle(RescheduleEvent.AlarmInputsChanged(setOf(AlarmKind.REMINDER)))

            store.alarms().filter { it.snoozedFrom() != null }.map { it.id } shouldBe listOf(kept.id, athan.id)
            registeredAt(deleted) shouldBe null
        }

    @Test
    fun `an athan snooze is delivered to the prayer delivery`(): Unit =
        runTest {
            val snooze = scheduler.snooze(AlarmKind.PRAYER_SNOOZE, null, start, start + 10.minutes)
            clock.advanceBy(10.minutes)

            coordinator.onAlarmFired(snooze.id)

            prayerDelivery.delivered shouldBe listOf(snooze)
            reminderDelivery.delivered.shouldBeEmpty()
        }

    @Test
    fun `snoozes follow the exact-alarm permission`(): Unit =
        runTest {
            alarmClock.exactPermitted = false
            val snooze = scheduler.snooze(AlarmKind.PRAYER_SNOOZE, null, start, start + 10.minutes)
            alarmClock.registered[snooze.id.toInt()]?.exact shouldBe false

            alarmClock.exactPermitted = true
            coordinator.handle(RescheduleEvent.ExactAlarmPermissionChanged)

            alarmClock.registered[snooze.id.toInt()] shouldBe RegisteredAlarm(start + 10.minutes, true)
        }

    @Test
    fun `only snooze kinds can be snoozed and outcomes combine failure first`() {
        runTest {
            shouldThrow<IllegalArgumentException> { scheduler.snooze(AlarmKind.REMINDER, 1, start, start) }
        }
        listOf(DeliveryOutcome.DELIVERED, DeliveryOutcome.FAILED).combined() shouldBe DeliveryOutcome.FAILED
        listOf(DeliveryOutcome.SKIPPED, DeliveryOutcome.DELIVERED).combined() shouldBe DeliveryOutcome.DELIVERED
        emptyList<DeliveryOutcome>().combined() shouldBe DeliveryOutcome.SKIPPED
        AlarmKind.SHIFT.snoozeKind() shouldBe null
        AlarmKind.PRAYER_SNOOZE.deliveryKind() shouldBe AlarmKind.PRAYER
    }
}
