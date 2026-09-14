/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin bindings of the athan (T-1102) and of reminders (T-1001, T-1002). `:app` provides [AthanSetupSource] (and
 * optionally an [AthanEventHook], T-1103) and [ReminderSetupSource], and adapts [AthanAlarms] and [ReminderAlarms] to
 * the scheduler's `PRAYER` and `REMINDER` `AlarmSource` and `AlarmDelivery`.
 */
val notificationFeatureModule: Module =
    module {
        single<AthanDeliveryLog> { SharedPreferencesAthanDeliveryLog(androidContext()) }
        single<AthanPlaybackStarter> { ServiceAthanPlaybackStarter(androidContext()) }
        single { AthanAlarms(get(), get(), get(), getOrNull() ?: AthanEventHook.NONE) }
        single<ReminderDeliveryLog> { SharedPreferencesReminderDeliveryLog(androidContext()) }
        single<ReminderNotifier> { SystemReminderNotifier(androidContext()) }
        single { ReminderAlarms(get(), get(), get()) }
    }
