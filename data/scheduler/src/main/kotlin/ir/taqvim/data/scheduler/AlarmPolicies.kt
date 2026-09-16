/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Identity of an alarm. Requests with equal keys are one alarm: the scheduler deduplicates by key, so asking for the
 * same alarm again changes nothing.
 */
data class AlarmKey(
    val kind: AlarmKind,
    /** Row id of the reminder or shift rotation, or `null` for prayer alarms (see [ScheduledAlarmEntity.sourceId]). */
    val sourceId: Long?,
    val triggerAt: Instant,
)

/** The [AlarmKey] of a stored alarm. */
fun ScheduledAlarmEntity.toKey(): AlarmKey =
    AlarmKey(kind, sourceId, Instant.fromEpochMilliseconds(triggerAtEpochMillis))

/** When the system alarm of this row fires: its retry or lease time when set, otherwise its trigger time. */
fun ScheduledAlarmEntity.registerAt(): Instant =
    Instant.fromEpochMilliseconds(retryAtEpochMillis ?: triggerAtEpochMillis)

/** The planned instant whose reminder or athan a snooze shows again, or `null` for other alarms. */
fun ScheduledAlarmEntity.snoozedFrom(): Instant? = snoozedFromEpochMillis?.let(Instant::fromEpochMilliseconds)

/** The kind of the snoozes of this kind (reminders and prayers can be snoozed), or `null`. */
fun AlarmKind.snoozeKind(): AlarmKind? =
    when (this) {
        AlarmKind.REMINDER -> AlarmKind.REMINDER_SNOOZE
        AlarmKind.PRAYER -> AlarmKind.PRAYER_SNOOZE
        else -> null
    }

/** The kind whose delivery shows alarms of this kind: a snooze is delivered like the alarm it snoozes. */
fun AlarmKind.deliveryKind(): AlarmKind = AlarmKind.entries.firstOrNull { it.snoozeKind() == this } ?: this

/** What delivering a fired alarm achieved (ADR-0033). */
enum class DeliveryOutcome {
    /** Shown or started; the alarm is done. */
    DELIVERED,

    /** Nothing to show any more (changed meanwhile, or already shown); the alarm is done. */
    SKIPPED,

    /** Showing or starting failed; the alarm stays pending and is retried per [RetryPolicy]. */
    FAILED,
}

/** The outcome of several deliveries of one alarm: any failure retries it, any success completes it. */
fun Collection<DeliveryOutcome>.combined(): DeliveryOutcome =
    when {
        DeliveryOutcome.FAILED in this -> DeliveryOutcome.FAILED
        DeliveryOutcome.DELIVERED in this -> DeliveryOutcome.DELIVERED
        else -> DeliveryOutcome.SKIPPED
    }

/**
 * When a pending alarm fires again (ADR-0033). A fired alarm is leased for [lease] before delivery starts, so it fires
 * again if the process dies before the outcome is recorded; a failed delivery is retried after [retryDelay] up to
 * [maxAttempts] failures. Neither may fire later than the lateness allowed for the planned trigger time.
 */
class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val retryDelay: Duration = DEFAULT_RETRY_DELAY,
    val lease: Duration = DEFAULT_LEASE,
    private val lateness: LatenessPolicy = LatenessPolicy(),
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive (got $maxAttempts)" }
        require(retryDelay.isPositive() && lease.isPositive()) { "retryDelay and lease must be positive" }
    }

    /** When the alarm fires again after its delivery failed at [now], or `null` when it is given up. */
    fun retryAt(
        alarm: ScheduledAlarmEntity,
        now: Instant,
    ): Instant? = (now + retryDelay).takeIf { alarm.attempts + 1 < maxAttempts && inWindow(alarm, it) }

    /** When the alarm fires again if its delivery starting at [now] never reports back. */
    fun leaseUntil(now: Instant): Instant = now + lease

    private fun inWindow(
        alarm: ScheduledAlarmEntity,
        at: Instant,
    ): Boolean = lateness.decide(alarm.toKey().triggerAt, at) == FireDecision.DELIVER

    companion object {
        /** Failed deliveries before an alarm is given up. */
        const val DEFAULT_MAX_ATTEMPTS: Int = 3

        val DEFAULT_RETRY_DELAY: Duration = 1.minutes
        val DEFAULT_LEASE: Duration = 1.minutes
    }
}

/** What to do with an alarm that the system has just fired. */
enum class FireDecision {
    /** On time, or late by at most the allowed lateness: deliver it. */
    DELIVER,

