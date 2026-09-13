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

    /** Stores [key] and returns the new row id. */
    suspend fun insert(key: AlarmKey): Long

    suspend fun delete(id: Long)
}

/** [AlarmStore] on the Room table of T-601. */
class RoomAlarmStore(
    private val dao: ReminderDao,
) : AlarmStore {
    override suspend fun alarms(): List<ScheduledAlarmEntity> = dao.alarms()

    override suspend fun insert(key: AlarmKey): Long =
        dao.insertAlarm(
            ScheduledAlarmEntity(
                kind = key.kind,
                sourceId = key.sourceId,
                triggerAtEpochMillis = key.triggerAt.toEpochMilliseconds(),
            ),
        )

    override suspend fun delete(id: Long) {
        dao.deleteAlarm(id)
    }
}

/** An alarm that the system fired and the lateness decision taken for it. */
data class FiredAlarm(
    val alarm: ScheduledAlarmEntity,
    val decision: FireDecision,
)

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
                plan.keep.forEach { register(it.id, it.toKey().triggerAt, exact) }
            }
        }

    /**
     * Handles the system firing alarm [id]. A delivered or skipped alarm is removed; an alarm that is not yet due stays
     * registered. Returns `null` when no such alarm is stored, e.g. because it was replaced meanwhile.
     */
    suspend fun onFired(id: Long): FiredAlarm? =
        mutex.withLock {
            store.alarms().firstOrNull { it.id == id }?.let { alarm ->
                val triggerAt = alarm.toKey().triggerAt
                val decision = lateness.decide(triggerAt, clock.now())
                if (decision == FireDecision.NOT_DUE) {
                    register(alarm.id, triggerAt, refreshStatus())
                } else {
                    store.delete(alarm.id)
                }
                FiredAlarm(alarm, decision)
            }
        }

    /** Re-reads the exact-alarm permission, e.g. when the user returns from the system settings. */
    fun refreshExactAlarmStatus(): ExactAlarmStatus = statusOf(refreshStatus())

    private suspend fun cancel(alarms: List<ScheduledAlarmEntity>) {
        alarms.forEach { alarm ->
            alarmClock.cancel(requestCode(alarm.id))
            store.delete(alarm.id)
        }
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
