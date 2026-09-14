/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Why widgets have to be redrawn. */
sealed interface WidgetUpdateTrigger {
    /** Local midnight passed in the widgets' time zone. */
    data object DayChanged : WidgetUpdateTrigger

    /** A prayer time was reached, so the next time changed. */
    data object PrayerTimeReached : WidgetUpdateTrigger

    /** A minute boundary passed. */
    data object MinuteTick : WidgetUpdateTrigger

    /** Events were added, changed or removed. */
    data object EventsChanged : WidgetUpdateTrigger

    /** Preferences changed in ways that affect content depending on [affected]. */
    data class PreferencesChanged(
        val affected: Set<WidgetDependency>,
    ) : WidgetUpdateTrigger

    /** The user saved the configuration of widget [appWidgetId]. */
    data class ConfigChanged(
        val appWidgetId: Int,
    ) : WidgetUpdateTrigger

    /** Clock, time zone, language or app version changed: everything may be stale. */
    data object Everything : WidgetUpdateTrigger
}

/** Installed widgets: app widget ids per kind. */
typealias InstalledWidgetIds = Map<WidgetKind, Set<Int>>

/** The next time widgets must be redrawn without an external event, and why. */
data class WidgetWakeUp(
    val at: Instant,
    val triggers: Set<WidgetUpdateTrigger>,
)

/** What the widgets' schedule depends on: their time zone and the next prayer time, if a place is chosen. */
data class WidgetTimeline(
    val timeZone: TimeZone,
    val nextPrayer: Instant?,
)

/**
 * Update policy of T-1200: updates are targeted (only widgets whose content depends on the trigger) and only installed
 * widgets are touched or scheduled for.
 */
class WidgetUpdatePolicy {
    /** The installed widgets that [trigger] makes stale. */
    fun targets(
        trigger: WidgetUpdateTrigger,
        installed: InstalledWidgetIds,
    ): InstalledWidgetIds {
        val stale =
            installed.mapValues { (kind, ids) ->
                when (trigger) {
                    is WidgetUpdateTrigger.ConfigChanged -> ids.filter { it == trigger.appWidgetId }.toSet()
                    is WidgetUpdateTrigger.PreferencesChanged -> ids.takeIf { kind.dependsOnAny(trigger.affected) }
                    WidgetUpdateTrigger.Everything -> ids
                    else -> ids.takeIf { kind.dependsOnAny(setOfNotNull(dependencyOf(trigger))) }
                }.orEmpty()
            }
        return stale.filterValues { it.isNotEmpty() }
    }

    /** [targets] of every trigger in [triggers], merged. */
    fun targets(
        triggers: Set<WidgetUpdateTrigger>,
        installed: InstalledWidgetIds,
    ): InstalledWidgetIds =
        triggers
            .flatMap { targets(it, installed).entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, idSets) -> idSets.flatten().toSet() }

    /**
     * The earliest moment after [now] at which a widget of [installedKinds] goes stale on its own: the next local
     * midnight in the [timeline]'s zone, the next prayer time or the next minute boundary, as far as installed widgets
     * depend on them. `null` when no installed widget needs a wake-up.
     */
    fun nextWakeUp(
        now: Instant,
        timeline: WidgetTimeline,
        installedKinds: Set<WidgetKind>,
    ): WidgetWakeUp? {
        val needed = installedKinds.flatMapTo(mutableSetOf()) { it.dependencies }
        val candidates =
            listOfNotNull(
                (nextMidnight(now, timeline.timeZone) to WidgetUpdateTrigger.DayChanged)
                    .takeIf { WidgetDependency.DATE in needed },
                timeline.nextPrayer
                    ?.takeIf { WidgetDependency.PRAYER_TIMES in needed && it > now }
                    ?.let { it to WidgetUpdateTrigger.PrayerTimeReached },
                (nextMinute(now) to WidgetUpdateTrigger.MinuteTick).takeIf { WidgetDependency.MINUTE in needed },
            )
        val earliest = candidates.minOfOrNull { it.first } ?: return null
        return WidgetWakeUp(earliest, candidates.filter { it.first == earliest }.mapTo(mutableSetOf()) { it.second })
    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60L

        /** The start of the next civil day after [now] in [zone] (the first valid instant when midnight is skipped). */
        fun nextMidnight(
            now: Instant,
            zone: TimeZone,
        ): Instant =
            now
                .toLocalDateTime(zone)
                .date
                .plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(zone)

        /** The next whole minute strictly after [now]; current zone offsets are whole minutes, so it is local too. */
        fun nextMinute(now: Instant): Instant =
            Instant.fromEpochSeconds((Math.floorDiv(now.epochSeconds, SECONDS_PER_MINUTE) + 1) * SECONDS_PER_MINUTE)

        private fun dependencyOf(trigger: WidgetUpdateTrigger): WidgetDependency? =
            when (trigger) {
                WidgetUpdateTrigger.DayChanged -> WidgetDependency.DATE
                WidgetUpdateTrigger.PrayerTimeReached -> WidgetDependency.PRAYER_TIMES
                WidgetUpdateTrigger.MinuteTick -> WidgetDependency.MINUTE
                WidgetUpdateTrigger.EventsChanged -> WidgetDependency.EVENTS
                else -> null
            }

        private fun WidgetKind.dependsOnAny(dependencies: Set<WidgetDependency>): Boolean =
            dependencies.any { it in this.dependencies }
    }
}
