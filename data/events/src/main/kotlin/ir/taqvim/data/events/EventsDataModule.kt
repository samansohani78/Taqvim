/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.data.devicecalendar.DeviceCalendarRepository
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin bindings of the events repository (ADR-0002). It needs `PersonalEventDao`, `IcsSubscriptionDao`,
 * `DeviceCalendarRepository`, `UserPreferencesRepository`, `kotlin.time.Clock` and the shared device time-zone stream
 * (qualified [DeviceTimeZone.QUALIFIER]) from other modules.
 */
val eventsDataModule =
    module {
        single<PersonalEventsSource> { RoomPersonalEventsSource(get()) }
        single<IcsEventsSource> { RoomIcsEventsSource(get()) }
        single<DeviceEventsSource> {
            val repository = get<DeviceCalendarRepository>()
            DeviceEventsSource { days -> repository.events(days) }
        }
        single {
            EventsRepository(
                // Sources chosen in the settings (T-1500) or the language default (ADR-0007 §3).
                settings = get<UserPreferencesRepository>().preferences.map { it.toEventsSettings() },
                inputs = EventInputs(get(), get(), get()),
                clock = get(),
                zones = get(named(DeviceTimeZone.QUALIFIER)),
            )
        }
    }
