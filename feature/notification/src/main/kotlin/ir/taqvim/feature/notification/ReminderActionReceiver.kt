/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import ir.taqvim.core.model.Jdn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/** The explicit broadcasts of [ReminderActionReceiver]; the reminder travels in the extras. */
internal object ReminderIntents {
    const val ACTION_SHOW: String = "ir.taqvim.feature.notification.action.SHOW_REMINDER"
    const val ACTION_DONE: String = "ir.taqvim.feature.notification.action.REMINDER_DONE"
    const val ACTION_SNOOZE: String = "ir.taqvim.feature.notification.action.SNOOZE_REMINDER"

    private const val EXTRA_KIND = "ir.taqvim.feature.notification.extra.REMINDER_KIND"
    private const val EXTRA_SOURCE = "ir.taqvim.feature.notification.extra.REMINDER_SOURCE"
    private const val EXTRA_TARGET = "ir.taqvim.feature.notification.extra.REMINDER_TARGET"
    private const val EXTRA_TITLE = "ir.taqvim.feature.notification.extra.REMINDER_TITLE"
    private const val EXTRA_OCCURRENCE = "ir.taqvim.feature.notification.extra.REMINDER_OCCURRENCE"
    private const val EXTRA_DAYS_BEFORE = "ir.taqvim.feature.notification.extra.REMINDER_DAYS_BEFORE"
    private const val EXTRA_AT = "ir.taqvim.feature.notification.extra.REMINDER_AT"
    private const val MISSING_LONG = Long.MIN_VALUE
    private const val MISSING_INT = Int.MIN_VALUE

    /** A broadcast for [action] carrying [reminder]. */
    fun of(
        context: Context,
        action: String,
        reminder: PlannedReminder,
    ): Intent =
        Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_KIND, reminder.kind.name)
            .putExtra(EXTRA_SOURCE, reminder.sourceId)
            .putExtra(EXTRA_TARGET, reminder.target)
            .putExtra(EXTRA_TITLE, reminder.title)
            .putExtra(EXTRA_OCCURRENCE, reminder.occurrence.value)
            .putExtra(EXTRA_DAYS_BEFORE, reminder.daysBefore)
            .putExtra(EXTRA_AT, reminder.at.toEpochMilliseconds())

    /** The reminder in [intent], or `null` when its extras are missing or invalid. */
    fun reminderOf(intent: Intent): PlannedReminder? {
        val kind = ReminderKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) }
        val target = intent.getStringExtra(EXTRA_TARGET)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val source = intent.getLongExtra(EXTRA_SOURCE, MISSING_LONG)
        val occurrence = intent.getLongExtra(EXTRA_OCCURRENCE, MISSING_LONG)
        val daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, MISSING_INT)
        val at = intent.getLongExtra(EXTRA_AT, MISSING_LONG)
        if (kind == null || target == null || title == null) return null
        val numbers = listOf(source, occurrence, at).none { it == MISSING_LONG } && daysBefore >= 0
        return if (numbers) {
            PlannedReminder(kind, source, target, title, Jdn(occurrence), daysBefore, Instant.fromEpochMilliseconds(at))
        } else {
            null
        }
    }
}

/** Handles the Done and Snooze actions of reminder notifications and shows snoozed reminders again. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val reminder = ReminderIntents.reminderOf(intent) ?: return
        when (intent.action) {
            ReminderIntents.ACTION_SHOW -> {
                SystemReminderNotifier(context).show(reminder)
            }

            ReminderIntents.ACTION_DONE -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotifications.idOf(reminder))
            }

            ReminderIntents.ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotifications.idOf(reminder))
                snooze(context, reminder)
            }
        }
    }

    /**
     * Snoozes [reminder] through the app's persistent scheduler (ADR-0033); without the app's graph (e.g. a bare
     * receiver in tests) it falls back to a one-off system alarm.
     */
    private fun snooze(
        context: Context,
        reminder: PlannedReminder,
    ) {
        val now = Clock.System.now()
        val snoozer = GlobalContext.getOrNull()?.getOrNull<SnoozeScheduler>()
        if (snoozer == null) {
            ReminderSnooze.schedule(context, reminder, now)
            return
        }
        // Null only when the receiver is called outside a broadcast (tests).
        val pending: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.Default)
            .launch { snoozer.snoozeReminder(reminder, now + ReminderSnooze.SNOOZE) }
            .invokeOnCompletion { pending?.finish() }
    }
}
