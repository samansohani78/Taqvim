/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

// The screen content of one state of CalendarViewModel, kept out of the view model so that file stays within the
// 400-line limit. The selected and shown days are clamped to what the shown calendars can express (BUG-1), so a day
// from a restored state or a deep link outside their range degrades instead of failing the whole state flow.

/** What loads after today and the preferences: the selected day's details and the pager's months. */
internal data class Loaded(
    val details: DayDetails?,
    val months: ImmutableList<MonthEvents>,
    val overview: DayOverview?,
    val times: DayTimesState,
)

/** `null` days follow today: no explicit selection, or the shown month is the selected day's month. */
internal data class NavigationState(
    val selectedDay: Jdn? = null,
    val shownDay: Jdn? = null,
    val tab: DayDetailsTab = DayDetailsTab.CALENDARS,
    val sourceEvent: DayEventItem? = null,
)

/**
 * The screen content of one view-model state: [state] clamped to the days the shown calendars can express, the
 * selected day in every calendar, and whatever has loaded so far (BUG-1).
 */
internal fun calendarContent(
    today: Jdn,
    calendars: CalendarCalendars,
    state: NavigationState,
    search: CalendarSearch,
    loaded: Loaded,
    menu: CalendarMenu,
): CalendarContent {
    val selected = CalendarRangeGuard.nearestValid(calendars, state.selectedDay ?: today, today)
    val shown = CalendarRangeGuard.nearestValid(calendars, state.shownDay ?: selected, today)
    return CalendarContent(
        today = today,
        selectedDay = selected,
        calendars = calendars.systems.toImmutableList(),
        selectedDates = calendars.datesOf(selected).toImmutableList(),
        selectedOrigins = calendars.originsOf(selected).toImmutableList(),
        monthOffset = calendars.monthOffset(today, shown),
        visibleMonth = calendars.monthStart(shown),
        weekStart = calendars.settings.weekStart,
        selectedTab = state.tab,
        dayDetails = loaded.details?.takeIf { it.jdn == selected },
        search = search,
        islamicVariant = calendars.settings.islamicVariant,
        islamicOverrides = calendars.settings.islamicOverrides,
        languageCode = calendars.settings.languageCode,
        showWeekNumbers = calendars.settings.showWeekNumbers,
        months = loaded.months,
        overview = loaded.overview?.takeIf { it.day == selected },
        times =
            loaded.times.takeIf { it !is DayTimesState.Ready || it.times.day == selected } ?: DayTimesState.Loading,
        sourceEvent = state.sourceEvent,
        menu = menu,
        secondaryChoices = calendars.secondaryChoices.toImmutableList(),
    )
}
