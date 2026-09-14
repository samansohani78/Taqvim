/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The athan notification channel and notification, with stop and snooze actions. */
internal object AthanNotifications {
    const val CHANNEL_ID: String = "athan"
    const val NOTIFICATION_ID: Int = 1102

    private const val STOP_REQUEST = 1
    private const val SNOOZE_REQUEST = 2
    private const val IMMUTABLE = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Creates the channel; it has no sound of its own, because the service plays the athan. */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_athan_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_athan_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** The notification of [request]. */
    fun build(
        context: Context,
        request: AthanRequest,
    ): Notification {
        val stopIntent = AthanIntents.of(context, AthanIntents.ACTION_STOP)
        val stop = PendingIntent.getService(context, STOP_REQUEST, stopIntent, IMMUTABLE)
        val snooze =
            PendingIntent.getService(
                context,
                SNOOZE_REQUEST,
                AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request),
                IMMUTABLE,
            )
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_ic_athan)
            .setContentTitle(context.getString(titleOf(request.athan.prayer)))
            .setContentText(context.getString(R.string.notification_athan_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setDeleteIntent(stop)
            .addAction(0, context.getString(R.string.notification_athan_stop), stop)
            .addAction(0, context.getString(R.string.notification_athan_snooze), snooze)
            .build()
    }

    /** String resource of the title of [prayer]'s athan. */
    fun titleOf(prayer: AthanPrayer): Int =
        when (prayer) {
            AthanPrayer.FAJR -> R.string.notification_athan_title_fajr
            AthanPrayer.DHUHR -> R.string.notification_athan_title_dhuhr
            AthanPrayer.ASR -> R.string.notification_athan_title_asr
            AthanPrayer.MAGHRIB -> R.string.notification_athan_title_maghrib
            AthanPrayer.ISHA -> R.string.notification_athan_title_isha
        }
}

/** Plays a snoozed athan again after [SNOOZE] through an alarm that starts [AthanService]. */
internal object AthanSnooze {
    /** How long a snoozed athan waits (product choice; the plan gives no duration). */
    val SNOOZE: Duration = 10.minutes

    private const val REQUEST = 3

    /** Schedules [request] to play again at [now] + [SNOOZE]; returns the time it plays. */
    fun schedule(
        context: Context,
        request: AthanRequest,
        now: Instant,
    ): Instant {
        val at = now + SNOOZE
        val operation =
            PendingIntent.getForegroundService(
                context,
                REQUEST,
                AthanIntents.of(context, AthanIntents.ACTION_PLAY, request),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        context.getSystemService(AlarmManager::class.java)?.let { alarms ->
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), operation)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), operation)
            }
        }
        return at
    }
}
