/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri

/** Reads ringer mode and Do Not Disturb from the platform. */
internal object DeviceAudio {
    fun read(context: Context): DeviceAudioState {
        val audio = context.getSystemService(AudioManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        val filter = notifications?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        return DeviceAudioState(
            ringer =
                when (audio?.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> RingerState.SILENT
                    AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
                    else -> RingerState.NORMAL
                },
            doNotDisturb =
                filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            policyAccessGranted = notifications?.isNotificationPolicyAccessGranted == true,
        )
    }
}

/**
 * One athan being played: sound through [MediaPlayer] on the alarm usage (so it plays regardless of the notification
 * channel's sound), audio focus and vibration. The picked sound falls back to the default alarm sound when it cannot be
 * read. [onInterrupted] runs when playback ends by itself or audio focus is lost (e.g. a call).
 */
internal class AthanSession(
    private val context: Context,
    private val onInterrupted: () -> Unit,
) {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var vibrating = false

    /** The player of the sound being played, for tests. */
    @get:VisibleForTesting
    internal val currentPlayer: MediaPlayer?
        get() = player

    /** Starts [request] as [output]; returns the sound that plays, or `null` without sound. */
    fun start(
        request: AthanRequest,
        output: AthanOutput,
    ): Uri? {
        val sound = if (output == AthanOutput.SOUND && requestFocus()) playFirst(request.playback) else null
        val vibrate = request.playback.vibrate && output != AthanOutput.NONE
        if (vibrate) startVibration()
        return sound
    }

    /** Stops sound and vibration and releases everything; safe to call more than once. */
    fun stop() {
        player?.let { runCatching { it.stop() } }
        player?.release()
        player = null
        focus?.let { request -> context.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request) }
        focus = null
        if (vibrating) vibrator()?.cancel()
        vibrating = false
    }

    private fun playFirst(playback: AthanPlayback): Uri? {
        val volume = playback.volumePercent / FULL_VOLUME_PERCENT
        val candidates =
            listOfNotNull(playback.soundUri?.toUri(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        return candidates
            .firstNotNullOfOrNull { uri ->
                prepared(uri, volume)?.let { uri to it }
            }?.let { (uri, prepared) ->
                player = prepared.apply { start() }
                uri
            }
    }

    private fun prepared(
        uri: Uri,
        volume: Float,
    ): MediaPlayer? {
        val candidate = MediaPlayer()
        return runCatching {
            candidate.setAudioAttributes(ATTRIBUTES)
            candidate.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            candidate.setDataSource(context, uri)
            candidate.setVolume(volume, volume)
            candidate.setOnCompletionListener { onInterrupted() }
            candidate.prepare()
            candidate
        }.onFailure { candidate.release() }.getOrNull()
    }

    private fun requestFocus(): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(ATTRIBUTES)
                .setOnAudioFocusChangeListener { change -> if (change < 0) onInterrupted() }
                .build()
        val granted = audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) focus = request
        return granted
    }

    private fun startVibration() {
        vibrator()?.let { device ->
            device.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, REPEAT_FROM_START))
            vibrating = true
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") // VibratorManager exists from API 31 only.
            context.getSystemService(Vibrator::class.java)
        }

    private companion object {
        const val FULL_VOLUME_PERCENT = 100f
        const val REPEAT_FROM_START = 0
        val VIBRATION_PATTERN = longArrayOf(0, 600, 400, 600, 1_400)
        val ATTRIBUTES: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
