/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ReminderDao
import ir.taqvim.data.database.ScheduledAlarmEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Whether alarms can fire at their exact time; [INEXACT] means the app should show the exact-alarm banner. */
enum class ExactAlarmStatus {
    EXACT,
    INEXACT,
}

/** The system alarm service, reduced to what the scheduler needs. */
interface AlarmClock {
    /** Whether exact alarms are permitted right now. */
    fun canScheduleExact(): Boolean

    /**
     * Registers alarm [requestCode] at [triggerAt], replacing any alarm with the same request code. It is exact only
     * when [exact] is requested and permitted; returns whether it was set exact.
     */
    fun set(
        requestCode: Int,
        triggerAt: Instant,
        exact: Boolean,
    ): Boolean

    /** Removes alarm [requestCode]; nothing happens when it is not registered. */
    fun cancel(requestCode: Int)
}

/** Persistent record of the alarms registered with the system (`scheduled_alarms`). */
interface AlarmStore {
    /** Every stored alarm in trigger order. */
    suspend fun alarms(): List<ScheduledAlarmEntity>

    /** Stores [key] (a snooze of [snoozedFrom] when set) and returns the new row id. */
    suspend fun insert(
        key: AlarmKey,
        snoozedFrom: Instant? = null,
    ): Long

    suspend fun delete(id: Long)

    /** Records that alarm [id] has failed [attempts] times, or is leased, and next fires at [retryAt]. */
    suspend fun updateAttempts(
        id: Long,
        attempts: Int,
        retryAt: Instant,
    )
}

/** [AlarmStore] on the Room table of T-601. */
class RoomAlarmStore(
    private val dao: ReminderDao,
) : AlarmStore {
    override suspend fun alarms(): List<ScheduledAlarmEntity> = dao.alarms()

    override suspend fun insert(
        key: AlarmKey,
        snoozedFrom: Instant?,
    ): Long =
        dao.insertAlarm(
            ScheduledAlarmEntity(
                kind = key.kind,
                sourceId = key.sourceId,
                triggerAtEpochMillis = key.triggerAt.toEpochMilliseconds(),
                snoozedFromEpochMillis = snoozedFrom?.toEpochMilliseconds(),
            ),
        )

    override suspend fun delete(id: Long) {
        dao.deleteAlarm(id)
    }

    override suspend fun updateAttempts(
        id: Long,
        attempts: Int,
        retryAt: Instant,
    ) {
        dao.updateAttempts(id, attempts, retryAt.toEpochMilliseconds())
    }
}

/** An alarm that the system fired and the lateness decision taken for it. */
data class FiredAlarm(
    val alarm: ScheduledAlarmEntity,
    val decision: FireDecision,
)

/** What became of a pending alarm after its delivery reported back (ADR-0033). */
enum class Completion {
    /** Delivered or skipped: the alarm is removed. */
    DONE,

    /** Delivery failed and the alarm fires again later. */
    RETRYING,

    /** Delivery failed too often or too late: the alarm is removed without being shown. */
    GAVE_UP,
}

/**
 * Single source of truth for alarms (T-604). The `scheduled_alarms` table mirrors what is registered with the system,
 * and every change goes through this class, serialised by a mutex, so repeating a request is idempotent. Alarm row ids
 * double as request codes.
 */
