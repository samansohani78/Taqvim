/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Reminder channels (one per [ReminderKind]) and notifications with Done and Snooze actions (T-1001). */
internal object ReminderNotifications {
    const val CHANNEL_PERSONAL: String = "reminders_personal"
    const val CHANNEL_OFFICIAL: String = "reminders_official"

    /**
     * Scheme of the link a tap opens: `taqvim://event/<id>` for a personal event and
     * `taqvim://occasion/<eventId>?day=<jdn>` for an official one. The app resolves these links in T-1103 (deep links);
     * until then they are placeholders.
     */
    const val LINK_SCHEME: String = "taqvim"
    const val LINK_EVENT: String = "event"
    const val LINK_OCCASION: String = "occasion"
    const val LINK_DAY: String = "day"

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Creates both reminder channels. */
    fun ensureChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            channel(
                context,
                CHANNEL_PERSONAL,
                R.string.notification_reminder_channel_personal_name,
                R.string.notification_reminder_channel_personal_description,
            ),
        )
        manager.createNotificationChannel(
            channel(
                context,
                CHANNEL_OFFICIAL,
                R.string.notification_reminder_channel_official_name,
                R.string.notification_reminder_channel_official_description,
            ),
        )
    }

    /** The channel of [kind]. */
    fun channelOf(kind: ReminderKind): String =
        when (kind) {
            ReminderKind.PERSONAL -> CHANNEL_PERSONAL
            ReminderKind.OFFICIAL -> CHANNEL_OFFICIAL
        }

    /**
     * The notification id of [reminder]; one notification per occurrence. Occurrences on their own day keep the id
     * older versions used, so their Done and Snooze actions still reach notifications posted before an update.
     */
    fun idOf(reminder: PlannedReminder): Int = (if (reminder.moved) reminder.key else reminder.legacyKey).hashCode()

    /** The link a tap on [reminder] opens. */
    fun linkOf(reminder: PlannedReminder): Uri {
        val builder = Uri.Builder().scheme(LINK_SCHEME)
        return when (reminder.kind) {
            ReminderKind.PERSONAL -> {
                builder.authority(LINK_EVENT).appendPath(reminder.target)
            }

            ReminderKind.OFFICIAL -> {
                builder
                    .authority(LINK_OCCASION)
                    .appendPath(reminder.target)
                    .appendQueryParameter(LINK_DAY, reminder.occurrence.value.toString())
            }
        }.build()
    }

    /** The notification of [reminder]. */
    fun build(
        context: Context,
        reminder: PlannedReminder,
    ): Notification {
        val id = idOf(reminder)
        val open =
            PendingIntent.getActivity(
                context,
                id,
                Intent(Intent.ACTION_VIEW, linkOf(reminder)).setPackage(context.packageName),
                FLAGS,
            )
        return NotificationCompat
            .Builder(context, channelOf(reminder.kind))
            .setSmallIcon(R.drawable.notification_ic_reminder)
            .setContentTitle(reminder.title)
            .setContentText(textOf(context, reminder))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(reminder.at.toEpochMilliseconds())
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.notification_reminder_done), action(context, reminder, id, DONE))
            .addAction(
                0,
                context.getString(R.string.notification_reminder_snooze),
                action(context, reminder, id, SNOOZE),
            ).build()
    }

    private fun textOf(
        context: Context,
        reminder: PlannedReminder,
    ): String =
        when {
            reminder.kind == ReminderKind.PERSONAL -> {
                context.getString(R.string.notification_reminder_text_personal)
            }

            reminder.daysBefore == 0 -> {
                context.getString(R.string.notification_reminder_text_official_today)
            }

            else -> {
                context.resources.getQuantityString(
                    R.plurals.notification_reminder_text_official_days,
                    reminder.daysBefore,
                    reminder.daysBefore,
                )
            }
        }

    private fun action(
        context: Context,
        reminder: PlannedReminder,
        id: Int,
        action: String,
    ): PendingIntent = PendingIntent.getBroadcast(context, id, ReminderIntents.of(context, action, reminder), FLAGS)

    private fun channel(
        context: Context,
        id: String,
        name: Int,
        description: Int,
    ): NotificationChannel =
        NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_HIGH).apply {
            this.description = context.getString(description)
        }

    private const val DONE = ReminderIntents.ACTION_DONE
    private const val SNOOZE = ReminderIntents.ACTION_SNOOZE
}

/** [ReminderNotifier] that posts the notification when the app may post notifications. */
class SystemReminderNotifier(
    private val context: Context,
) : ReminderNotifier {
    override fun show(reminder: PlannedReminder): Boolean {
        val allowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (allowed) {
            ReminderNotifications.ensureChannels(context)
            NotificationManagerCompat
                .from(context)
                .notify(ReminderNotifications.idOf(reminder), ReminderNotifications.build(context, reminder))
        }
        return allowed
    }
}

/** Shows a snoozed reminder again after [SNOOZE] through an alarm that reaches [ReminderActionReceiver]. */
internal object ReminderSnooze {
    /** How long a snoozed reminder waits (product choice; the plan gives no duration). */
    val SNOOZE: Duration = 10.minutes

    /** Schedules [reminder] to show again at [now] + [SNOOZE]; returns that time. */
    fun schedule(
        context: Context,
        reminder: PlannedReminder,
        now: Instant,
    ): Instant {
        val at = now + SNOOZE
        val operation =
            PendingIntent.getBroadcast(
                context,
                ReminderNotifications.idOf(reminder),
                ReminderIntents.of(context, ReminderIntents.ACTION_SHOW, reminder),
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
