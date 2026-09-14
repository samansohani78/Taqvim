/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The groups results are shown in, in display order. */
enum class SearchGroup {
    EVENTS,
    TOOLS,
    SETTINGS,
}

/** Which groups are shown; [group] `null` shows all. */
enum class SearchFilter(
    val group: SearchGroup?,
) {
    ALL(null),
    EVENTS(SearchGroup.EVENTS),
    TOOLS(SearchGroup.TOOLS),
    SETTINGS(SearchGroup.SETTINGS),
}

/** Where a result leads. */
@Immutable
sealed interface SearchTarget {
    data class Day(
        val jdn: Jdn,
    ) : SearchTarget

    data class Event(
        val kind: SearchEventKind,
        val id: String,
        /** The event's next day, when it has one. */
        val day: Jdn?,
    ) : SearchTarget

    data class Settings(
        val entry: SettingsEntry,
    ) : SearchTarget

    data class Tool(
        val entry: ToolEntry,
    ) : SearchTarget
}

/** One result: its [title] with the [highlight]ed query characters, and for events their kind, day and holiday. */
@Immutable
data class SearchResult(
    /** Unique among all results, e.g. `event:OFFICIAL:ir.nowruz`. */
    val key: String,
    val group: SearchGroup,
    val title: String,
    /** Characters of [title] covered by the query; `null` for typo matches or when an alias matched. */
    val highlight: IntRange?,
    /** The alias or keyword that matched when it is not the title. */
    val matchedAlias: String?,
    val target: SearchTarget,
    val eventKind: SearchEventKind? = null,
    /** The event's next day in the primary calendar (LONG style). */
    val dayLabel: String? = null,
    val isHoliday: Boolean = false,
)

/** The results of one [group], best first. */
@Immutable
data class SearchSection(
    val group: SearchGroup,
    val results: ImmutableList<SearchResult>,
)

/** A date the query reads as, with its LONG label in the primary calendar. */
@Immutable
data class DateSuggestion(
    val jdn: Jdn,
    val label: String,
)

/** What the area under the search field shows. */
@Immutable
sealed interface SearchContent {
    /** The query is blank: recent searches. */
    data object Idle : SearchContent

    /** The results of the current query are not ready yet. */
    data object Searching : SearchContent

    /** Results of the current query in the selected groups, and the date it reads as (all groups only). */
    data class Results(
        val sections: ImmutableList<SearchSection>,
        val date: DateSuggestion?,
    ) : SearchContent

    /** Nothing matched in the selected groups and the query is not a date. */
    data object NoResults : SearchContent
}

/** State of the search screen (T-804). */
data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val recent: ImmutableList<String> = persistentListOf(),
    val content: SearchContent = SearchContent.Idle,
)

/** User actions of the search screen. */
sealed interface SearchAction {
    data class ChangeQuery(
        val text: String,
    ) : SearchAction

    data object ClearQuery : SearchAction

    data class SelectFilter(
        val filter: SearchFilter,
    ) : SearchAction

    data class UseRecent(
        val query: String,
    ) : SearchAction

    data object ClearRecent : SearchAction

    data class OpenResult(
        val result: SearchResult,
    ) : SearchAction

    data class OpenDate(
        val date: DateSuggestion,
    ) : SearchAction
}

/** One-shot navigation effects. */
sealed interface SearchEffect {
    data class Navigate(
        val target: SearchTarget,
    ) : SearchEffect
}
