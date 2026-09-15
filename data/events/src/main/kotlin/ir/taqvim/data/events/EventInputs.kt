/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.IcsSubscriptionDao
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.toRule
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.devicecalendar.InstantWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A personal event with its [recurrence] rule (`null` for a one-off event), the [exceptions] days on which it does not
 * occur and its [overrides] replacing single occurrences (T-1003).
 */
data class PersonalEventRecord(
    val event: PersonalEventEntity,
    val recurrence: RecurrenceRule?,
    val exceptions: Set<Jdn> = emptySet(),
    val overrides: List<EventOverrideEntity> = emptyList(),
)

/** Personal events overlapping some days, plus recurring events starting before their end. */
fun interface PersonalEventsSource {
    fun events(days: JdnRange): Flow<List<PersonalEventRecord>>
}

/** Device calendar instances touching some days (T-602). */
fun interface DeviceEventsSource {
    fun events(days: JdnRange): Flow<List<DeviceEvent>>
}

/** Cached occurrences of enabled iCalendar subscriptions overlapping a window. */
fun interface IcsEventsSource {
    fun events(window: InstantWindow): Flow<List<IcsEventCacheEntity>>
}

/** The inputs of [EventsRepository] besides the dataset: personal, device and iCalendar events. */
data class EventInputs(
    val personal: PersonalEventsSource,
    val device: DeviceEventsSource,
    val ics: IcsEventsSource,
)

/**
 * [PersonalEventsSource] over the `personal_events` and `event_recurrences` tables (T-601) and the exception days and
 * overridden occurrences of recurring events (T-1003).
 */
class RoomPersonalEventsSource(
    private val dao: PersonalEventDao,
) : PersonalEventsSource {
    override fun events(days: JdnRange): Flow<List<PersonalEventRecord>> =
        dao.observeInRange(days.start.value, days.endInclusive.value).map { events ->
            events.map {
                PersonalEventRecord(
                    event = it,
                    recurrence = dao.getRecurrence(it.id)?.toRule(),
                    exceptions = dao.exceptionDays(it.id).map(::Jdn).toSet(),
                    overrides = dao.overrides(it.id),
                )
            }
        }
}

/** [IcsEventsSource] over the `ics_events_cache` table (T-601). */
class RoomIcsEventsSource(
    private val dao: IcsSubscriptionDao,
) : IcsEventsSource {
    override fun events(window: InstantWindow): Flow<List<IcsEventCacheEntity>> =
        dao.observeEvents(window.fromEpochMillis, window.toEpochMillis)
}
