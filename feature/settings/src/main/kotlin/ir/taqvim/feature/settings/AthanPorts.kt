/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.LanguageSpec
import kotlinx.coroutines.flow.Flow

/** The prayers that can have an athan. */
enum class AthanPrayerKind {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

/** The athan of one prayer: whether it sounds, and [gapMinutes] from the prayer time (negative is before). */
data class PrayerAlert(
    val enabled: Boolean,
    val gapMinutes: Int,
) {
    companion object {
        /** Allowed gap between the prayer time and the athan, in minutes. */
        val GAP_RANGE: IntRange = -60..60
    }
}

/** A sound picked through the system file picker. */
data class AthanSoundChoice(
    /** Content URI of the sound. */
    val uri: String,
    /** Display name, or `null` when unknown. */
    val name: String?,
)

/** Athan settings (T-1101) as stored by [AthanSettingsStore]. */
data class AthanSettings(
    /** One alert per [AthanPrayerKind]. */
    val alerts: Map<AthanPrayerKind, PrayerAlert>,
    /** The picked sound, or `null` for the default alarm sound. */
    val sound: AthanSoundChoice?,
    val vibrate: Boolean,
    /** Whether the Fajr athan may sound while Do Not Disturb is on. */
    val bypassDndForFajr: Boolean,
    /** Volume in percent of the alarm stream, 0‥100. */
    val volumePercent: Int,
    /** Whether athan times use Iran Standard Time (UTC+03:30) instead of the time zone rules of the place. */
    val useIranTime: Boolean,
)

/** What the athan settings show: the app language and the stored settings. */
data class AthanSettingsData(
    val language: LanguageSpec,
    val settings: AthanSettings,
)

/** Reads and stores the athan settings; bound in `:app` over the user preferences. */
interface AthanSettingsStore {
    fun settings(): Flow<AthanSettingsData>

    /** Atomically replaces the settings with [transform] of the current value. */
    suspend fun update(transform: (AthanSettings) -> AthanSettings)
}

/** Whether the app may schedule exact alarms (T-604); bound in `:app` over the scheduler's status. */
fun interface ExactAlarmAccess {
    fun allowed(): Flow<Boolean>
}

/** Makes a picked sound usable later: keeps read access to [uri] and finds its name. */
fun interface AthanSoundLibrary {
    /** The sound at [uri], or `null` when it cannot be read. */
    suspend fun adopt(uri: String): AthanSoundChoice?
}

/** Plays a short preview of the athan sound (the full athan is played by T-1102). */
interface AthanPreview {
    /**
     * Starts [sound] (the default alarm sound when `null`) at [volumePercent]; [onFinished] runs when playback ends by
     * itself. Returns whether playback started.
     */
    fun play(
        sound: AthanSoundChoice?,
        volumePercent: Int,
        onFinished: () -> Unit,
    ): Boolean

    fun stop()
}
