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

/** What a fired reminder or athan alarm achieved; `:app` hands it to the scheduler (ADR-0033). */
enum class AlarmDeliveryResult {
    /** Posted or started. */
    DELIVERED,

    /** Nothing to show: it changed meanwhile or was already shown. */
    SKIPPED,

    /** Posting or starting failed; the scheduler retries it. */
    FAILED,
}

/** The recorded end state of one reminder or athan occurrence; an occurrence without a record is still pending. */
enum class DeliveryState {
    DELIVERED,

    /** Given up after its deliveries kept failing. */
    FAILED,
}

/**
 * Remembers how reminder and athan occurrences ended (ADR-0033), so a delivered one is never shown twice. An occurrence
 * is recorded only after it was shown, so a failure or process death before that leaves it pending.
 */
interface DeliveryLog {
    /** The recorded state of occurrence [key], or `null` while it is pending. */
    suspend fun state(key: String): DeliveryState?

    /** Records that occurrence [key] ended in [state]. */
    suspend fun record(
        key: String,
        state: DeliveryState,
    )
}

/** The recent occurrence records as text entries `key<TAB>STATE`, newest last, bounded by [capacity]; pure. */
data class DeliveryHistory(
    val entries: List<String>,
    val capacity: Int,
) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    /** The recorded state of [key]; entries written before states existed (only the key) mean delivered. */
    fun stateOf(key: String): DeliveryState? =
        entries.lastOrNull { keyOf(it) == key }?.let { entry ->
            DeliveryState.entries.firstOrNull {
                it.name ==
                    entry.substringAfter(
                        SEPARATOR,
                        DeliveryState.DELIVERED.name,
                    )
            }
        }

    /** This history with [key] recorded as [state], dropping the oldest entries beyond [capacity]. */
    fun with(
        key: String,
        state: DeliveryState,
    ): DeliveryHistory =
        copy(
            entries =
                (
                    entries.filterNot {
                        keyOf(it) == key
                    } + (key + SEPARATOR + state.name)
                ).takeLast(capacity),
        )

    private companion object {
        const val SEPARATOR = "\t"

        fun keyOf(entry: String): String = entry.substringBefore(SEPARATOR)
    }
}

/** [DeliveryLog] kept in the private preferences [file], so it survives process death between alarms. */
class SharedPreferencesDeliveryLog(
    context: Context,
    file: String,
    private val capacity: Int,
) : DeliveryLog {
    private val preferences: SharedPreferences = context.getSharedPreferences(file, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun state(key: String): DeliveryState? = mutex.withLock { history().stateOf(key) }

    override suspend fun record(
        key: String,
        state: DeliveryState,
    ) {
        mutex.withLock {
            preferences.edit(commit = true) { putString(KEY, history().with(key, state).entries.joinToString(LINE)) }
        }
    }

    private fun history(): DeliveryHistory =
        DeliveryHistory(
            preferences
                .getString(KEY, null)
                .orEmpty()
                .split(LINE)
                .filter { it.isNotBlank() },
            capacity,
        )

    companion object {
        /** Reminder records: more than the reminders of a month for a busy calendar. */
        const val REMINDER_FILE: String = "taqvim_reminder_deliveries"
        const val REMINDER_CAPACITY: Int = 500

        /** Athan records: two weeks of five athans a day. */
        const val ATHAN_FILE: String = "taqvim_athan_deliveries"
        const val ATHAN_CAPACITY: Int = 70

        private const val KEY = "delivered"
        private const val LINE = "\n"
    }
}

/**
 * Snoozes reminders and athans through the app's persistent alarm scheduler (ADR-0033), so a snooze survives reboots
 * and is dropped when its event is deleted; bound in `:app`.
 */
interface SnoozeScheduler {
    /** Shows [reminder] again at [at]. */
    suspend fun snoozeReminder(
        reminder: PlannedReminder,
        at: Instant,
    )

    /** Plays [athan] again at [at]. */
    suspend fun snoozeAthan(
        athan: PlannedAthan,
        at: Instant,
    )
}

/** The delivery-log key of [athan]: its prayer and day, as in the records written before ADR-0033. */
fun athanKey(athan: PlannedAthan): String = athan.prayer.name + "@" + athan.day.value
