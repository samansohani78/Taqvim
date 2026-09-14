/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.model.Jdn

/** User actions on the calendar screen (T-800). */
sealed interface CalendarAction {
    /** Actions that move the selection, the shown month or the details tab. */
    sealed interface Navigation : CalendarAction

    /** Actions of the search bar. */
    sealed interface Search : CalendarAction

    /** Actions that leave the screen. */
    sealed interface Event : CalendarAction

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
}

/** Messages the calendar screen shows in a snackbar; the UI maps them to string resources. */
enum class CalendarMessage {
    /** A search result has no known occurrence from today on. */
    NO_UPCOMING_OCCURRENCE,
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
}
