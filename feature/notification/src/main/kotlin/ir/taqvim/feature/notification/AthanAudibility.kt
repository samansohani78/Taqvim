/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

/** The device's ringer mode. */
enum class RingerState {
    NORMAL,
    VIBRATE,
    SILENT,
}

/** What the device allows right now. */
data class DeviceAudioState(
    val ringer: RingerState,
    /** Whether Do Not Disturb filters interruptions. */
    val doNotDisturb: Boolean,
    /** Whether the app has notification policy access, which a Do Not Disturb bypass requires. */
    val policyAccessGranted: Boolean,
)

/** How an athan is presented. */
enum class AthanOutput {
    SOUND,
    VIBRATION_ONLY,
    NONE,
}

/**
 * Silent mode and Do Not Disturb are respected (docs/PLAN.md T-1102) unless the Fajr bypass applies: it is turned on
 * for Fajr and the app has notification policy access. A muted athan still vibrates in vibrate mode when vibration is
 * on; the notification is posted in every case.
 */
object AthanAudibility {
    fun decide(
        prayer: AthanPrayer,
        playback: AthanPlayback,
        state: DeviceAudioState,
    ): AthanOutput {
        val bypass = bypasses(prayer, playback, state)
        return when {
            (bypass || !silenced(state)) && playback.volumePercent > 0 -> AthanOutput.SOUND
            playback.vibrate && (bypass || vibrationAllowed(state)) -> AthanOutput.VIBRATION_ONLY
            else -> AthanOutput.NONE
        }
    }

    private fun bypasses(
        prayer: AthanPrayer,
        playback: AthanPlayback,
        state: DeviceAudioState,
    ): Boolean = prayer == AthanPrayer.FAJR && playback.bypassDndForFajr && state.policyAccessGranted

    private fun silenced(state: DeviceAudioState): Boolean = state.ringer != RingerState.NORMAL || state.doNotDisturb

    private fun vibrationAllowed(state: DeviceAudioState): Boolean =
        !state.doNotDisturb && state.ringer != RingerState.SILENT
}
