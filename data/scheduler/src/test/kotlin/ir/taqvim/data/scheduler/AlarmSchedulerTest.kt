/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.database.AlarmKind
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-604 (U): the scheduler keeps the table and the system alarms in step, idempotently. */
class AlarmSchedulerTest {
    private val clock = FakeClock(Instant.parse("2026-09-13T12:00:00Z"))
    private val start = clock.now()
    private val store = FakeAlarmStore()
    private val alarmClock = FakeAlarmClock()
    private val scheduler = AlarmScheduler(store, alarmClock, clock)

    private fun prayer(minutes: Int) = AlarmKey(AlarmKind.PRAYER, null, start + minutes.minutes)

    @Test
    fun `replacing registers each alarm once and repeating it changes nothing`(): Unit =
        runTest {
            scheduler.replace(AlarmKind.PRAYER, listOf(prayer(30), prayer(30), prayer(90)))

            alarmClock.registered.values.toList() shouldBe
                listOf(RegisteredAlarm(start + 30.minutes, true), RegisteredAlarm(start + 90.minutes, true))
            store.alarms().map { it.toKey() } shouldBe listOf(prayer(30), prayer(90))

            val calls = alarmClock.setCalls to alarmClock.cancelCalls
            scheduler.replace(AlarmKind.PRAYER, listOf(prayer(90), prayer(30))).isEmpty shouldBe true
            (alarmClock.setCalls to alarmClock.cancelCalls) shouldBe calls
        }

    @Test
    fun `a moved alarm replaces the old one and an empty list cancels the kind`(): Unit =
        runTest {
            val reminder = AlarmKey(AlarmKind.REMINDER, 7, start + 10.minutes)
            scheduler.replace(AlarmKind.REMINDER, listOf(reminder))
            scheduler.replace(AlarmKind.PRAYER, listOf(prayer(30)))

            val plan = scheduler.replace(AlarmKind.PRAYER, listOf(prayer(45)))

            plan.cancel.map { it.toKey() } shouldBe listOf(prayer(30))
            alarmClock.registered.values.map { it.triggerAt } shouldContainExactlyInAnyOrder
                listOf(start + 10.minutes, start + 45.minutes)

            scheduler.replace(AlarmKind.PRAYER, emptyList())
            store.alarms().map { it.toKey() } shouldBe listOf(reminder)
            alarmClock.registered.values.map { it.triggerAt } shouldBe listOf(start + 10.minutes)
        }

    @Test
    fun `without the exact-alarm permission alarms are inexact and the banner state is raised`(): Unit =
        runTest {
            alarmClock.exactPermitted = false
            val denied = AlarmScheduler(store, alarmClock, clock)
            denied.exactAlarmStatus.value shouldBe ExactAlarmStatus.INEXACT
            denied.replace(AlarmKind.PRAYER, listOf(prayer(30)))
            alarmClock.registered.values
                .single()
                .exact shouldBe false

            alarmClock.exactPermitted = true
            denied.refreshExactAlarmStatus() shouldBe ExactAlarmStatus.EXACT
            denied.restore()
            alarmClock.registered.values
                .single()
                .exact shouldBe true
            denied.exactAlarmStatus.value shouldBe ExactAlarmStatus.EXACT

            alarmClock.exactCallFails = true
            denied.restore()
            denied.exactAlarmStatus.value shouldBe ExactAlarmStatus.INEXACT
        }

    @Test
    fun `restoring after a reboot registers stored alarms again and drops late ones`(): Unit =
        runTest {
            scheduler.replace(AlarmKind.PRAYER, listOf(prayer(10), prayer(60)))
            val reminder = AlarmKey(AlarmKind.REMINDER, 3, start + 40.minutes)
            scheduler.replace(AlarmKind.REMINDER, listOf(reminder))
            alarmClock.registered.clear()
            clock.advanceBy(30.minutes)

            val plan = scheduler.restore(setOf(AlarmKind.PRAYER))

            plan.cancel.map { it.toKey() } shouldBe listOf(prayer(10))
            alarmClock.registered.values.map { it.triggerAt } shouldBe listOf(start + 60.minutes)
            store.alarms().map { it.toKey() } shouldBe listOf(reminder, prayer(60))
        }

    @Test
    fun `fired alarms are delivered on time, skipped when late and kept when not yet due`(): Unit =
        runTest {
            scheduler.replace(AlarmKind.PRAYER, listOf(prayer(5), prayer(10), prayer(60)))
            val (late, onTime, early) = store.alarms()
            clock.advanceBy(25.minutes)

            scheduler.onFired(999) shouldBe null
            scheduler.onFired(late.id) shouldBe FiredAlarm(late, FireDecision.SKIP_LATE)
            scheduler.onFired(onTime.id) shouldBe FiredAlarm(onTime, FireDecision.DELIVER)
            val sets = alarmClock.setCalls
            scheduler.onFired(early.id) shouldBe FiredAlarm(early, FireDecision.NOT_DUE)

            alarmClock.setCalls shouldBe sets + 1
            store.alarms() shouldBe listOf(early)
        }

    @Test
    fun `alarm ids must fit a request code`(): Unit =
        runTest {
            val overflowing = AlarmScheduler(FakeAlarmStore(firstId = Int.MAX_VALUE + 1L), FakeAlarmClock(), clock)

            shouldThrow<IllegalArgumentException> { overflowing.replace(AlarmKind.PRAYER, listOf(prayer(5))) }
        }
}
