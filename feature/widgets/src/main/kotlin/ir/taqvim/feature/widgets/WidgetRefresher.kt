/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /** Forgets the configurations of deleted widgets and re-plans the wake-up for the remaining ones. */
    suspend fun onDeleted(appWidgetIds: Set<Int>) {
        configs.delete(appWidgetIds)
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
 * the stored configuration (or the kind's default), normalized, and the content, computed on [dispatcher].
 */
class WidgetStateLoader(
    private val configs: WidgetConfigStore,
    private val data: WidgetDataSource,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun load(
        kind: WidgetKind,
        appWidgetId: Int,
    ): WidgetContentState =
        withContext(dispatcher) {
            val config = (configs.config(appWidgetId) ?: WidgetConfig.defaultFor(kind)).normalizedFor(kind)
            runCatching { data.load(kind, config, clock.now()) }
                .fold(
                    onSuccess = { WidgetContentState.Ready(config, it) },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        WidgetContentState.Failed(config)
                    },
                )
        }
}
