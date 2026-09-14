/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Something shown outside the app that goes stale at a day change or a prayer time: the persistent notification
 * (T-1213) or the launcher icon (T-1214). Bound with Koin's `bind DailyRefresh::class` and run together by
 * [DailyRefreshCoordinator].
 */
fun interface DailyRefresh {
    /** Brings the surface up to date at [now]; returns when it goes stale next, or `null` when it needs no wake-up. */
    suspend fun refresh(now: Instant): Instant?
}

/** One wake-up for [DailyRefreshCoordinator]; a new schedule replaces the previous one. */
interface DailyRefreshScheduler {
    fun schedule(at: Instant)

    fun cancel()
}

/**
 * Runs every [DailyRefresh] and sets a single wake-up at the earliest time one of them goes stale, so no foreground
 * service is needed: an inexact alarm at the day change or next prayer time is enough for a status line. A refresh
 * that fails is retried after [RETRY]; a wake-up is never sooner than [MIN_GAP] so a surface cannot spin.
 */
class DailyRefreshCoordinator(
    private val refreshes: List<DailyRefresh>,
    private val scheduler: DailyRefreshScheduler,
    private val clock: Clock = Clock.System,
) {
    private val mutex = Mutex()

    /** Refreshes everything at [now]; returns the wake-up that was set, or `null` when none is needed. */
    suspend fun run(now: Instant = clock.now()): Instant? =
        mutex.withLock {
            val next =
                refreshes
                    .mapNotNull { wakeUpOf(it, now) }
                    .minOrNull()
                    ?.let { maxOf(it, now + MIN_GAP) }
            if (next == null) scheduler.cancel() else scheduler.schedule(next)
            next
        }

    /** When [refresh] wants to run again: its own answer, or [RETRY] from [now] when it failed. */
    private suspend fun wakeUpOf(
        refresh: DailyRefresh,
        now: Instant,
    ): Instant? {
        val result = runCatching { refresh.refresh(now) }
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        return if (failure != null) now + RETRY else result.getOrNull()
    }

    companion object {
        /** When a failed refresh is tried again. */
        val RETRY: Duration = 30.minutes

        /** The shortest wait between two runs. */
        val MIN_GAP: Duration = 1.minutes

        /**
         * The start of the local day after the day of [now] in [zone]; when a clock change skips midnight the day
         * starts at its first existing time.
         */
        fun startOfNextDay(
            now: Instant,
            zone: TimeZone,
        ): Instant =
            now
                .toLocalDateTime(zone)
                .date
                .plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(zone)
    }
}
