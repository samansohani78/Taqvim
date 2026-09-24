/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.devicecalendar.DeviceEventMapping
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * The events of days (T-305): the generated dataset (D-08) filtered by the visibility and holiday policies of
 * [settings], combined with personal, device and iCalendar events from [inputs]. Every flow re-emits when the settings,
 * any input or the device zone ([zones], review I06) change; dataset lookups are rebuilt only when the settings value changes.
 */
class EventsRepository(
    private val settings: Flow<EventsSettings>,
    private val inputs: EventInputs,
    private val clock: Clock,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
    private val catalog: OfficialCatalog = OfficialCatalog(),
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val shared = AtomicReference<OfficialView?>(null)

    /**
     * One [DayEvents] per day of [days], in order. Timed events are dated in the current device zone (the [EventDays]
     * rule), so every source is read one day wider than [days]. A zone change re-reads only the zone-dependent sources;
     * the settings stream stays subscribed. An expiring Hijri offset takes effect at the next emission.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun days(days: JdnRange): Flow<List<DayEvents>> {
        require(!days.isEmpty()) { "days must not be empty" }
        val official = settings.distinctUntilChanged().map { viewFor(it) }
        val sourced = zones.distinctUntilChanged().flatMapLatest { zone -> sourcesIn(days, zone) }
        return combine(official, sourced) { view, zoned ->
            zoned.assembler.assemble(days, Snapshot(view, zoned.personal, zoned.device, zoned.ics))
        }.distinctUntilChanged()
            .flowOn(computeDispatcher)
    }

    /**
     * The lookups for [settings], shared by every collector rather than built per flow.
     *
     * The calendar screen collects four of these flows at once — the pager's three pages and the day-details pane —
     * and rebuilds them on every swipe, so a per-flow view meant each swipe threw away four freshly built event
     * lookups with their caches and recomputed every year index from the ~300 dataset definitions. The view derives
     * only from the immutable [EventsSettings], so any instance will do; publication is lock-free and a race simply
     * builds one spare view rather than making a collector wait for another's construction.
     */
    private fun viewFor(settings: EventsSettings): OfficialView {
        shared.get()?.takeIf { it.settings == settings }?.let { return it }
        val built = OfficialView(catalog, settings).also { it.warmUp() }
        val current = shared.get()
        return if (current != null && current.settings == settings) {
            current
        } else {
            shared.set(built)
            built
        }
    }

    private fun sourcesIn(
        days: JdnRange,
        zone: TimeZone,
    ): Flow<ZonedSources> {
        val assembler = DayEventsAssembler(clock, zone)
        return combine(
            inputs.personal.events(EventDays.widened(days)),
            inputs.device.events(days),
            inputs.ics.events(DeviceEventMapping.window(days, zone)),
        ) { personal, device, ics -> ZonedSources(assembler, personal, device, ics) }
    }

    /** The events of the single day [jdn]. */
    fun day(jdn: Jdn): Flow<DayEvents> = days(jdn..jdn).map { it.single() }
}

/** The zone-dependent inputs read for one device zone, with the assembler dating them in that zone. */
private class ZonedSources(
    val assembler: DayEventsAssembler,
    val personal: List<PersonalEventRecord>,
    val device: List<DeviceEvent>,
    val ics: List<IcsEventCacheEntity>,
)
