/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

/**
 * One day of a widget month grid or week strip (T-1205…T-1207), already localized.
 *
 * @property date the civil (Gregorian) date, for the "open day" link.
 * @property dayLabel the day of month in the primary calendar, in the language's digits.
 * @property weekdayLabel the narrow weekday name (its first letter).
 * @property secondaryLabel the day of month in the secondary calendar, or `null` without one.
 * @property eventCount how many events the day has in the enabled sources.
 * @property description the spoken date, e.g. "22 Shahrivar 1405".
 */
data class WidgetCalendarDay(
    val date: LocalDate,
    val dayLabel: String,
    val weekdayLabel: String,
    val secondaryLabel: String?,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val inMonth: Boolean,
    val eventCount: Int,
    val description: String,
)

/**
 * A month page of the month widgets (T-1205, T-1206): six weeks from the user's week start.
 *
 * @property title the month in the primary calendar, e.g. "Shahrivar 1405".
 * @property secondaryTitle the months of the secondary calendar the page spans, or `null` without one.
 * @property weekdayLabels one narrow weekday name per column, from the week start.
 * @property offset months from today's month (0 = today's month).
 * @property firstDay the civil date of the shown month's first day.
 */
data class WidgetMonth(
    val title: String,
    val secondaryTitle: String?,
    val weekdayLabels: ImmutableList<String>,
    val days: ImmutableList<WidgetCalendarDay>,
    val offset: Int,
    val firstDay: LocalDate,
)

/** A day of the schedule list (T-1208) with its events; [title] is the weekday and the date. */
data class WidgetScheduleDay(
    val date: LocalDate,
    val title: String,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val events: ImmutableList<WidgetEventLine>,
)

/** Today's sunrise and sunset at the chosen place (T-1209), with the [progress] of daylight or `null` at night. */
data class WidgetSun(
    val sunrise: String,
    val sunset: String,
    val progress: Float?,
)

/** What the events repository knows about a day, for the calendar widgets. */
data class WidgetDayFacts(
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val events: List<WidgetEventLine>,
) {
    companion object {
        val NONE: WidgetDayFacts = WidgetDayFacts(isHoliday = false, isWeekend = false, events = emptyList())
    }
}
