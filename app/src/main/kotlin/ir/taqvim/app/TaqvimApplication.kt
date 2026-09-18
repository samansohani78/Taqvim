/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.app.Application
import androidx.work.Configuration
import ir.taqvim.app.automation.AppShortcuts
import ir.taqvim.app.automation.LauncherIconSwitcher
import ir.taqvim.app.di.AppLanguageSync
import ir.taqvim.app.di.RestoreRecovery
import ir.taqvim.app.di.SurfaceRefreshWatcher
import ir.taqvim.app.di.WidgetTriggerWatcher
import ir.taqvim.app.di.appModule
import ir.taqvim.data.events.ics.SubscriptionRefreshWorkerFactory
import ir.taqvim.data.scheduler.AlarmInputWatcher
import ir.taqvim.data.scheduler.PreferenceChangeWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Process entry point: builds the Koin DI graph (see docs/adr/0002-dependency-injection-koin.md), starts the
 * scheduler's preference watcher (T-604), reminder data watcher (T-1001, T-1002), widget update triggers
 * (T-1201…T-1204), the app language sync (T-1501) and the refresh of the persistent notification and launcher icon
 * (T-1213, T-1214), publishes the launcher shortcuts (T-1215), and provides WorkManager with the subscription worker
 * factory (T-1003). A restore left unfinished by the previous process is completed or undone first (B09, ADR-0032), so
 * the watchers, widgets, notification and language sync only ever read and schedule from consistent data.
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
        DebugStrictMode.install(BuildConfig.DEBUG)
        startKoin {
            androidContext(this@TaqvimApplication)
            modules(appModule)
        }
        val recovery = get<RestoreRecovery>()
        val watcher = get<PreferenceChangeWatcher>()
        val reminderInputs = get<AlarmInputWatcher>()
        val widgetTriggers = get<WidgetTriggerWatcher>()
        val languageSync = get<AppLanguageSync>()
        val surfaces = get<SurfaceRefreshWatcher>()
        processScope.launch {
            // Everything that reads or schedules from the stores starts only once they are settled (ADR-0032).
            recovery.run()
            launch { watcher.watch() }
            launch { reminderInputs.watch() }
            launch { widgetTriggers.watch() }
            launch { languageSync.run() }
            launch { surfaces.watch() }
        }
        val launcherIcon = get<LauncherIconSwitcher>()
        processScope.launch { AppShortcuts.publish(this@TaqvimApplication, launcherIcon.enabledEntry()) }
    }

    /**
     * Ends the process-lifetime work with the process. Android does not call this on a device, but Robolectric does
     * when it tears an application down: without it the watchers of a finished test keep collecting on
     * `Dispatchers.Default` and write launcher component state into the next test's package manager, which made
     * `LauncherIconTest` fail on CI (PR run 35340364471) on an app build byte-identical to a green one.
     */
    override fun onTerminate() {
        processScope.cancel()
        super.onTerminate()
    }
}
