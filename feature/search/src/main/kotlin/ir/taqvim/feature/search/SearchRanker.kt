/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.toImmutableList

/**
 * Ranks events, tools and settings for a query (T-804) into sections in [SearchGroup] order. Every item is matched by
 * [SearchMatcher] on its title and aliases or keywords; items appearing twice (same key) keep their best match.
 * Within a section: higher score first, then holidays, then the title's key, then the key — so the result does not
 * depend on the order of the input. Each section holds at most `limit` results; empty sections are left out.
 */
object SearchRanker {
    /** Results per section when no limit is given. */
    const val DEFAULT_LIMIT: Int = 20

    private val ORDER: Comparator<Ranked> =
        compareByDescending<Ranked> { it.match.score }
            .thenByDescending { it.result.isHoliday }
            .thenBy { SearchMatcher.key(it.result.title) }
            .thenBy { it.result.key }
            .thenBy { it.match.text }
            .thenBy { it.day?.value ?: Long.MIN_VALUE }

    /** Ranked sections; [dayLabel] writes an event's next day. */
    fun rank(
        query: String,
        events: List<SearchEvent>,
        entries: List<SearchEntry>,
        dayLabel: (Jdn) -> String,
        limit: Int = DEFAULT_LIMIT,
    ): List<SearchSection> {
        require(limit > 0) { "limit must be positive (was $limit)" }
        val ranked =
            events.mapNotNull { rankEvent(query, it, dayLabel) } + entries.mapNotNull { rankEntry(query, it) }
        return ranked
            .groupBy { it.result.key }
            .values
            .map { duplicates -> duplicates.minWith(ORDER) }
            .groupBy { it.result.group }
            .let { byGroup ->
                SearchGroup.entries.mapNotNull { group ->
                    byGroup[group]?.sortedWith(ORDER)?.take(limit)?.let { items ->
                        SearchSection(group, items.map { it.result }.toImmutableList())
                    }
                }
            }
    }

    private fun rankEvent(
        query: String,
        event: SearchEvent,
        dayLabel: (Jdn) -> String,
    ): Ranked? =
        SearchMatcher.match(query, listOf(event.title) + event.aliases)?.let { match ->
            Ranked(
                SearchResult(
                    key = "event:${event.kind}:${event.id}",
                    group = SearchGroup.EVENTS,
                    title = event.title,
                    highlight = highlightIn(event.title, query, match),
                    matchedAlias = match.text.takeUnless { it == event.title },
                    target = SearchTarget.Event(event.kind, event.id, event.nextDay),
                    eventKind = event.kind,
                    dayLabel = event.nextDay?.let(dayLabel),
                    isHoliday = event.isHoliday,
                ),
                match,
                event.nextDay,
            )
        }

    private fun rankEntry(
        query: String,
        entry: SearchEntry,
    ): Ranked? =
        SearchMatcher.match(query, listOf(entry.title) + entry.keywords)?.let { match ->
            val (key, group) =
                when (val target = entry.target) {
                    is SearchTarget.Settings -> "settings:${target.entry}" to SearchGroup.SETTINGS
                    is SearchTarget.Tool -> "tool:${target.entry}" to SearchGroup.TOOLS
                    else -> "other:$target" to SearchGroup.TOOLS
                }
            val result =
                SearchResult(
                    key = key,
                    group = group,
                    title = entry.title,
                    highlight = highlightIn(entry.title, query, match),
                    matchedAlias = match.text.takeUnless { it == entry.title },
                    target = entry.target,
                )
            Ranked(result, match, null)
        }

    private fun highlightIn(
        title: String,
        query: String,
        match: TextMatch,
    ): IntRange? = if (match.kind == MatchKind.FUZZY) null else SearchMatcher.highlight(title, query)

    private class Ranked(
        val result: SearchResult,
        val match: TextMatch,
        val day: Jdn?,
    )
}
