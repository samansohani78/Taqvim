/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin bindings of the athan (T-1102). `:app` provides [AthanSetupSource] (and optionally an [AthanEventHook], T-1103)
 * and adapts [AthanAlarms] to the scheduler's prayer `AlarmSource` and `AlarmDelivery`.
 */
val notificationFeatureModule: Module =
    module {
        single<AthanDeliveryLog> { SharedPreferencesAthanDeliveryLog(androidContext()) }
        single<AthanPlaybackStarter> { ServiceAthanPlaybackStarter(androidContext()) }
        single { AthanAlarms(get(), get(), get(), getOrNull() ?: AthanEventHook.NONE) }
    }
