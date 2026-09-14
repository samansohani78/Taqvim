/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Tabs of the day details under the month (T-802). */
enum class DayDetailsTab {
    CALENDARS,
    EVENTS,
    TIMES,
}

/** State of the calendar (home) screen (T-800). */
data class CalendarUiState(
    /** `null` while today and the preferences are loading. */
    val content: CalendarContent? = null,
)

/** The loaded calendar screen. */
data class CalendarContent(
    val today: Jdn,
    val selectedDay: Jdn,
    /** Available calendars in the user's order; the first is the primary calendar the month pager uses. */
    val calendars: ImmutableList<CalendarSystem>,
    /** [selectedDay] in each of [calendars], in the same order. */
    val selectedDates: ImmutableList<CalendarDate>,
    /** Pager position: primary-calendar months from the month of [today] to the shown month. */
    val monthOffset: Int,
    /** The first day of the shown month in the primary calendar. */
    val visibleMonth: CalendarDate,
    val weekStart: Weekday,
    val selectedTab: DayDetailsTab,
    /** Events of [selectedDay]; `null` while they load. */
    val dayDetails: DayDetails?,
    val search: CalendarSearch,
    val islamicVariant: IslamicVariant,
    /** App language code for month names, digits and spoken dates. */
    val languageCode: String,
    val showWeekNumbers: Boolean,
    /** Loaded events of the shown month and the months either side of it (pager prefetch); others are loading. */
    val months: ImmutableList<MonthEvents>,
    /** The Calendars tab of [selectedDay]; `null` while it is computed (T-802). */
    val overview: DayOverview? = null,
    /** The Times tab of [selectedDay] (T-802). */
    val times: DayTimesState = DayTimesState.Loading,
    /** The official event whose source and citation are shown, if any (T-802). */
    val sourceEvent: DayEventItem? = null,
    /** The toolbar menu and its open dialog (T-803). */
    val menu: CalendarMenu = CalendarMenu(),
    /** Calendars the user can choose as the secondary calendar: every available one except the primary (T-803). */
    val secondaryChoices: ImmutableList<CalendarSystem> = persistentListOf(),
)

/** Dialogs opened from the toolbar menu (T-803). */
enum class CalendarDialog {
    DATE_PICKER,
    SECONDARY_CALENDAR,
}

/** The toolbar menu (T-803): whether it is expanded and which of its dialogs is open. */
data class CalendarMenu(
    val isOpen: Boolean = false,
    val dialog: CalendarDialog? = null,
)

/** The six grid weeks of the month [offset] (see [CalendarContent.monthOffset]) with their events. */
data class MonthEvents(
    val offset: Int,
    val days: ImmutableList<CalendarDay>,
)

/** The events of one day as shown in the day details. */
data class DayDetails(
    val jdn: Jdn,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val events: ImmutableList<DayEventItem>,
)

/** The search bar of the calendar screen. */
data class CalendarSearch(
    val isOpen: Boolean = false,
    val query: String = "",
    /** Whether results for [query] are still being computed. */
    val isSearching: Boolean = false,
    val results: ImmutableList<EventSearchResult> = persistentListOf(),
)
