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
        val (keep, cancel) = firstPerKey(stored.filter { it.kind == kind }) { it.toKey() in wanted }
        val kept = keep.mapTo(HashSet()) { it.toKey() }
        return ReconcilePlan(cancel, wanted.filterNot { it in kept }.sortedBy { it.triggerAt }, keep)
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

    /** Splits [alarms] into the first retained alarm of each key and everything else, keeping their order. */
    private fun firstPerKey(
        alarms: List<ScheduledAlarmEntity>,
        retain: (ScheduledAlarmEntity) -> Boolean,
    ): Pair<List<ScheduledAlarmEntity>, List<ScheduledAlarmEntity>> {
        val seen = HashSet<AlarmKey>()
        return alarms.partition { retain(it) && seen.add(it.toKey()) }
    }
}
