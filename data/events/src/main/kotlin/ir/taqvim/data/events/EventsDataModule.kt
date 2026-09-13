/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.data.devicecalendar.DeviceCalendarRepository
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

/**
 * Koin bindings of the events repository (ADR-0002). It needs `PersonalEventDao`, `IcsSubscriptionDao`,
 * `DeviceCalendarRepository`, `UserPreferencesRepository` and `kotlin.time.Clock` from other modules.
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
                settings = get<UserPreferencesRepository>().preferences.map { it.toEventsSettings() },
                inputs = EventInputs(get(), get(), get()),
                clock = get(),
            )
        }
    }
