/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/** T-1102 (R): sound on the alarm usage at the chosen volume, fallback sound, vibration and interruptions. */
@RunWith(AndroidJUnit4::class)
class AthanSessionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audio = requireNotNull(context.getSystemService(AudioManager::class.java))
    private val vibrator = requireNotNull(context.getSystemService(VibratorManager::class.java)).defaultVibrator
    private val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    private val picked: Uri = Uri.parse("content://sounds/athan.mp3")
    private var interruptions = 0
    private val session = AthanSession(context) { interruptions++ }

    private fun playable(uri: Uri) {
        val info = ShadowMediaPlayer.MediaInfo(DURATION_MS, 0)
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), info)
    }

    private fun request(
        sound: Uri?,
        volume: Int = 80,
        vibrate: Boolean = true,
    ) = AthanFixtures.request(
        playback = AthanFixtures.PLAYBACK.copy(soundUri = sound?.toString(), volumePercent = volume, vibrate = vibrate),
    )

    @Before
    fun sounds() {
        playable(alarmSound)
    }

    @Test
    fun thePickedSoundPlaysAtItsVolumeWithVibrationUntilStopped() {
        playable(picked)

        session.start(request(picked, volume = 50), AthanOutput.SOUND) shouldBe picked

        val player = session.currentPlayer.shouldNotBeNull()
        shadowOf(player).state shouldBe ShadowMediaPlayer.State.STARTED
        shadowOf(player).leftVolume shouldBe 0.5f
        shadowOf(vibrator).isVibrating shouldBe true
        shadowOf(audio).lastAudioFocusRequest.shouldNotBeNull()

        session.stop()
        session.stop()
        session.currentPlayer.shouldBeNull()
        shadowOf(vibrator).isVibrating shouldBe false
    }

    @Test
    fun anUnreadableSoundFallsBackToTheDefaultAlarmSound() {
        session.start(request(Uri.parse("content://missing/sound")), AthanOutput.SOUND) shouldBe alarmSound
        session.currentPlayer.shouldNotBeNull()
        session.stop()

        session.start(request(sound = null, vibrate = false), AthanOutput.SOUND) shouldBe alarmSound
        shadowOf(vibrator).isVibrating shouldBe false
        session.stop()
    }

    @Test
    fun mutedOutputsPlayNoSound() {
        session.start(request(picked), AthanOutput.VIBRATION_ONLY).shouldBeNull()
        session.currentPlayer.shouldBeNull()
        shadowOf(vibrator).isVibrating shouldBe true
        session.stop()

        session.start(request(picked), AthanOutput.NONE).shouldBeNull()
        shadowOf(vibrator).isVibrating shouldBe false

        shadowOf(audio).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        session.start(request(sound = null, vibrate = false), AthanOutput.SOUND).shouldBeNull()
        session.currentPlayer.shouldBeNull()
    }

    @Test
    fun lostAudioFocusAndTheEndOfTheSoundInterrupt() {
        session.start(request(sound = null), AthanOutput.SOUND)

        shadowOf(audio).lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        interruptions shouldBe 1
        shadowOf(audio).lastAudioFocusRequest.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        interruptions shouldBe 1

        shadowOf(session.currentPlayer.shouldNotBeNull()).invokeCompletionListener()
        interruptions shouldBe 2
        session.stop()
    }

    private companion object {
        const val DURATION_MS = 60_000
    }
}
