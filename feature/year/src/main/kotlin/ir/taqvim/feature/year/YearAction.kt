/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import ir.taqvim.core.model.Jdn

/** User actions on the year view (T-805). */
sealed interface YearAction {
    /** Actions that change the shown calendar or year. */
    sealed interface Navigation : YearAction

    /** Actions that change how the year is shown. */
    sealed interface View : YearAction

    /** Show the calendar at [index] of the available calendars (calendar pager position). */
    data class SelectCalendar(
        val index: Int,
    ) : Navigation

    /** Show [year] of the shown calendar; also closes the year selection. */
    data class ShowYear(
        val year: Int,
    ) : Navigation

    data object ShowNextYear : Navigation

    data object ShowPreviousYear : Navigation

    /** Show the year containing today; the view then follows today across a new year. */
    data object GoToToday : Navigation

    /** Fewer, larger mini months per row. */
    data object ZoomIn : View

    /** More, smaller mini months per row. */
    data object ZoomOut : View

    data object OpenYearPicker : View

    data object CloseYearPicker : View

    /** Tap on a month: open the calendar at the month beginning on [firstDay]. */
    data class OpenMonth(
        val firstDay: Jdn,
    ) : YearAction
}

/** One-shot effects of the year view. */
sealed interface YearEffect {
    /** Open the calendar (month pager) at the month that begins on [firstDay]. */
    data class NavigateToMonth(
        val firstDay: Jdn,
    ) : YearEffect
}
