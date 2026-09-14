/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * [DailyRefreshScheduler] over one inexact, non-waking alarm: a sleeping device is not woken for a status line; the
 * refresh runs when the device next wakes (at the latest when the user turns the screen on).
 */
class AlarmDailyRefreshScheduler(
    context: Context,
) : DailyRefreshScheduler {
    private val app = context.applicationContext

    override fun schedule(at: Instant) {
        app
            .getSystemService(AlarmManager::class.java)
            ?.setAndAllowWhileIdle(AlarmManager.RTC, at.toEpochMilliseconds(), operation())
    }

    override fun cancel() {
        app.getSystemService(AlarmManager::class.java)?.cancel(operation())
    }

    private fun operation(): PendingIntent =
        PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            Intent(app, DailyRefreshReceiver::class.java).setAction(DailyRefreshReceiver.ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val REQUEST_CODE = 1_213
    }
}

/**
 * Runs the [DailyRefreshCoordinator] when its alarm fires and after a reboot, an app update or a clock, time zone or
 * locale change (which change what "today" is or how it is written). Other broadcasts are ignored (T-1804).
 */
class DailyRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        val coordinator = GlobalContext.getOrNull()?.getOrNull<DailyRefreshCoordinator>() ?: return
        // Null only when the receiver is called outside a broadcast (tests).
        val pending: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.Default)
            .launch { coordinator.run() }
            .invokeOnCompletion { pending?.finish() }
    }

    companion object {
        const val ACTION_REFRESH: String = "ir.taqvim.feature.notification.action.DAILY_REFRESH"

        private val HANDLED_ACTIONS: Set<String> =
            setOf(
                ACTION_REFRESH,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
    }
}
