/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.app.Application
import androidx.work.Configuration
import ir.taqvim.app.di.appModule
import ir.taqvim.data.events.ics.SubscriptionRefreshWorkerFactory
import ir.taqvim.data.scheduler.PreferenceChangeWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Process entry point: builds the Koin DI graph (see docs/adr/0002-dependency-injection-koin.md), starts the
 * scheduler's preference watcher (T-604) and provides WorkManager with the subscription worker factory (T-1003).
 */
class TaqvimApplication :
    Application(),
    Configuration.Provider {
    /** Work that lasts as long as the process. */
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(get<SubscriptionRefreshWorkerFactory>()).build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TaqvimApplication)
            modules(appModule)
        }
        val watcher = get<PreferenceChangeWatcher>()
        processScope.launch { watcher.watch() }
    }
}