class AlarmScheduler(
    private val store: AlarmStore,
    private val alarmClock: AlarmClock,
    private val clock: Clock,
    private val lateness: LatenessPolicy = LatenessPolicy(),
    private val retry: RetryPolicy = RetryPolicy(lateness = lateness),
) {
    private val reconciler = AlarmReconciler(lateness)
    private val mutex = Mutex()
    private val status = MutableStateFlow(statusOf(alarmClock.canScheduleExact()))

    /** Current exact-alarm availability, for the "exact alarms unavailable" banner. */
    val exactAlarmStatus: StateFlow<ExactAlarmStatus> = status.asStateFlow()

    /** Makes the alarms of [kind] exactly [desired] (deduplicated by key); an empty list cancels them all. */
    suspend fun replace(
        kind: AlarmKind,
        desired: Collection<AlarmKey>,
    ): ReconcilePlan =
        mutex.withLock {
            reconciler.reconcile(kind, store.alarms(), desired, clock.now()).also { plan ->
                cancel(plan.cancel)
                if (plan.schedule.isNotEmpty()) {
                    val exact = refreshStatus()
                    plan.schedule.forEach { key -> register(store.insert(key), key.triggerAt, exact) }
                }
            }
        }

    /** Registers the stored alarms of [kinds] with the system again, after a reboot, update or permission change. */
    suspend fun restore(kinds: Set<AlarmKind> = AlarmKind.entries.toSet()): ReconcilePlan =
        mutex.withLock {
            reconciler.restore(kinds, store.alarms(), clock.now()).also { plan ->
                cancel(plan.cancel)
                val exact = refreshStatus()
                plan.keep.forEach { register(it.id, it.registerAt(), exact) }
            }
        }

    /**
     * Handles the system firing alarm [id]. An alarm to deliver stays pending and is leased: it fires again unless
     * [complete] reports its delivery first. A late alarm is removed; one that is not yet due stays registered.
     * Returns `null` when no such alarm is stored, e.g. because it was replaced or completed meanwhile.
     */
    suspend fun onFired(id: Long): FiredAlarm? =
        mutex.withLock {
            store.alarms().firstOrNull { it.id == id }?.let { alarm ->
                val now = clock.now()
                val decision = lateness.decide(alarm.toKey().triggerAt, now)
                when (decision) {
                    FireDecision.NOT_DUE -> register(alarm.id, alarm.registerAt(), refreshStatus())
                    FireDecision.SKIP_LATE -> store.delete(alarm.id)
                    FireDecision.DELIVER -> lease(alarm, retry.leaseUntil(now))
                }
                FiredAlarm(alarm, decision)
            }
        }

    /**
     * Records the [outcome] of delivering pending alarm [id]: delivered or skipped alarms are removed, failed ones fire
     * again per [RetryPolicy] or are given up. Returns `null` when the alarm is no longer stored.
     */
    suspend fun complete(
        id: Long,
        outcome: DeliveryOutcome,
    ): Completion? =
        mutex.withLock {
            store.alarms().firstOrNull { it.id == id }?.let { alarm ->
                val retryAt = retry.retryAt(alarm, clock.now()).takeIf { outcome == DeliveryOutcome.FAILED }
                when {
                    retryAt != null -> {
                        store.updateAttempts(alarm.id, alarm.attempts + 1, retryAt)
                        register(alarm.id, retryAt, refreshStatus())
                        Completion.RETRYING
                    }

                    else -> {
                        cancel(listOf(alarm))
                        if (outcome == DeliveryOutcome.FAILED) Completion.GAVE_UP else Completion.DONE
                    }
                }
            }
        }

    /**
     * Schedules a snooze of kind [kind] (see [snoozeKind]) that shows the alarm of [sourceId] planned at [snoozedFrom]
     * again at [at], replacing an earlier snooze of the same alarm. Returns the stored alarm.
     */
    suspend fun snooze(
        kind: AlarmKind,
        sourceId: Long?,
        snoozedFrom: Instant,
        at: Instant,
    ): ScheduledAlarmEntity =
        mutex.withLock {
            require(AlarmKind.entries.any { it.snoozeKind() == kind }) { "$kind is not a snooze kind" }
            cancel(
                store.alarms().filter { it.kind == kind && it.sourceId == sourceId && it.snoozedFrom() == snoozedFrom },
            )
            val key = AlarmKey(kind, sourceId, at)
            val id = store.insert(key, snoozedFrom)
            register(id, at, refreshStatus())
            store.alarms().first { it.id == id }
        }

    /** Removes the alarms of [kind] that [keep] rejects, e.g. snoozes of reminders whose event was deleted. */
    suspend fun prune(
        kind: AlarmKind,
        keep: suspend (ScheduledAlarmEntity) -> Boolean,
    ): List<ScheduledAlarmEntity> =
        mutex.withLock {
            store.alarms().filter { it.kind == kind && !keep(it) }.also { cancel(it) }
        }

    /** Re-reads the exact-alarm permission, e.g. when the user returns from the system settings. */
    fun refreshExactAlarmStatus(): ExactAlarmStatus = statusOf(refreshStatus())

    private suspend fun cancel(alarms: List<ScheduledAlarmEntity>) {
        alarms.forEach { alarm ->
            alarmClock.cancel(requestCode(alarm.id))
            store.delete(alarm.id)
        }
    }

    private suspend fun lease(
        alarm: ScheduledAlarmEntity,
        until: Instant,
    ) {
        store.updateAttempts(alarm.id, alarm.attempts, until)
        register(alarm.id, until, refreshStatus())
    }

    private fun refreshStatus(): Boolean = alarmClock.canScheduleExact().also { status.value = statusOf(it) }

    private fun register(
        id: Long,
        triggerAt: Instant,
        exact: Boolean,
    ) {
        val setExact = alarmClock.set(requestCode(id), triggerAt, exact)
        if (exact && !setExact) status.value = ExactAlarmStatus.INEXACT
    }

    private companion object {
        fun statusOf(exact: Boolean): ExactAlarmStatus = if (exact) ExactAlarmStatus.EXACT else ExactAlarmStatus.INEXACT

        fun requestCode(id: Long): Int {
            require(id in 1L..Int.MAX_VALUE.toLong()) { "alarm id $id cannot be used as a request code" }
            return id.toInt()
        }
    }
}
