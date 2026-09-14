/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** An event line of a widget, already localized. */
data class WidgetEventLine(
    val title: String,
    val isHoliday: Boolean,
    /** Personal event id to open, or `null` for events that open their day. */
    val eventId: Long? = null,
)

/** The next prayer time, already localized (e.g. "Maghrib" and "19:12" in the app's digits). */
data class WidgetPrayerLine(
    val name: String,
    val time: String,
)

/**
 * Content shared by the widgets (T-1200 shared state), computed off the main thread by [WidgetDataSource] with every
 * text already localized in the app language.
 *
 * @property date the civil (Gregorian) date the content describes, used for the "open day" click target.
 * @property title the date in the primary calendar, e.g. "22 Shahrivar 1405".
 * @property secondaryDate the date in the configuration's secondary calendar, or `null` when there is none.
 */
data class WidgetData(
    val date: LocalDate,
    val dayNumber: String,
    val title: String,
    val weekday: String,
    val secondaryDate: String?,
    val isHoliday: Boolean,
    val events: ImmutableList<WidgetEventLine> = persistentListOf(),
    val nextPrayer: WidgetPrayerLine? = null,
)

/** Loads the content of a widget of [WidgetKind] with its [WidgetConfig] at an instant; bound in `:app`. */
fun interface WidgetDataSource {
    suspend fun load(
        kind: WidgetKind,
        config: WidgetConfig,
        now: Instant,
    ): WidgetData
}

/** Stored per-widget configurations keyed by app widget id; bound in `:app` (e.g. DataStore). */
interface WidgetConfigStore {
    /** The stored configuration of [appWidgetId], or `null` when the widget was never configured. */
    suspend fun config(appWidgetId: Int): WidgetConfig?

    suspend fun save(
        appWidgetId: Int,
        config: WidgetConfig,
    )

    /** Forgets the configurations of deleted widgets. */
    suspend fun delete(appWidgetIds: Set<Int>)
}

/** The widgets' time zone and next prayer time; bound in `:app` from preferences and prayer times. */
fun interface WidgetTimelineSource {
    suspend fun timeline(now: Instant): WidgetTimeline
}

/** Calendars offered as a widget's secondary calendar (the user's calendars); bound in `:app`. */
fun interface WidgetCalendarsSource {
    fun calendars(): Flow<List<CalendarSystem>>
}

/** Which widgets are placed on the home screen. */
fun interface InstalledWidgets {
    suspend fun installed(): InstalledWidgetIds
}

/** Redraws placed widgets. */
fun interface WidgetUpdater {
    suspend fun update(targets: InstalledWidgetIds)
}

/** Wakes the framework once at a [WidgetWakeUp]; a new schedule replaces the previous one. */
interface WidgetWakeUpScheduler {
    fun schedule(wakeUp: WidgetWakeUp)

    fun cancel()
}

/** The widget kind of a placed app widget id, or `null` when the id is not one of Taqvim's widgets. */
fun interface WidgetKindResolver {
    fun kindOf(appWidgetId: Int): WidgetKind?
}
