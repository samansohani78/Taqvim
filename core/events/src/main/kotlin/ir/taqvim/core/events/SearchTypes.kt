/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.model.CalendarSystem

/** How a search hit matched the query, from strongest to weakest. */
public enum class MatchKind {
    /** The whole title or alias equals the query. */
    EXACT,

    /** The title or alias starts with the query. */
    PREFIX,

    /** The query occurs inside the title or alias. */
    SUBSTRING,

    /** The title, alias or one of its words is within a few edits of the query. */
    FUZZY,
}

/**
 * A search over event titles (every language) and aliases (T-304). Empty [sources] or [categories] mean "all";
 * [calendar] `null` means any calendar. At most [limit] hits are returned.
 */
public data class SearchQuery(
    public val text: String,
    public val sources: Set<EventSource> = emptySet(),
    public val categories: Set<EventCategory> = emptySet(),
    public val holidaysOnly: Boolean = false,
    public val calendar: CalendarSystem? = null,
    public val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(limit > 0) { "limit must be positive (was $limit)" }
    }

    /** Whether [definition] passes the filters; the text is not considered. */
    public fun accepts(definition: EventDefinition): Boolean {
        val sourceMatches = sources.isEmpty() || definition.source in sources
        val categoryMatches = categories.isEmpty() || definition.category in categories
        val holidayMatches = !holidaysOnly || definition.isHoliday
        val calendarMatches = calendar == null || definition.calendar == calendar
        return sourceMatches && categoryMatches && holidayMatches && calendarMatches
    }

    public companion object {
        /** Hits returned when no limit is given. */
        public const val DEFAULT_LIMIT: Int = 20
    }
}

/**
 * One result: the [definition], the title or alias that matched, how it matched, and its [score] (higher
 * is better).
 */
public data class SearchHit(
    public val definition: EventDefinition,
    public val matchedText: String,
    public val kind: MatchKind,
    public val score: Int,
)
