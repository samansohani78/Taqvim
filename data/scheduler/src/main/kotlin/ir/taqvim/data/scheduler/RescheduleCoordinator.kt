/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** Produces the pending alarms of one [kind] (prayers, reminders, shifts); implemented by the feature that owns it. */
interface AlarmSource {
    val kind: AlarmKind

    /** Alarms that should be pending at [now]: all of [kind] and strictly after [now], so fired ones never repeat. */
    suspend fun upcomingAlarms(now: Instant): List<AlarmKey>
}

/** Shows or plays an alarm of [kind] that fired on time. */
interface AlarmDelivery {
    val kind: AlarmKind

    suspend fun deliver(alarm: ScheduledAlarmEntity)
}

/** Work handed over by the broadcast receivers; implemented by [RescheduleCoordinator]. */
interface SchedulerEvents {
    suspend fun handle(event: RescheduleEvent)

    suspend fun onAlarmFired(id: Long)
}

/**
 * Applies the reschedule matrix: it re-registers stored alarms and asks [sources] for fresh ones. A kind without any
 * source is left untouched, so alarms of a feature that is not wired in are neither lost nor added.
 */
class RescheduleCoordinator(
    private val scheduler: AlarmScheduler,
    private val sources: List<AlarmSource>,
    private val deliveries: List<AlarmDelivery>,
    private val clock: Clock,
    private val policy: ReschedulePolicy = ReschedulePolicy(),
) : SchedulerEvents {
    override suspend fun handle(event: RescheduleEvent) {
        val plan = policy.planFor(event)
        if (plan.restore.isNotEmpty()) scheduler.restore(plan.restore)
        plan.recompute.forEach { recompute(it) }
    }

    /** Delivers an alarm that fired on time, then schedules the next alarms of its kind unless it was not yet due. */
    override suspend fun onAlarmFired(id: Long) {
        val fired = scheduler.onFired(id) ?: return
        if (fired.decision == FireDecision.DELIVER) {
            deliveries.filter { it.kind == fired.alarm.kind }.forEach { it.deliver(fired.alarm) }
        }
        if (fired.decision != FireDecision.NOT_DUE) recompute(fired.alarm.kind)
    }

    /** Replaces the alarms of [kind] with what its sources want now; does nothing when [kind] has no source. */
    suspend fun recompute(kind: AlarmKind) {
        val owners = sources.filter { it.kind == kind }
        if (owners.isEmpty()) return
        val now = clock.now()
        scheduler.replace(kind, owners.flatMap { it.upcomingAlarms(now) })
    }
}

/** Turns preference updates into [RescheduleEvent.PreferencesChanged] when they move alarm times. */
class PreferenceChangeWatcher(
    private val preferences: Flow<UserPreferences>,
    private val events: SchedulerEvents,
    private val policy: ReschedulePolicy = ReschedulePolicy(),
) {
    /** Collects [preferences] until cancelled; the first value is the baseline and triggers nothing. */
    suspend fun watch() {
        var previous: UserPreferences? = null
        preferences.distinctUntilChanged().collect { current ->
            previous
                ?.takeIf { policy.affectedKinds(it, current).isNotEmpty() }
                ?.let { events.handle(RescheduleEvent.PreferencesChanged(it, current)) }
            previous = current
        }
    }
}

/**
 * Turns changes of the data that alarms of [kinds] are computed from (e.g. personal events and their reminders) into
 * [RescheduleEvent.AlarmInputsChanged]. Every value of [changes] announces a change; a burst of changes within
 * [quietPeriod], such as an event saved together with its reminders, recomputes once.
 */
class AlarmInputWatcher(
    private val changes: Flow<Any?>,
    private val kinds: Set<AlarmKind>,
    private val events: SchedulerEvents,
    private val quietPeriod: Duration = DEFAULT_QUIET_PERIOD,
) {
    /** Collects [changes] until cancelled. */
    @OptIn(FlowPreview::class)
    suspend fun watch() {
        changes.debounce(quietPeriod).collect { events.handle(RescheduleEvent.AlarmInputsChanged(kinds)) }
    }

    companion object {
        /** How long changes must pause before the alarms are recomputed. */
        val DEFAULT_QUIET_PERIOD: Duration = 2.seconds
    }
}
