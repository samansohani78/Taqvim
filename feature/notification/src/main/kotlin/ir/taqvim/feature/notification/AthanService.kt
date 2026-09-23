/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.core.app.ServiceCompat
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Plays the athan in the foreground (docs/PLAN.md T-1102): it posts the athan notification with stop and snooze
 * actions, respects silent mode and Do Not Disturb unless the Fajr bypass applies ([AthanAudibility]), and stops on the
 * stop action, when playback ends, when audio focus is lost, or after at most [MAX_DURATION].
 */
class AthanService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish() }
    private var session: AthanSession? = null

    /** Work the service waits for before it stops (a snooze being stored); cancelled when it is destroyed. */
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The athan being played, for tests. */
    @get:VisibleForTesting
    internal val currentSession: AthanSession?
        get() = session

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val request = intent?.let(AthanIntents::requestOf)
        when {
            intent?.action == AthanIntents.ACTION_PLAY && request != null -> {
                play(request)
            }

            intent?.action == AthanIntents.ACTION_SNOOZE && request != null -> {
                snooze(request, startId)
            }

            else -> {
                finish()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Snoozes [request] through the app's persistent scheduler (ADR-0033); without the app's graph, or when the
     * scheduler cannot store it, a one-off system alarm keeps the snooze (review R16). The sound stops at once, but
     * the service — still in the foreground, so the process is not reclaimed — stops only once the snooze is stored.
     * It stops with [startId], so an athan that starts meanwhile keeps playing.
     */
    private fun snooze(
        request: AthanRequest,
        startId: Int,
    ) {
        release()
        val now = Clock.System.now()
        val snoozer = GlobalContext.getOrNull()?.getOrNull<SnoozeScheduler>()
        if (snoozer == null) {
            AthanSnooze.schedule(this, request, now)
            finish(startId)
            return
        }
        work.launch {
            runCatching { snoozer.snoozeAthan(request.athan, now + AthanSnooze.SNOOZE) }
                .onFailure {
                    if (it is CancellationException) {
                        throw it
                    } else {
                        AthanSnooze.schedule(
                            this@AthanService,
                            request,
                            now,
                        )
                    }
                }
            handler.post { finish(startId) }
        }
    }

    override fun onDestroy() {
        work.cancel()
        release()
        super.onDestroy()
    }

    private fun play(request: AthanRequest) {
        release()
        AthanNotifications.ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            AthanNotifications.NOTIFICATION_ID,
            AthanNotifications.build(this, request),
            foregroundType(),
        )
        val output = AthanAudibility.decide(request.athan.prayer, request.playback, DeviceAudio.read(this))
        session = AthanSession(this) { handler.post { finish() } }.also { it.start(request, output) }
        handler.postDelayed(timeout, MAX_DURATION.inWholeMilliseconds)
    }

    private fun finish() {
        release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Stops the service for the command [startId], unless a later command started another athan. */
    private fun finish(startId: Int) {
        if (session != null) return
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun release() {
        handler.removeCallbacks(timeout)
        session?.stop()
        session = null
    }

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0

    companion object {
        /** The longest an athan plays (docs/PLAN.md T-1102). */
        val MAX_DURATION = 5.minutes
    }
}
