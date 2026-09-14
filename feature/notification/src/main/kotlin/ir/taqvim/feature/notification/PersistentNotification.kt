/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import kotlin.time.Instant

/** The persistent date notification's channel and notification (T-1213). */
object PersistentNotifications {
    const val CHANNEL: String = "persistent_date"
    const val ID: Int = 1_213
    private const val STATUS_ICON_DP = 24f

    /** Creates the low-importance channel, visible on the lock screen, without sound, vibration or badge. */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notification_persistent_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_persistent_channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** The ongoing, silent notification of [content]; a tap opens today in the app. */
    fun build(
        context: Context,
        content: PersistentNotificationContent,
        icons: DayIconCache,
    ): Notification {
        val resources = context.resources
        val statusPx = (STATUS_ICON_DP * resources.displayMetrics.density).toInt()
        val open = TodaySurfaces.openTodayPendingIntent(context, ID)
        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(content.title)
        content.lines.forEach(inbox::addLine)
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(IconCompat.createWithBitmap(icons.icon(content.iconText, statusPx, DayIconStyle.GLYPH)))
                .setContentTitle(content.title)
                .setContentText(content.text)
                .setSubText(content.subText)
                .setStyle(inbox)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(open)
        if (content.largeNumber) {
            val largePx = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
            val color = ContextCompat.getColor(context, R.color.notification_day_badge)
            builder.setLargeIcon(icons.icon(content.iconText, largePx, DayIconStyle.BADGE, color))
        }
        return builder.build()
    }
}

/**
 * Keeps the persistent notification (T-1213) in step with the setting and today's summary. Off: the notification is
 * removed and no wake-up is needed. On: it is posted when its content changed or it is not showing (e.g. after a
 * reboot), and the next wake-up is the next prayer time or the day change, whichever comes first.
 */
class PersistentNotificationRefresh(
    private val context: Context,
    private val summaries: TodaySummarySource,
    private val options: PersistentNotificationOptionsSource,
    private val posted: PostedNotificationStore,
    private val icons: DayIconCache,
) : DailyRefresh {
    override suspend fun refresh(now: Instant): Instant? {
        val chosen = options.options()
        if (!chosen.enabled) {
            NotificationManagerCompat.from(context).cancel(PersistentNotifications.ID)
            posted.forget()
            return null
        }
        val summary = summaries.load(now)
        val content =
            PersistentNotificationContents.of(summary, chosen.largeNumber) {
                context.getString(R.string.notification_persistent_next_prayer, it.name, it.time)
            }
        if (content.key != posted.postedKey() || !isShowing()) post(content)
        return summary.staleAt
    }

    private fun post(content: PersistentNotificationContent) {
        val allowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (allowed) {
            PersistentNotifications.ensureChannel(context)
            NotificationManagerCompat
                .from(context)
                .notify(PersistentNotifications.ID, PersistentNotifications.build(context, content, icons))
            posted.remember(content.key)
        }
    }

    private fun isShowing(): Boolean =
        context
            .getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            .orEmpty()
            .any { it.id == PersistentNotifications.ID }
}
