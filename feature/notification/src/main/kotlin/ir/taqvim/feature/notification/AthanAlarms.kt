/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * The athan side of the scheduler (T-604, ADR-0033): the instants athans are due, and what happens when one of them
 * fires. `:app` adapts [upcoming] and [isPlanned] to the prayer `AlarmSource` and [onAlarm] and [onGaveUp] to the prayer
 * `AlarmDelivery`; the scheduler has already dropped alarms that fired more than 15 minutes late.
 */
class AthanAlarms(
    private val setup: AthanSetupSource,
    private val log: DeliveryLog,
    private val starter: AthanPlaybackStarter,
    private val hook: AthanEventHook = AthanEventHook.NONE,
) {
    private val mutex = Mutex()

    /** Instants of the athans due strictly after [now]; none without a setup. */
    suspend fun upcoming(now: Instant): List<Instant> =
        setup
            .current()
            ?.let { current -> AthanPlanner.upcoming(now, current.plan).map { it.at } }
            .orEmpty()
            .distinct()

    /** Whether an athan is still planned at [plannedAt], e.g. for a snooze of it. */
    suspend fun isPlanned(plannedAt: Instant): Boolean = planned(plannedAt) != null

    /**
     * Plays the athan planned exactly at [plannedAt], at most once per prayer and day unless it plays again after a
     * [snoozed] alarm. It is recorded as delivered only after playback started, so a failure leaves it pending for the
     * scheduler to retry.
     */
    suspend fun onAlarm(
        plannedAt: Instant,
        snoozed: Boolean = false,
    ): AlarmDeliveryResult =
        mutex.withLock {
            val request = planned(plannedAt)
            when {
                request == null -> {
                    AlarmDeliveryResult.SKIPPED
                }

                !snoozed && log.state(athanKey(request.athan)) == DeliveryState.DELIVERED -> {
                    AlarmDeliveryResult.SKIPPED
                }

                !starter.start(request) -> {
                    AlarmDeliveryResult.FAILED
                }

                else -> {
                    log.record(athanKey(request.athan), DeliveryState.DELIVERED)
                    hook.onAthanStarted(request.athan)
                    AlarmDeliveryResult.DELIVERED
                }
            }
        }

    /** Records that the athan planned at [plannedAt] was given up after repeated failures. */
    suspend fun onGaveUp(plannedAt: Instant) {
        planned(plannedAt)?.let { log.record(athanKey(it.athan), DeliveryState.FAILED) }
    }

    private suspend fun planned(plannedAt: Instant): AthanRequest? {
        val current = setup.current() ?: return null
        return AthanPlanner.at(plannedAt, current.plan)?.let { AthanRequest(it, current.playback) }
    }
}
