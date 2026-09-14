/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList

/** Which days the list shows (T-901). */
enum class AgendaMode {
    /** Only days with events, plus today; months without events say so. */
    AGENDA,

    /** Every day of every month. */
    MONTH_LIST,
}

/** State of the month list and agenda screen (T-901). */
data class AgendaUiState(
    /** `null` while today, the preferences and the first months load. */
    val content: AgendaContent? = null,
)

/** The loaded list. */
data class AgendaContent(
    val today: Jdn,
    val mode: AgendaMode,
    /** Month headers, days and empty-month notes of the loaded months, in day order. */
    val items: ImmutableList<AgendaItem>,
    /** Index of today's row in [items], or -1 when today's month is not loaded. */
    val todayIndex: Int,
    val canLoadEarlier: Boolean,
    val canLoadLater: Boolean,
    /** Whether months beyond the loaded ones are being loaded. */
    val isLoading: Boolean,
    /** Changes every time the list must scroll to today (the "Today" action). */
    val scrollToTodayRequest: Int,
    /** App language: locale tag for printing, and whether the language is written right to left. */
    val localeTag: String,
    val isRightToLeft: Boolean,
)

/** A row of the list; [key] is stable across loads so the list keeps its position. */
@Immutable
sealed interface AgendaItem {
    val key: String
}

/** A month name and its year, both already in the app language and digits. */
@Immutable
data class MonthName(
    val name: String,
    val year: String,
)

/** The months of another calendar over a month of the primary one; [first] equals [last] inside one month. */
@Immutable
data class MonthSpan(
    val first: MonthName,
    val last: MonthName,
)

/** The header of a month: its name in the primary calendar and the months it spans in the other calendars. */
@Immutable
data class AgendaMonthHeader(
    val offset: Int,
    val title: MonthName,
    /** One span per other available calendar, in the user's order. */
    val otherCalendars: ImmutableList<MonthSpan>,
) : AgendaItem {
    override val key: String get() = "month-$offset"
}

/** One day of the list. */
@Immutable
data class AgendaDayRow(
    val jdn: Jdn,
    /** Day number in the primary calendar, in the language's digits. */
    val dayNumber: String,
    /** The full date in the primary calendar (LONG style, with the weekday). */
    val longDate: String,
    /** The day in the other calendars (NUMERIC style). */
    val otherDates: ImmutableList<String>,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
    val events: ImmutableList<AgendaEvent>,
) : AgendaItem {
    override val key: String get() = "day-${jdn.value}"
}

/** A note that the month [offset] has no events (agenda mode only). */
@Immutable
data class AgendaEmptyMonth(
    val offset: Int,
) : AgendaItem {
    override val key: String get() = "empty-$offset"
}

/** User actions of the list. */
sealed interface AgendaAction {
    /** The first rows are visible: load earlier months. */
    data object LoadEarlier : AgendaAction

    /** The last rows are visible: load later months. */
    data object LoadLater : AgendaAction

    data object GoToToday : AgendaAction

    data class SelectMode(
        val mode: AgendaMode,
    ) : AgendaAction

    data class OpenDay(
        val jdn: Jdn,
    ) : AgendaAction

    data class OpenEvent(
        val event: AgendaEvent,
    ) : AgendaAction
}

/** One-shot navigation effects. */
sealed interface AgendaEffect {
    data class NavigateToDay(
        val jdn: Jdn,
    ) : AgendaEffect

    data class NavigateToEvent(
        val event: AgendaEvent,
    ) : AgendaEffect
}
