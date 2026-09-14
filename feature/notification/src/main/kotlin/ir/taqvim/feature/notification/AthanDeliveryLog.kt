/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Remembers which athans have sounded, so an athan never sounds twice for the same prayer of the same day. */
fun interface AthanDeliveryLog {
    /** Records [prayer] of [day]; returns `false` when it was already recorded (the athan must not sound again). */
    suspend fun claim(
        prayer: AthanPrayer,
        day: Jdn,
    ): Boolean
}

/** The recent athans as text entries, newest last, bounded by [capacity]; pure. */
data class AthanDeliveryHistory(
    val entries: List<String>,
    val capacity: Int = DEFAULT_CAPACITY,
) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    /** Whether [prayer] of [day] has sounded. */
    fun contains(
        prayer: AthanPrayer,
        day: Jdn,
    ): Boolean = entryOf(prayer, day) in entries

    /** This history with [prayer] of [day] added, dropping the oldest entries beyond [capacity]. */
    fun plus(
        prayer: AthanPrayer,
        day: Jdn,
    ): AthanDeliveryHistory =
        copy(entries = (entries.filterNot { it == entryOf(prayer, day) } + entryOf(prayer, day)).takeLast(capacity))

    companion object {
        /** Two weeks of five athans a day. */
        const val DEFAULT_CAPACITY: Int = 70

        private const val SEPARATOR = "@"

        /** The entry of [prayer] of [day]. */
        fun entryOf(
            prayer: AthanPrayer,
            day: Jdn,
        ): String = prayer.name + SEPARATOR + day.value
    }
}

/** [AthanDeliveryLog] kept in a private preferences file, so it survives process death between alarms. */
class SharedPreferencesAthanDeliveryLog(
    context: Context,
) : AthanDeliveryLog {
    private val preferences: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun claim(
        prayer: AthanPrayer,
        day: Jdn,
    ): Boolean =
        mutex.withLock {
            val history = AthanDeliveryHistory(read())
            if (history.contains(prayer, day)) return@withLock false
            preferences.edit(commit = true) { putString(KEY, history.plus(prayer, day).entries.joinToString(LINE)) }
            true
        }

    private fun read(): List<String> =
        preferences
            .getString(KEY, null)
            .orEmpty()
            .split(LINE)
            .filter { it.isNotBlank() }

    private companion object {
        const val FILE = "taqvim_athan_deliveries"
        const val KEY = "delivered"
        const val LINE = "\n"
    }
}
