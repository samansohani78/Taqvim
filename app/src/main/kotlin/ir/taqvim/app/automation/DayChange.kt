/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.taqvim.core.calendar.toJdn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * The start of the local day after the day of [now] in [zone]. When a clock change skips midnight, the day starts at
 * its first existing time.
 */
internal fun nextLocalMidnight(
    now: Instant,
    zone: TimeZone,
): Instant =
    now
        .toLocalDateTime(zone)
        .date
        .plus(1, DateTimeUnit.DAY)
        .atStartOfDayIn(zone)

/** One inexact alarm at the next local midnight, for [AutomationBroadcasts.ACTION_DAY_CHANGED] (T-1103). */
internal object DayChangeAlarm {
    const val ACTION_MIDNIGHT: String = "ir.taqvim.app.action.MIDNIGHT"
    private const val REQUEST_CODE = 1_103

    /**
     * Sets (or replaces) the alarm for the midnight after [now] in [zone]. The alarm outlives any screen, so it is
     * built from the Application context even when an Activity calls (T-1803).
     */
    fun schedule(
        context: Context,
        now: Instant,
        zone: TimeZone,
    ) {
        val app = context.applicationContext
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val at = nextLocalMidnight(now, zone).toEpochMilliseconds()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(app))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DayChangeReceiver::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/**
 * At local midnight announces the new day and sets the next alarm; after a reboot, an app update or a clock or time
 * zone change it only sets the alarm again. The alarm is inexact, so the announcement comes shortly after midnight.
 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // T-1804: anything other than our own alarm or the declared system broadcasts is ignored.
        if (intent.action !in HANDLED_ACTIONS) return
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        if (intent.action == DayChangeAlarm.ACTION_MIDNIGHT) {
            context.sendBroadcast(AutomationBroadcasts.dayChanged(now.toJdn(zone)))
        }
        DayChangeAlarm.schedule(context, now, zone)
    }

    private companion object {
        val HANDLED_ACTIONS: Set<String> =
            setOf(
                DayChangeAlarm.ACTION_MIDNIGHT,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
    }
}
