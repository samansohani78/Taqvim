/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reads the current [ReminderSetup]; bound in `:app` over personal events, official opt-ins and preferences. */
fun interface ReminderSetupSource {
    suspend fun current(): ReminderSetup
}

/** Shows a reminder notification ([SystemReminderNotifier] in the app). */
fun interface ReminderNotifier {
    /** Shows [reminder]; returns whether it could be posted. */
    fun show(reminder: PlannedReminder): Boolean
}

/** One scheduler alarm of a reminder: the scheduler's [sourceId] (see [PlannedReminder.alarmSourceId]) and time. */
data class ReminderAlarm(
    val sourceId: Long,
    val at: Instant,
)

/**
 * The reminder side of the scheduler (T-604): the alarms reminders need, and what happens when one fires. `:app` adapts
 * [upcoming] to the `REMINDER` `AlarmSource` and [onAlarm] to its `AlarmDelivery`; the scheduler has already dropped
 * alarms that fired more than 15 minutes late.
 */
class ReminderAlarms(
    private val setup: ReminderSetupSource,
    private val log: ReminderDeliveryLog,
    private val notifier: ReminderNotifier,
) {
    /** The alarms of the reminders due strictly after [now]. */
    suspend fun upcoming(now: Instant): List<ReminderAlarm> =
        ReminderPlanner.upcoming(now, setup.current()).map { ReminderAlarm(it.alarmSourceId, it.at) }

    /**
     * Shows the reminder of scheduler source [sourceId] planned exactly at [triggerAt], at most once per occurrence.
     * Returns it, or `null` when nothing is planned then (the event changed meanwhile), it was already shown, or it
     * could not be posted.
     */
    suspend fun onAlarm(
        sourceId: Long,
        triggerAt: Instant,
    ): PlannedReminder? {
        val reminder = ReminderPlanner.at(sourceId, triggerAt, setup.current()) ?: return null
        val shown = log.claim(reminder.key) && notifier.show(reminder)
        return reminder.takeIf { shown }
    }
}

/** Remembers which reminders were shown, so the reminder of one occurrence is never shown twice. */
fun interface ReminderDeliveryLog {
    /** Records [key] ([PlannedReminder.key]); returns `false` when it was already recorded. */
    suspend fun claim(key: String): Boolean
}

/** The keys of recently shown reminders, newest last, bounded by [capacity]; pure. */
data class ReminderDeliveryHistory(
    val entries: List<String>,
    val capacity: Int = DEFAULT_CAPACITY,
) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    fun contains(key: String): Boolean = key in entries

    /** This history with [key] added, dropping the oldest entries beyond [capacity]. */
    fun plus(key: String): ReminderDeliveryHistory = copy(entries = (entries - key + key).takeLast(capacity))

    companion object {
        /** More than the reminders of a month for a busy calendar. */
        const val DEFAULT_CAPACITY: Int = 500
    }
}

/** [ReminderDeliveryLog] kept in a private preferences file, so it survives process death between alarms. */
class SharedPreferencesReminderDeliveryLog(
    context: Context,
) : ReminderDeliveryLog {
    private val preferences: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun claim(key: String): Boolean =
        mutex.withLock {
            val history = ReminderDeliveryHistory(read())
            if (history.contains(key)) return@withLock false
            preferences.edit(commit = true) { putString(KEY, history.plus(key).entries.joinToString(LINE)) }
            true
        }

    private fun read(): List<String> =
        preferences
            .getString(KEY, null)
            .orEmpty()
            .split(LINE)
            .filter { it.isNotBlank() }

    private companion object {
        const val FILE = "taqvim_reminder_deliveries"
        const val KEY = "delivered"
        const val LINE = "\n"
    }
}
