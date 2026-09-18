/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/** User actions on the calendar screen (T-800). */
sealed interface CalendarAction {
    /** Actions that move the selection, the shown month or the details tab. */
    sealed interface Navigation : CalendarAction

    /** Actions of the search bar. */
    sealed interface Search : CalendarAction

    /** Actions that leave the screen. */
    sealed interface Event : CalendarAction

    /** Actions of the toolbar menu and its dialogs (T-803). */
    sealed interface Menu : CalendarAction

    data object OpenMenu : Menu

    data object DismissMenu : Menu

    data object OpenDatePicker : Menu

    /** Go to [year]-[month]-[day] of the primary calendar; the month and day are clamped to what exists. */
    data class PickDate(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Menu

    data object OpenSecondaryCalendarChooser : Menu

    data class ChooseSecondaryCalendar(
        val system: CalendarSystem,
    ) : Menu

    /** Closes the open menu dialog (date picker or secondary calendar chooser). */
    data object DismissDialog : Menu

    data class ShowWeekNumbers(
        val show: Boolean,
    ) : Menu

    /** Toolbar search: the search screen (T-804). */
    data object OpenSearchScreen : Event

    data object OpenShiftWork : Event

    /** The planetary hours of the selected day. */
    data object OpenPlanetaryHours : Event

    /** Print the shown month. */
    data object PrintMonth : Event

    data class SelectDay(
        val jdn: Jdn,
    ) : Navigation

    /** Show the month [offset] primary-calendar months from the month of today (pager position). */
    data class ShowMonth(
        val offset: Int,
    ) : Navigation

    data object ShowNextMonth : Navigation

    data object ShowPreviousMonth : Navigation

    /** Select today and show its month; the selection then follows today across midnight. */
    data object GoToToday : Navigation

    data class SelectTab(
        val tab: DayDetailsTab,
    ) : Navigation

    /** Show the source and citation of an official event of the selected day (T-802). */
    data class ShowEventSource(
        val event: DayEventItem,
    ) : Navigation

    data object DismissEventSource : Navigation

    data object OpenSearch : Search

    data class ChangeSearchQuery(
        val query: String,
    ) : Search

    data object CloseSearch : Search

    /** Jump to the next occurrence of a search result. */
    data class OpenSearchResult(
        val result: EventSearchResult,
    ) : Search

    /** Long press on a day: create an event on [jdn]. */
    data class CreateEvent(
        val jdn: Jdn,
    ) : Event

    data class OpenEvent(
        val event: DayEventItem,
    ) : Event

    /** Tap on a week number: open the timeline at the week beginning on [firstDay]. */
    data class OpenWeek(
        val firstDay: Jdn,
    ) : Event

    /** Open the primary source at [url] cited for an event (T-802). */
    data class OpenCitation(
        val url: String,
    ) : Event

    /** Turns the reminder [daysBefore] days before the official event whose source is shown on or off (T-1002). */
    data class ToggleOfficialReminder(
        val daysBefore: Int,
        val enabled: Boolean,
    ) : Event
}

/** Messages the calendar screen shows in a snackbar; the UI maps them to string resources. */
enum class CalendarMessage {
    /** A search result has no known occurrence from today on. */
    NO_UPCOMING_OCCURRENCE,

    /** A display choice of the menu could not be stored (T-803). */
    SETTING_NOT_SAVED,

    /** A day was asked for that the shown calendars cannot express; the nearest one they can is shown (BUG-1). */
    DATE_OUT_OF_RANGE,
}

/** One-shot effects of the calendar screen. */
sealed interface CalendarEffect {
    data class NavigateToEventEditor(
        val day: Jdn,
    ) : CalendarEffect

    data class NavigateToEvent(
        val event: DayEventItem,
    ) : CalendarEffect

    data class ShowSnackbar(
        val message: CalendarMessage,
    ) : CalendarEffect

    /** Open the timeline at the week that begins on [firstDay] (T-801 week-number column). */
    data class NavigateToTimeline(
        val firstDay: Jdn,
    ) : CalendarEffect

    /** Open a cited primary source (T-802 source tooltip). */
    data class OpenUrl(
        val url: String,
    ) : CalendarEffect

    /** Open the search screen (T-803 toolbar, T-804). */
    data object NavigateToSearch : CalendarEffect

    /** Open shift work (T-803 menu). */
    data object NavigateToShiftWork : CalendarEffect

    /** Open the planetary hours of [day] (T-803 menu, astronomy). */
    data class NavigateToPlanetaryHours(
        val day: Jdn,
    ) : CalendarEffect

    /** Print the shown month (T-803 menu); the route renders and prints it. */
    data object PrintMonth : CalendarEffect

    /** A reminder was turned on: ask for the notification permission if it is missing (T-1002). */
    data object RequestNotificationPermission : CalendarEffect
}