    /** Later than the allowed lateness (device off, doze, clock change): drop it without delivering. */
    SKIP_LATE,

    /** Fired before its trigger time (e.g. after a clock change): keep it registered. */
    NOT_DUE,
}

/** The "skip if fired more than 15 minutes late" rule (docs/PLAN.md T-604); exactly [maxLateness] late is on time. */
class LatenessPolicy(
    val maxLateness: Duration = DEFAULT_MAX_LATENESS,
) {
    init {
        require(!maxLateness.isNegative()) { "maxLateness must not be negative (got $maxLateness)" }
    }

    /** Decision for an alarm due at [triggerAt] that fires at [now]. */
    fun decide(
        triggerAt: Instant,
        now: Instant,
    ): FireDecision =
        when {
            now < triggerAt -> FireDecision.NOT_DUE
            now - triggerAt > maxLateness -> FireDecision.SKIP_LATE
            else -> FireDecision.DELIVER
        }

    companion object {
        /** Lateness allowed by the plan. */
        val DEFAULT_MAX_LATENESS: Duration = 15.minutes
    }
}

/** Changes that bring the stored alarms to a wanted state. */
data class ReconcilePlan(
    /** Stored alarms to cancel with the system and delete. */
    val cancel: List<ScheduledAlarmEntity>,
    /** New alarms to store and register, in trigger order. */
    val schedule: List<AlarmKey>,
    /** Stored alarms that stay. */
    val keep: List<ScheduledAlarmEntity>,
) {
    /** Whether applying the plan changes nothing. */
    val isEmpty: Boolean
        get() = cancel.isEmpty() && schedule.isEmpty()
}

/** Pure planning of alarm changes: the one place that decides deduplication and the dropping of late alarms. */
class AlarmReconciler(
    private val lateness: LatenessPolicy = LatenessPolicy(),
) {
    /**
     * Plan that makes the stored alarms of [kind] exactly [desired]. Duplicates in either list collapse to one alarm,
     * alarms that would already be skipped as late are not scheduled, and alarms of other kinds are untouched.
     */
    fun reconcile(
        kind: AlarmKind,
        stored: List<ScheduledAlarmEntity>,
        desired: Collection<AlarmKey>,
        now: Instant,
    ): ReconcilePlan {
        require(desired.all { it.kind == kind }) { "every desired alarm must be of kind $kind" }
        val wanted = desired.filterTo(LinkedHashSet()) { !isLate(it, now) }
        val (inFlight, others) = stored.filter { it.kind == kind }.partition { isInFlight(it, now) }
        val (keep, cancel) = firstPerKey(others) { it.toKey() in wanted }
        val kept = (keep + inFlight).mapTo(HashSet()) { it.toKey() }
        return ReconcilePlan(cancel, wanted.filterNot { it in kept }.sortedBy { it.triggerAt }, keep + inFlight)
    }

    /**
     * Plan after the system lost its alarms (reboot, app update) or the exact-alarm permission changed: the stored
     * alarms of [kinds] are registered again, except duplicates and alarms already later than the allowed lateness.
     */
    fun restore(
        kinds: Set<AlarmKind>,
        stored: List<ScheduledAlarmEntity>,
        now: Instant,
    ): ReconcilePlan {
        val (keep, cancel) = firstPerKey(stored.filter { it.kind in kinds }) { !isLate(it.toKey(), now) }
        return ReconcilePlan(cancel = cancel, schedule = emptyList(), keep = keep)
    }

    private fun isLate(
        key: AlarmKey,
        now: Instant,
    ): Boolean = lateness.decide(key.triggerAt, now) == FireDecision.SKIP_LATE

    /** A due alarm still within its lateness window: its delivery may be under way, so it is never replaced. */
    private fun isInFlight(
        alarm: ScheduledAlarmEntity,
        now: Instant,
    ): Boolean = lateness.decide(alarm.toKey().triggerAt, now) == FireDecision.DELIVER

    /** Splits [alarms] into the first retained alarm of each key and everything else, keeping their order. */
    private fun firstPerKey(
        alarms: List<ScheduledAlarmEntity>,
        retain: (ScheduledAlarmEntity) -> Boolean,
    ): Pair<List<ScheduledAlarmEntity>, List<ScheduledAlarmEntity>> {
        val seen = HashSet<AlarmKey>()
        return alarms.partition { retain(it) && seen.add(it.toKey()) }
    }
}
