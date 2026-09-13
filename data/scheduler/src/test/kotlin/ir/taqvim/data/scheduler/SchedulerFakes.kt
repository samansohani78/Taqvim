/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Instant

/** An alarm as the fake system holds it. */
internal data class RegisteredAlarm(
    val triggerAt: Instant,
    val exact: Boolean,
)

/** In-memory `AlarmManager`: one alarm per request code, with a switchable exact-alarm permission. */
internal class FakeAlarmClock(
    var exactPermitted: Boolean = true,
) : AlarmClock {
    /** Simulates the permission being revoked between the check and the call. */
    var exactCallFails: Boolean = false
    val registered = LinkedHashMap<Int, RegisteredAlarm>()
    var setCalls: Int = 0
    var cancelCalls: Int = 0

    override fun canScheduleExact(): Boolean = exactPermitted

    override fun set(
        requestCode: Int,
        triggerAt: Instant,
        exact: Boolean,
    ): Boolean {
        setCalls++
        val setExact = exact && exactPermitted && !exactCallFails
        registered[requestCode] = RegisteredAlarm(triggerAt, setExact)
        return setExact
    }

    override fun cancel(requestCode: Int) {
        cancelCalls++
        registered.remove(requestCode)
    }
}

/** In-memory `scheduled_alarms` table with ids starting at [firstId]. */
internal class FakeAlarmStore(
    firstId: Long = 1L,
) : AlarmStore {
    private val rows = mutableListOf<ScheduledAlarmEntity>()
    private var nextId = firstId

    override suspend fun alarms(): List<ScheduledAlarmEntity> =
        rows.sortedWith(compareBy({ it.triggerAtEpochMillis }, { it.id }))

    override suspend fun insert(key: AlarmKey): Long {
        val id = nextId++
        rows += ScheduledAlarmEntity(id, key.kind, key.sourceId, key.triggerAt.toEpochMilliseconds())
        return id
    }

    override suspend fun delete(id: Long) {
        rows.removeAll { it.id == id }
    }
}

/** A source with fixed alarm [times]; only the times after `now` are pending. */
internal class FakeSource(
    override val kind: AlarmKind,
    private val times: List<Instant>,
) : AlarmSource {
    private val queryCount = AtomicInteger()

    val queries: Int
        get() = queryCount.get()

    override suspend fun upcomingAlarms(now: Instant): List<AlarmKey> {
        queryCount.incrementAndGet()
        return times.filter { it > now }.map { AlarmKey(kind, null, it) }
    }
}

/** Records delivered alarms. */
internal class FakeDelivery(
    override val kind: AlarmKind,
) : AlarmDelivery {
    val delivered = mutableListOf<ScheduledAlarmEntity>()

    override suspend fun deliver(alarm: ScheduledAlarmEntity) {
        delivered += alarm
    }
}

/** Records the events handed over by receivers or watchers. */
internal class RecordingEvents : SchedulerEvents {
    val events = mutableListOf<RescheduleEvent>()
    val fired = mutableListOf<Long>()

    override suspend fun handle(event: RescheduleEvent) {
        events += event
    }

    override suspend fun onAlarmFired(id: Long) {
        fired += id
    }
}
