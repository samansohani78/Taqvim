/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import kotlin.time.Instant

/** How the athan plays (T-1101 settings). */
data class AthanPlayback(
    /** Content URI of the picked sound, or `null` for the default alarm sound. */
    val soundUri: String?,
    /** Volume in percent, 0‥100. */
    val volumePercent: Int,
    val vibrate: Boolean,
    /** Whether the Fajr athan may sound while silent mode or Do Not Disturb is on. */
    val bypassDndForFajr: Boolean,
) {
    init {
        require(volumePercent in VOLUME_RANGE) { "volume must be within $VOLUME_RANGE" }
    }

    companion object {
        val VOLUME_RANGE: IntRange = 0..100
    }
}

/** The current athan setup, or no setup while no place is chosen. */
data class AthanSetup(
    val plan: AthanPlanSettings,
    val playback: AthanPlayback,
)

/** Reads the current [AthanSetup]; bound in `:app` over the user preferences and the chosen place. */
fun interface AthanSetupSource {
    /** The setup now, or `null` when athans cannot be planned (no place chosen). */
    suspend fun current(): AthanSetup?
}

/** One athan to play. */
data class AthanRequest(
    val athan: PlannedAthan,
    val playback: AthanPlayback,
)

/** Starts playing an athan (the foreground [AthanService] in the app). */
fun interface AthanPlaybackStarter {
    /** Starts [request]; returns whether playback could start. */
    fun start(request: AthanRequest): Boolean
}

/** Hook for automation broadcasts (T-1103, `ACTION_ATHAN_STARTED`). */
fun interface AthanEventHook {
    fun onAthanStarted(athan: PlannedAthan)

    companion object {
        /** No automation until T-1103 provides it. */
        val NONE: AthanEventHook = AthanEventHook { }
    }
}

/**
 * The athan side of the scheduler (T-604): the instants athans are due, and what happens when one of them fires. `:app`
 * adapts [upcoming] to the prayer `AlarmSource` and [onAlarm] to the prayer `AlarmDelivery`; the scheduler has already
 * dropped alarms that fired more than 15 minutes late.
 */
class AthanAlarms(
    private val setup: AthanSetupSource,
    private val log: AthanDeliveryLog,
    private val starter: AthanPlaybackStarter,
    private val hook: AthanEventHook = AthanEventHook.NONE,
) {
    /** Instants of the athans due strictly after [now]; none without a setup. */
    suspend fun upcoming(now: Instant): List<Instant> =
        setup
            .current()
            ?.let { current -> AthanPlanner.upcoming(now, current.plan).map { it.at } }
            .orEmpty()
            .distinct()

    /**
     * Plays the athan planned exactly at [triggerAt], at most once per prayer and day. Returns the started request, or
     * `null` when nothing is planned at that instant (settings changed meanwhile), it already sounded, or it could not
     * start.
     */
    suspend fun onAlarm(triggerAt: Instant): AthanRequest? {
        val current = setup.current() ?: return null
        val athan = AthanPlanner.at(triggerAt, current.plan) ?: return null
        val request = AthanRequest(athan, current.playback)
        val started = log.claim(athan.prayer, athan.day) && starter.start(request)
        if (started) hook.onAthanStarted(athan)
        return request.takeIf { started }
    }
}
