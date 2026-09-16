/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.devicecalendar.DeviceEventMapping
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * The events of days (T-305): the generated dataset (D-08) filtered by the visibility and holiday policies of
 * [settings], combined with personal, device and iCalendar events from [inputs]. Every flow re-emits when the settings
 * or any input change; dataset lookups are rebuilt only when the settings value changes.
 */
class EventsRepository(
    private val settings: Flow<EventsSettings>,
    private val inputs: EventInputs,
    private val clock: Clock,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val catalog: OfficialCatalog = OfficialCatalog(),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * One [DayEvents] per day of [days], in order. Timed events are dated in the zone read when collection starts (the
     * [EventDays] rule), so every source is read one day wider than [days]; an expiring Hijri offset takes effect at
     * the next emission.
     */
    fun days(days: JdnRange): Flow<List<DayEvents>> {
        require(!days.isEmpty()) { "days must not be empty" }
        return flow {
            val zone = zone()
            val assembler = DayEventsAssembler(clock, zone)
            val official = settings.distinctUntilChanged().map { OfficialView(catalog, it) }
            val snapshots =
                combine(
                    official,
                    inputs.personal.events(EventDays.widened(days)),
                    inputs.device.events(days),
                    inputs.ics.events(DeviceEventMapping.window(days, zone)),
                ) { view, personal, device, ics -> Snapshot(view, personal, device, ics) }
            emitAll(snapshots.map { assembler.assemble(days, it) })
        }.distinctUntilChanged().flowOn(computeDispatcher)
    }

    /** The events of the single day [jdn]. */
    fun day(jdn: Jdn): Flow<DayEvents> = days(jdn..jdn).map { it.single() }
}
