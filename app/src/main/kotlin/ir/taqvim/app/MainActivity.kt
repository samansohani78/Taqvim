/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ir.taqvim.app.automation.DayChangeAlarm
import ir.taqvim.app.di.AppLocales
import ir.taqvim.app.di.LegacyAppLocales
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.AppIntents
import java.util.Locale
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.android.ext.android.get

/**
 * Single activity hosting the Compose navigation graph. `taqvim://` links and selected text (T-1103, ADR-0016) open
 * their screen, both when they start the activity and when they arrive while it is shown. Before Android 13 the app
 * language (T-1501, ADR-0023) is applied to the activity's context and a new language recreates it.
 */
class MainActivity : ComponentActivity() {
    /** The screen a link or shared text asked for, until the shell has opened it. */
    private var pendingLink by mutableStateOf<AppDestination?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LegacyAppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingLink = destinationOf(intent)
        DayChangeAlarm.schedule(this, Clock.System.now(), TimeZone.currentSystemDefault())
        recreateOnLegacyLanguageChange()
        setContent {
            TaqvimAppShell(
                // Test tags readable as resource ids by the macrobenchmarks and profile generator (T-1801).
                modifier = Modifier.semantics { testTagsAsResourceId = true },
                link = pendingLink,
                onLinkOpened = { pendingLink = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingLink = destinationOf(intent)
    }

    /** Android 13+ recreates activities itself when the per-app language changes. */
    private fun recreateOnLegacyLanguageChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        val locales = get<AppLocales>()
        if (locales !is LegacyAppLocales) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { locales.applied.collect { recreate() } }
        }
    }

    private fun destinationOf(intent: Intent?): AppDestination? =
        AppIntents.destination(intent) {
            AppIntents.parseContext(Clock.System.now(), TimeZone.currentSystemDefault(), Locale.getDefault().language)
        }
}
