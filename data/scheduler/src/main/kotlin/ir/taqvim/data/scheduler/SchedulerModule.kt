/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin bindings of the scheduler (ADR-0002). It needs `ReminderDao` and `kotlin.time.Clock` from other modules;
 * features contribute [AlarmSource] and [AlarmDelivery] bindings, which are collected with `getAll`.
 */
val schedulerModule =
    module {
        single<AlarmClock> { SystemAlarmClock(androidContext()) }
        single<AlarmStore> { RoomAlarmStore(get()) }
        single { AlarmScheduler(get(), get(), get()) }
        single { RescheduleCoordinator(get(), getAll(), getAll(), get()) } bind SchedulerEvents::class
    }
