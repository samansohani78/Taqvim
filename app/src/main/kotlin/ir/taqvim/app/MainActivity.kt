/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.taqvim.app.automation.DayChangeAlarm
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.AppIntents
import java.util.Locale
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/**
 * Single activity hosting the Compose navigation graph. `taqvim://` links and selected text (T-1103, ADR-0016) open
 * their screen, both when they start the activity and when they arrive while it is shown.
 */
class MainActivity : ComponentActivity() {
    /** The screen a link or shared text asked for, until the shell has opened it. */
    private var pendingLink by mutableStateOf<AppDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingLink = destinationOf(intent)
        DayChangeAlarm.schedule(this, Clock.System.now(), TimeZone.currentSystemDefault())
        setContent { TaqvimAppShell(link = pendingLink, onLinkOpened = { pendingLink = null }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingLink = destinationOf(intent)
    }

    private fun destinationOf(intent: Intent?): AppDestination? =
        AppIntents.destination(intent) {
            AppIntents.parseContext(Clock.System.now(), TimeZone.currentSystemDefault(), Locale.getDefault().language)
        }
}
