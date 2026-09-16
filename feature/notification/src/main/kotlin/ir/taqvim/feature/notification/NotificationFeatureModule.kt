/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin bindings of the athan (T-1102), of reminders (T-1001, T-1002) and of the surfaces outside the app (T-1213,
 * T-1215). `:app` provides [AthanSetupSource] (and optionally an [AthanEventHook], T-1103), [ReminderSetupSource],
 * [TodaySummarySource], [PersistentNotificationOptionsSource] and the [SnoozeScheduler] (ADR-0033), adapts [AthanAlarms] and [ReminderAlarms] to the
 * scheduler's `PRAYER` and `REMINDER` `AlarmSource` and `AlarmDelivery`, and may bind more [DailyRefresh]es.
 */
val notificationFeatureModule: Module =
    module {
        single<DeliveryLog>(named(ATHAN_DELIVERIES)) {
            SharedPreferencesDeliveryLog(
                androidContext(),
                SharedPreferencesDeliveryLog.ATHAN_FILE,
                SharedPreferencesDeliveryLog.ATHAN_CAPACITY,
            )
        }
        single<AthanPlaybackStarter> { ServiceAthanPlaybackStarter(androidContext()) }
        single { AthanAlarms(get(), get(named(ATHAN_DELIVERIES)), get(), getOrNull() ?: AthanEventHook.NONE) }
        single<DeliveryLog>(named(REMINDER_DELIVERIES)) {
            SharedPreferencesDeliveryLog(
                androidContext(),
                SharedPreferencesDeliveryLog.REMINDER_FILE,
                SharedPreferencesDeliveryLog.REMINDER_CAPACITY,
            )
        }
        single<ReminderNotifier> { SystemReminderNotifier(androidContext()) }
        single { ReminderAlarms(get(), get(named(REMINDER_DELIVERIES)), get()) }
        single { DayIconCache() }
        single<PostedNotificationStore> { SharedPreferencesPostedNotificationStore(androidContext()) }
        single<DailyRefreshScheduler> { AlarmDailyRefreshScheduler(androidContext()) }
        single { PersistentNotificationRefresh(androidContext(), get(), get(), get(), get()) } bind DailyRefresh::class
        single { DailyRefreshCoordinator(getAll(), get()) }
    }

/** Qualifiers of the two [DeliveryLog]s. */
const val ATHAN_DELIVERIES: String = "athan-deliveries"
const val REMINDER_DELIVERIES: String = "reminder-deliveries"
