/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import kotlin.time.Instant

/**
 * [AlarmClock] on the platform `AlarmManager`. It uses `setExactAndAllowWhileIdle` when exact alarms are permitted and
 * `setAndAllowWhileIdle` otherwise. Alarms are wall-clock (`RTC_WAKEUP`) broadcasts to [AlarmReceiver], one per request
 * code, so setting a request code again replaces its alarm.
 */
class SystemAlarmClock(
    private val context: Context,
) : AlarmClock {
    private val alarmManager: AlarmManager =
        requireNotNull(context.getSystemService(AlarmManager::class.java)) { "AlarmManager is unavailable" }

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun set(
        requestCode: Int,
        triggerAt: Instant,
        exact: Boolean,
    ): Boolean {
        val operation = operation(requestCode)
        val millis = triggerAt.toEpochMilliseconds()
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        // The permission can be revoked between the check and the call; that SecurityException falls back to inexact.
        val setExact =
            exact &&
                permitted &&
                runCatching { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation) }
                    .isSuccess
        if (!setExact) alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        return setExact
    }

    override fun cancel(requestCode: Int) {
        val operation = operation(requestCode)
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun operation(requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            AlarmReceiver.intent(context, requestCode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
