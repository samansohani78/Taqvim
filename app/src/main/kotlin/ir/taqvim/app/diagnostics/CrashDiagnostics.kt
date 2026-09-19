/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.diagnostics

import android.content.Context
import android.os.Build
import ir.taqvim.app.di.appModule
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.about.CrashReport
import ir.taqvim.feature.about.CrashReportSource
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** The crash store and the facts of this run, created before Koin so a failure during start-up is kept too. */
data class CrashDiagnostics(
    val store: CrashLogStore,
    val context: CrashContext,
) {
    companion object {
        /**
         * Installs the handler for this process and returns what the app needs to read and update it. The handler
         * that was installed before stays in the chain, so Android still ends the process and logs the crash.
         */
        fun install(
            context: Context,
            app: String,
        ): CrashDiagnostics {
            val store = CrashLogStore(File(context.filesDir, CrashLogStore.DIRECTORY))
            val facts =
                CrashFacts(
                    app = app,
                    android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    device = "${Build.MANUFACTURER} ${Build.MODEL}",
                    locale = Locale.getDefault().toLanguageTag(),
                )
            val crashContext = CrashContext(facts)
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashCapture(store, crashContext, System::currentTimeMillis, previous),
            )
            return CrashDiagnostics(store, crashContext)
        }
    }
}

/**
 * Every Koin module the app starts with. [crash] is installed before the graph is built (so a failure while it is
 * built is still kept), which is why its bindings arrive as an instance rather than as constructors.
 */
fun startupModules(crash: CrashDiagnostics): List<Module> = listOf(appModule, crashDiagnosticsModule(crash))

/** Bindings of the crash log: the store, the facts of this run and the About screen's port. */
fun crashDiagnosticsModule(diagnostics: CrashDiagnostics): Module =
    module {
        single { diagnostics.store }
        single { diagnostics.context }
        single<CrashReportSource> { FileCrashReportSource(get()) }
        single { CrashSettingsWatcher(get<UserPreferencesRepository>(), get()) }
    }

/** The stored crashes as the About screen's port; files are read off the main thread. */
internal class FileCrashReportSource(
    private val store: CrashLogStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : CrashReportSource {
    private val reloads = MutableStateFlow(0)

    override fun crashes(): Flow<List<CrashReport>> =
        reloads
            .map { store.stored().map { stored -> CrashReport(stored.atEpochMillis, stored.text) } }
            .flowOn(io)

    override suspend fun clear() {
        withContext(io) { store.clear() }
        reloads.update { it + 1 }
    }
}

/** Keeps the crash facts in step with the settings that decide which calendars a screen computes. */
internal class CrashSettingsWatcher(
    private val preferences: UserPreferencesRepository,
    private val context: CrashContext,
) {
    suspend fun watch() {
        preferences.preferences
            .map(::summary)
            .distinctUntilChanged()
            .collect(context::onSettings)
    }

    private fun summary(preferences: UserPreferences): String =
        listOf(
            "calendars=" + preferences.calendars.joinToString("+") { it.name },
            "islamic=${preferences.islamicVariant.name}",
            "override=${preferences.islamicOverride.origin.name}",
            "language=${preferences.languageCode}",
        ).joinToString(" ")
}
