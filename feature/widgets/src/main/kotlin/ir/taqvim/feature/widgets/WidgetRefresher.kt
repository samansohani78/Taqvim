/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Applies the [WidgetUpdatePolicy]: redraws only the installed widgets a trigger makes stale, then schedules the next
 * wake-up for the widgets still installed (or cancels it when there are none).
 */
class WidgetRefresher(
    private val installed: InstalledWidgets,
    private val updater: WidgetUpdater,
    private val scheduler: WidgetWakeUpScheduler,
    private val timeline: WidgetTimelineSource,
    private val configs: WidgetConfigStore,
    private val clock: Clock,
    private val policy: WidgetUpdatePolicy = WidgetUpdatePolicy(),
    private val views: WidgetViewStore = WidgetViewStore.NONE,
) {
    suspend fun refresh(trigger: WidgetUpdateTrigger) {
        refresh(setOf(trigger))
    }

    suspend fun refresh(triggers: Set<WidgetUpdateTrigger>) {
        val widgets = installed.installed().filterValues { it.isNotEmpty() }
        val targets = policy.targets(triggers, widgets)
        if (targets.isNotEmpty()) updater.update(targets)
        reschedule(widgets)
    }

    /** Re-plans the wake-up without redrawing, e.g. after a widget was placed. */
    suspend fun reschedule() {
        reschedule(installed.installed().filterValues { it.isNotEmpty() })
    }

    /** Forgets the configurations and views of deleted widgets and re-plans the wake-up for the remaining ones. */
    suspend fun onDeleted(appWidgetIds: Set<Int>) {
        configs.delete(appWidgetIds)
        views.delete(appWidgetIds)
        reschedule()
    }

    private suspend fun reschedule(widgets: InstalledWidgetIds) {
        val now = clock.now()
        val wakeUp = if (widgets.isEmpty()) null else policy.nextWakeUp(now, timeline.timeline(now), widgets.keys)
        if (wakeUp == null) scheduler.cancel() else scheduler.schedule(wakeUp)
    }
}

/** What a widget shows: its configuration with loaded content, or a failure message. */
sealed interface WidgetContentState {
    val config: WidgetConfig

    data class Ready(
        override val config: WidgetConfig,
        val data: WidgetData,
    ) : WidgetContentState

    data class Failed(
        override val config: WidgetConfig,
    ) : WidgetContentState
}

/**
 * Loads a widget's state from the stores on every session (nothing is kept in memory, so process death loses nothing):
 * the stored configuration (or the kind's default), normalized, its navigation view, and the content, computed on
 * [dispatcher].
 */
class WidgetStateLoader(
    private val configs: WidgetConfigStore,
    private val data: WidgetDataSource,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val views: WidgetViewStore = WidgetViewStore.NONE,
    private val timeout: Duration = LOAD_TIMEOUT,
) {
    /**
     * The widget's state, always — this function has no path that waits forever.
     *
     * `provideGlance` awaits this before it calls `provideContent`, so for as long as this suspends the launcher
     * keeps showing the widget's `android:initialLayout`, which for every Taqvim widget is Glance's loading
     * spinner. A dependency that never answers therefore does not degrade the widget, it replaces it with a
     * spinner that never goes away, and nothing retries. That is what users saw in v1.3.0 and it is the reason
     * both halves below are bounded rather than merely wrapped in `runCatching`: catching a throw was never the
     * problem, and a `runCatching` around a suspend that never returns catches nothing.
     *
     * A store that is slow, broken or silent costs the widget its content, not its existence: the configuration
     * falls back to the kind's default and the content to [WidgetContentState.Failed], which draws a message the
     * user can act on.
     */
    suspend fun load(
        kind: WidgetKind,
        appWidgetId: Int,
    ): WidgetContentState =
        withContext(dispatcher) {
            val config = storedConfig(kind, appWidgetId)
            val loaded = bounded { data.load(kind, config, clock.now(), views.view(appWidgetId)) }
            if (loaded == null) WidgetContentState.Failed(config) else WidgetContentState.Ready(config, loaded)
        }

    /** The stored configuration, or the kind's default when the store throws or does not answer in time. */
    private suspend fun storedConfig(
        kind: WidgetKind,
        appWidgetId: Int,
    ): WidgetConfig = (bounded { configs.config(appWidgetId) } ?: WidgetConfig.defaultFor(kind)).normalizedFor(kind)

    /**
     * [block]'s value, or `null` when it fails or outlasts [timeout].
     *
     * The ordering matters: the `try` is *inside* [withTimeoutOrNull] and rethrows [CancellationException], so the
     * timeout's own cancellation is not swallowed by the failure handling. A `runCatching` wrapped around a
     * `withTimeout` would catch the timeout and turn a bounded wait back into an unbounded one.
     */
    private suspend fun <T> bounded(block: suspend () -> T): T? =
        withTimeoutOrNull(timeout) {
            // The state is the report: a failure draws the widget's failure message, and the next trigger retries.
            runCatching { block() }
                .getOrElse { failure -> if (failure is CancellationException) throw failure else null }
        }

    public companion object {
        /**
         * How long one store may take before the widget draws without it.
         *
         * Long enough that a cold process reading two DataStore files and the event dataset on a slow device
         * still wins — the dataset's first read alone was 148 ms on the owner's phone — and short enough that a
         * user watching a launcher does not conclude the widget is broken.
         */
        public val LOAD_TIMEOUT: Duration = 10.seconds
    }
}
