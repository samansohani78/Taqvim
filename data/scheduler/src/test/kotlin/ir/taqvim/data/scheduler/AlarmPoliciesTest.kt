/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-604 (U): the "skip if fired more than 15 minutes late" rule, deduplication and reconciliation plans. */
class AlarmPoliciesTest {
    private val now = Instant.parse("2026-09-13T12:00:00Z")
    private val reconciler = AlarmReconciler()

    private fun key(
        minutes: Int,
        kind: AlarmKind = AlarmKind.PRAYER,
        source: Long? = null,
    ) = AlarmKey(kind, source, now + minutes.minutes)

    private fun stored(
        id: Long,
        key: AlarmKey,
    ) = ScheduledAlarmEntity(id, key.kind, key.sourceId, key.triggerAt.toEpochMilliseconds())

    @Test
    fun `alarms fired more than fifteen minutes late are skipped`() {
        val policy = LatenessPolicy()

        policy.maxLateness shouldBe 15.minutes
        policy.decide(now, now) shouldBe FireDecision.DELIVER
        policy.decide(now - 15.minutes, now) shouldBe FireDecision.DELIVER
        policy.decide(now - 15.minutes - 1.milliseconds, now) shouldBe FireDecision.SKIP_LATE
        policy.decide(now + 1.milliseconds, now) shouldBe FireDecision.NOT_DUE
        LatenessPolicy(Duration.ZERO).decide(now - 1.milliseconds, now) shouldBe FireDecision.SKIP_LATE
        shouldThrow<IllegalArgumentException> { LatenessPolicy((-1).minutes) }
    }

    @Test
    fun `reconciling deduplicates, replaces moved alarms and leaves other kinds alone`() {
        val kept = stored(1, key(30))
        val duplicate = stored(2, key(30))
        val moved = stored(3, key(60))
        val reminder = stored(4, key(10, AlarmKind.REMINDER, source = 5))

        val plan =
            reconciler.reconcile(
                AlarmKind.PRAYER,
                listOf(kept, duplicate, moved, reminder),
                listOf(key(30), key(30), key(90)),
                now,
            )

        plan shouldBe ReconcilePlan(cancel = listOf(duplicate, moved), schedule = listOf(key(90)), keep = listOf(kept))
        plan.isEmpty shouldBe false
    }

    @Test
    fun `reconciling the stored state again changes nothing`() {
        val stored = listOf(stored(1, key(30)), stored(2, key(90)))

        val plan = reconciler.reconcile(AlarmKind.PRAYER, stored, listOf(key(90), key(30)), now)

        plan.isEmpty shouldBe true
        plan.keep shouldBe stored
    }

    @Test
    fun `desired alarms already too late are not scheduled and the rest are in trigger order`() {
        val plan = reconciler.reconcile(AlarmKind.PRAYER, emptyList(), listOf(key(20), key(-16), key(-15)), now)

        plan.schedule shouldBe listOf(key(-15), key(20))
        shouldThrow<IllegalArgumentException> {
            reconciler.reconcile(AlarmKind.PRAYER, emptyList(), listOf(key(5, AlarmKind.SHIFT)), now)
        }
    }

    @Test
    fun `restoring keeps due and future alarms of the chosen kinds and drops late ones and duplicates`() {
        val late = stored(1, key(-20))
        val due = stored(2, key(-10))
        val duplicate = stored(3, key(-10))
        val reminder = stored(4, key(5, AlarmKind.REMINDER, source = 9))
        val all = listOf(late, due, duplicate, reminder)

        reconciler.restore(setOf(AlarmKind.PRAYER), all, now) shouldBe
            ReconcilePlan(cancel = listOf(late, duplicate), schedule = emptyList(), keep = listOf(due))
        reconciler.restore(AlarmKind.entries.toSet(), all, now).keep shouldBe listOf(due, reminder)
    }

    @Test
    fun `reconciling keeps due alarms still within their lateness window, whose delivery may be under way`() {
        val inFlight = stored(1, key(-5))
        val late = stored(2, key(-20))
        val future = stored(3, key(30))

        val plan = reconciler.reconcile(AlarmKind.PRAYER, listOf(late, inFlight, future), listOf(key(60)), now)

        plan shouldBe ReconcilePlan(cancel = listOf(late, future), schedule = listOf(key(60)), keep = listOf(inFlight))
        reconciler.reconcile(AlarmKind.PRAYER, listOf(inFlight), listOf(key(-5)), now).isEmpty shouldBe true
    }

    @Test
    fun `failed deliveries are retried within the lateness window up to the attempt limit`() {
        val policy = RetryPolicy()
        val due = stored(1, key(0))

        policy.retryAt(due, now) shouldBe now + 1.minutes
        policy.retryAt(due.copy(attempts = 1), now) shouldBe now + 1.minutes
        policy.retryAt(due.copy(attempts = 2), now) shouldBe null
        policy.retryAt(due, now + 14.minutes) shouldBe now + 15.minutes
        policy.retryAt(due, now + 14.minutes + 1.milliseconds) shouldBe null
        policy.leaseUntil(now) shouldBe now + 1.minutes
        shouldThrow<IllegalArgumentException> { RetryPolicy(maxAttempts = 0) }
        shouldThrow<IllegalArgumentException> { RetryPolicy(retryDelay = Duration.ZERO) }
        shouldThrow<IllegalArgumentException> { RetryPolicy(lease = Duration.ZERO) }
    }

    @Test
    fun `stored alarms report their registration time and snoozed instant`() {
        val alarm = stored(1, key(5))

        alarm.registerAt() shouldBe now + 5.minutes
        alarm.snoozedFrom() shouldBe null
        val retried = alarm.copy(retryAtEpochMillis = (now + 6.minutes).toEpochMilliseconds())
        retried.registerAt() shouldBe now + 6.minutes
        alarm.copy(snoozedFromEpochMillis = now.toEpochMilliseconds()).snoozedFrom() shouldBe now
        AlarmKind.REMINDER.snoozeKind() shouldBe AlarmKind.REMINDER_SNOOZE
        AlarmKind.REMINDER.deliveryKind() shouldBe AlarmKind.REMINDER
    }
}
