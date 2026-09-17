/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList

/** Whether the timeline shows one day or a week. */
enum class TimelineMode {
    DAY,
    WEEK,
}

/** State of the timeline (T-900). */
data class TimelineUiState(
    /** `null` while the clock and the preferences are loading. */
    val content: TimelineContent? = null,
)

/** The loaded timeline. */
data class TimelineContent(
    val now: TimelineNow,
    val mode: TimelineMode,
    /** One column per shown day, in day order. */
    val columns: ImmutableList<TimelineColumn>,
    /** Hour height factor, from [TimelineGeometry.MIN_ZOOM] to [TimelineGeometry.MAX_ZOOM]. */
    val zoom: Float,
    /** The new event being drawn, if any. */
    val draft: TimelineDraft?,
    /** The calendar that titles the days (the first available of the user's calendars). */
    val calendar: CalendarSystem,
    val islamicVariant: IslamicVariant,
    /** App language code for names, digits and spoken descriptions. */
    val languageCode: String,
    /** Whether the events of the shown days have loaded. */
    val isLoaded: Boolean,
    /** Optional official Iranian month starts in use (ADR-0037); `null` when every Islamic date is computed. */
    val islamicOverrides: IslamicMonthTable? = null,
)

/** One shown day. */
data class TimelineColumn(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val allDay: ImmutableList<TimelineEvent>,
    /** Timed events with their columns from [IntervalColoring], in the greedy order. */
    val timed: ImmutableList<PlacedEvent>,
    /** Prayer times of the day at the chosen place; empty without a place or on polar days. */
    val prayerLines: ImmutableList<PrayerLine>,
)

/** A timed [event] drawn in [column] of [columns] equal columns of its day. */
data class PlacedEvent(
    val event: TimelineEvent,
    val column: Int,
    val columns: Int,
)

/** The prayer times drawn as lines across a day. */
enum class PrayerLineKind {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    SUNSET,
    MAGHRIB,
    ISHA,
}

/** A prayer time of [kind] at [minute] of the day. */
data class PrayerLine(
    val kind: PrayerLineKind,
    val minute: Int,
)
