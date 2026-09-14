/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.content.Context
import android.content.Intent
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.feature.notification.AthanEventHook
import ir.taqvim.feature.notification.PlannedAthan

/**
 * Broadcasts for automation apps such as Tasker (T-1103, F-14). The names and extras are a public contract (ADR-0016,
 * docs/AUTOMATION.md): they are only ever added to, never renamed.
 */
object AutomationBroadcasts {
    /** An athan started playing (T-1102). Extras: [EXTRA_PRAYER], [EXTRA_TIME], [EXTRA_JDN]. */
    const val ACTION_ATHAN_STARTED: String = "ir.taqvim.action.ATHAN_STARTED"

    /** A new local day began. Extras: [EXTRA_JDN], [EXTRA_DATE]. */
    const val ACTION_DAY_CHANGED: String = "ir.taqvim.action.DAY_CHANGED"

    /** The prayer: `FAJR`, `DHUHR`, `ASR`, `MAGHRIB` or `ISHA`. */
    const val EXTRA_PRAYER: String = "prayer"

    /** When the athan was due, in milliseconds since the Unix epoch (a `long`). */
    const val EXTRA_TIME: String = "time"

    /** The day as a Julian day number (a `long`). */
    const val EXTRA_JDN: String = "jdn"

    /** The day as an ISO 8601 Gregorian date, e.g. `2026-03-21`. */
    const val EXTRA_DATE: String = "date"

    fun athanStarted(athan: PlannedAthan): Intent =
        Intent(ACTION_ATHAN_STARTED)
            .putExtra(EXTRA_PRAYER, athan.prayer.name)
            .putExtra(EXTRA_TIME, athan.at.toEpochMilliseconds())
            .putExtra(EXTRA_JDN, athan.day.value)

    fun dayChanged(day: Jdn): Intent =
        Intent(ACTION_DAY_CHANGED)
            .putExtra(EXTRA_JDN, day.value)
            .putExtra(EXTRA_DATE, day.toLocalDate().toString())
}

/** Announces every athan that starts (T-1102's [AthanEventHook]) as [AutomationBroadcasts.ACTION_ATHAN_STARTED]. */
internal class BroadcastAthanEventHook(
    private val context: Context,
) : AthanEventHook {
    override fun onAthanStarted(athan: PlannedAthan) {
        context.sendBroadcast(AutomationBroadcasts.athanStarted(athan))
    }
}
