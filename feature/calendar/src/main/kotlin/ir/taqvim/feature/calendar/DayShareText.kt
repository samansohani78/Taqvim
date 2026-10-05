/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

/**
 * The plain text of "share this day" (T-802): the day in every calendar the user has on, its events, and — the point
 * of it — the **primary source** behind each official one.
 *
 * Every official record in this app carries a citation to the document it came from, and until now that evidence
 * could only be read inside the app. A calendar that can hand someone the gazette its holiday came from is making a
 * different claim from one that just asserts the date, so the citations travel with the text rather than being
 * summarised away.
 *
 * Pure text assembly: no Android types, no formatting decisions of its own. Callers pass already-localized strings,
 * so this stays correct in all 24 languages and in right-to-left scripts without knowing about either.
 */
internal object DayShareText {
    /** A citation as shared: its title, the page or clause within it, and the URL. */
    internal data class Source(
        val title: String,
        val page: String?,
        val url: String,
    )

    /** One event as shared: its title and the sources behind it (empty for personal and device events). */
    internal data class Entry(
        val title: String,
        val sources: List<Source>,
    )

    /**
     * [dateLines] is the day in each calendar, already formatted and labelled; [entries] are its events in the order
     * shown; [sourcesHeading] introduces an event's citations. An event with no citation prints its title alone, so
     * a personal reminder is not dressed up as something sourced.
     */
    fun build(
        dateLines: List<String>,
        entries: List<Entry>,
        sourcesHeading: String,
    ): String {
        val lines = mutableListOf<String>()
        lines += dateLines
        entries.forEach { entry ->
            lines += ""
            lines += entry.title
            if (entry.sources.isNotEmpty()) {
                lines += sourcesHeading
                entry.sources.forEach { lines += "  " + sourceLine(it) }
            }
        }
        return lines.joinToString("\n").trim()
    }

    private fun sourceLine(source: Source): String =
        listOfNotNull(source.title.takeIf(String::isNotBlank), source.page?.takeIf(String::isNotBlank))
            .joinToString(" — ")
            .let { described -> if (described.isBlank()) source.url else "$described\n  ${source.url}" }
}
