/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ir.taqvim.core.model.Jdn
import kotlin.time.Instant

/** The explicit intents of [AthanService]; a request travels in the extras so the service needs no other state. */
internal object AthanIntents {
    const val ACTION_PLAY: String = "ir.taqvim.feature.notification.action.PLAY_ATHAN"
    const val ACTION_STOP: String = "ir.taqvim.feature.notification.action.STOP_ATHAN"
    const val ACTION_SNOOZE: String = "ir.taqvim.feature.notification.action.SNOOZE_ATHAN"

    private const val EXTRA_PRAYER = "ir.taqvim.feature.notification.extra.PRAYER"
    private const val EXTRA_DAY = "ir.taqvim.feature.notification.extra.DAY"
    private const val EXTRA_AT = "ir.taqvim.feature.notification.extra.AT"
    private const val EXTRA_SOUND = "ir.taqvim.feature.notification.extra.SOUND"
    private const val EXTRA_VOLUME = "ir.taqvim.feature.notification.extra.VOLUME"
    private const val EXTRA_VIBRATE = "ir.taqvim.feature.notification.extra.VIBRATE"
    private const val EXTRA_BYPASS = "ir.taqvim.feature.notification.extra.BYPASS"
    private const val MISSING_LONG = Long.MIN_VALUE
    private const val MISSING_INT = Int.MIN_VALUE

    /** An intent for [action] carrying [request]. */
    fun of(
        context: Context,
        action: String,
        request: AthanRequest,
    ): Intent =
        Intent(context, AthanService::class.java)
            .setAction(action)
            .putExtra(EXTRA_PRAYER, request.athan.prayer.name)
            .putExtra(EXTRA_DAY, request.athan.day.value)
            .putExtra(EXTRA_AT, request.athan.at.toEpochMilliseconds())
            .putExtra(EXTRA_SOUND, request.playback.soundUri)
            .putExtra(EXTRA_VOLUME, request.playback.volumePercent)
            .putExtra(EXTRA_VIBRATE, request.playback.vibrate)
            .putExtra(EXTRA_BYPASS, request.playback.bypassDndForFajr)

    /** An intent for [action] without a request (stop). */
    fun of(
        context: Context,
        action: String,
    ): Intent = Intent(context, AthanService::class.java).setAction(action)

    /** The request in [intent], or `null` when its extras are missing or invalid. */
    fun requestOf(intent: Intent): AthanRequest? {
        val prayer = AthanPrayer.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) } ?: return null
        val day = intent.getLongExtra(EXTRA_DAY, MISSING_LONG)
        val at = intent.getLongExtra(EXTRA_AT, MISSING_LONG)
        val volume = intent.getIntExtra(EXTRA_VOLUME, MISSING_INT)
        val valid = day != MISSING_LONG && at != MISSING_LONG && volume in AthanPlayback.VOLUME_RANGE
        return if (valid) {
            AthanRequest(
                athan = PlannedAthan(prayer, Jdn(day), Instant.fromEpochMilliseconds(at)),
                playback =
                    AthanPlayback(
                        soundUri = intent.getStringExtra(EXTRA_SOUND),
                        volumePercent = volume,
                        vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false),
                        bypassDndForFajr = intent.getBooleanExtra(EXTRA_BYPASS, false),
                    ),
            )
        } else {
            null
        }
    }
}

/**
 * [AthanPlaybackStarter] that starts [AthanService] in the foreground. Android may refuse a foreground start from the
 * background when the alarm was inexact; the athan notification is then posted without playback.
 */
class ServiceAthanPlaybackStarter(
    private val context: Context,
) : AthanPlaybackStarter {
    override fun start(request: AthanRequest): Boolean {
        val started =
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    AthanIntents.of(context, AthanIntents.ACTION_PLAY, request),
                )
            }.isSuccess
        if (!started) postWithoutPlayback(request)
        return started
    }

    private fun postWithoutPlayback(request: AthanRequest) {
        val allowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (allowed) {
            AthanNotifications.ensureChannel(context)
            NotificationManagerCompat
                .from(context)
                .notify(AthanNotifications.NOTIFICATION_ID, AthanNotifications.build(context, request))
        }
    }
}
