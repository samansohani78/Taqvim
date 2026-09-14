/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.core.net.toUri
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanPreferences
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.ExactAlarmStatus
import ir.taqvim.feature.settings.AthanPrayerKind
import ir.taqvim.feature.settings.AthanPreview
import ir.taqvim.feature.settings.AthanSettings
import ir.taqvim.feature.settings.AthanSettingsData
import ir.taqvim.feature.settings.AthanSettingsStore
import ir.taqvim.feature.settings.AthanSoundChoice
import ir.taqvim.feature.settings.AthanSoundLibrary
import ir.taqvim.feature.settings.ExactAlarmAccess
import ir.taqvim.feature.settings.PrayerAlert
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The athan settings (T-1101) stored in the user preferences (T-600). */
internal class PreferencesAthanSettingsStore(
    private val preferences: UserPreferencesRepository,
) : AthanSettingsStore {
    override fun settings(): Flow<AthanSettingsData> =
        preferences.preferences
            .map { AthanSettingsData(it.languageSpec(), it.athan.toFeature()) }
            .distinctUntilChanged()

    override suspend fun update(transform: (AthanSettings) -> AthanSettings) {
        preferences.update { current -> current.copy(athan = transform(current.athan.toFeature()).toData()) }
    }
}

/** These stored settings as the athan screen's model. */
internal fun AthanPreferences.toFeature(): AthanSettings =
    AthanSettings(
        alerts =
            AthanPrayer.entries.associate { prayer ->
                val alert = alerts.getValue(prayer)
                AthanPrayerKind.valueOf(prayer.name) to PrayerAlert(alert.enabled, alert.gapMinutes)
            },
        sound = sound?.let { AthanSoundChoice(it.uri, it.name) },
        vibrate = vibrate,
        bypassDndForFajr = bypassDndForFajr,
        volumePercent = volumePercent,
        useIranTime = useIranTime,
    )

/** The screen's settings as stored ones: a missing alert is off, gaps and volume are clamped, a blank sound is none. */
internal fun AthanSettings.toData(): AthanPreferences =
    AthanPreferences(
        alerts =
            AthanPrayer.entries.associateWith { prayer ->
                alerts[AthanPrayerKind.valueOf(prayer.name)]
                    ?.let { AthanAlert(it.enabled, it.gapMinutes.coerceIn(AthanAlert.GAP_RANGE)) }
                    ?: AthanAlert.OFF
            },
        sound = sound?.takeIf { it.uri.isNotBlank() }?.let { AthanSound(it.uri, it.name) },
        vibrate = vibrate,
        bypassDndForFajr = bypassDndForFajr,
        volumePercent = volumePercent.coerceIn(AthanPreferences.VOLUME_RANGE),
        useIranTime = useIranTime,
    )

/** [ExactAlarmAccess] (T-1101) from the scheduler's exact-alarm [status] (T-604). */
internal class SchedulerExactAlarmAccess(
    private val status: Flow<ExactAlarmStatus>,
) : ExactAlarmAccess {
    override fun allowed(): Flow<Boolean> = status.map { it == ExactAlarmStatus.EXACT }.distinctUntilChanged()
}

/** [AthanSoundLibrary] (T-1101): keeps read access to a picked document and reads its display name. */
internal class ContentResolverAthanSoundLibrary(
    private val resolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AthanSoundLibrary {
    override suspend fun adopt(uri: String): AthanSoundChoice? =
        withContext(io) {
            runCatching {
                val parsed = uri.toUri()
                resolver.takePersistableUriPermission(parsed, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                AthanSoundChoice(uri, displayName(parsed))
            }.getOrNull()
        }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
}

/**
 * [AthanPreview] (T-1101) on the alarm stream: the picked sound, or the default alarm sound, at the given volume for
 * at most [maxDurationMillis]. Playing again stops the previous preview. Main thread only.
 */
internal class MediaPlayerAthanPreview(
    private val context: Context,
    private val maxDurationMillis: Long = DEFAULT_DURATION_MILLIS,
) : AthanPreview {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var finished: () -> Unit = {}
    private val timeout = Runnable { finish() }

    override fun play(
        sound: AthanSoundChoice?,
        volumePercent: Int,
        onFinished: () -> Unit,
    ): Boolean {
        stop()
        val uri = sound?.uri?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return false
        val volume = volumePercent.coerceIn(AthanPreferences.VOLUME_RANGE) / PERCENT
        val candidate = MediaPlayer()
        val started =
            runCatching {
                candidate.setAudioAttributes(ALARM_ATTRIBUTES)
                candidate.setDataSource(context, uri)
                candidate.setVolume(volume, volume)
                candidate.setOnCompletionListener { finish() }
                candidate.prepare()
                candidate.start()
            }
        if (started.isFailure) {
            candidate.release()
            return false
        }
        player = candidate
        finished = onFinished
        handler.postDelayed(timeout, maxDurationMillis)
        return true
    }

    override fun stop() {
        handler.removeCallbacks(timeout)
        player?.let { current ->
            runCatching { current.stop() }
            current.release()
        }
        player = null
        finished = {}
    }

    private fun finish() {
        val callback = finished
        stop()
        callback()
    }

    private companion object {
        const val DEFAULT_DURATION_MILLIS = 10_000L
        const val PERCENT = 100f
        val ALARM_ATTRIBUTES: AudioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
