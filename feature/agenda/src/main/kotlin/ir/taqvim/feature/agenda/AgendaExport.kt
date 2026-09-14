/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

/** Localized texts of the list, resolved from string resources by the UI (T-901). */
class AgendaTexts(
    /** A month name followed by its year. */
    val monthTitle: (name: String, year: String) -> String,
    /** Two months as a range. */
    val monthRange: (first: String, last: String) -> String,
    /** Joins the other calendars' months and the other calendars' dates. */
    val separator: String,
    val today: String,
    val holiday: String,
    val noEvents: String,
    val emptyMonth: String,
    val kinds: Map<AgendaEventKind, String>,
    /** Title of the exported list from its first to its last day. */
    val exportTitle: (from: String, to: String) -> String,
) {
    fun title(header: AgendaMonthHeader): String = monthTitle(header.title.name, header.title.year)

    /** The other calendars' months of [header], e.g. "Sha'ban – Ramadan 1447", or `null` when there are none. */
    fun subtitle(header: AgendaMonthHeader): String? =
        header.otherCalendars
            .map { span ->
                val first = monthTitle(span.first.name, span.first.year)
                if (span.first == span.last) first else monthRange(first, monthTitle(span.last.name, span.last.year))
            }.takeIf { it.isNotEmpty() }
            ?.joinToString(separator)

    fun kind(event: AgendaEvent): String = kinds[event.kind].orEmpty()
}

/** The loaded rows as plain text for sharing and as an HTML document for printing (T-901). */
object AgendaExport {
    private const val STYLE =
        "body{font-family:sans-serif;margin:16px}h1{font-size:18px}h2{font-size:15px;margin:16px 0 4px}" +
            "p{margin:0}td{padding:3px 6px;vertical-align:top;border-bottom:1px solid #ccc}.holiday{color:#b00020}"

    fun text(
        content: AgendaContent,
        texts: AgendaTexts,
    ): String =
        buildString {
            appendLine(title(content, texts))
            content.items.forEach { item ->
                when (item) {
                    is AgendaMonthHeader -> appendLine().appendLine(texts.title(item))
                    is AgendaEmptyMonth -> appendLine(texts.emptyMonth)
                    is AgendaDayRow -> appendLine(dayLine(item, texts))
                }
            }
        }.trimEnd()

    fun html(
        content: AgendaContent,
        texts: AgendaTexts,
    ): String {
        val title = escape(title(content, texts))
        val direction = if (content.isRightToLeft) "rtl" else "ltr"
        return buildString {
            append("<!DOCTYPE html><html lang=\"${escape(content.localeTag)}\" dir=\"$direction\">")
            append("<head><meta charset=\"utf-8\"><title>$title</title><style>$STYLE</style></head>")
            append("<body><h1>$title</h1>")
            content.items.forEach { item -> append(itemHtml(item, texts)) }
            append("</body></html>")
        }
    }

    private fun itemHtml(
        item: AgendaItem,
        texts: AgendaTexts,
    ): String =
        when (item) {
            is AgendaMonthHeader -> {
                "<h2>${escape(texts.title(item))}</h2>"
            }

            is AgendaEmptyMonth -> {
                "<p>${escape(texts.emptyMonth)}</p>"
            }

            is AgendaDayRow -> {
                val events =
                    item.events.joinToString("") { event ->
                        val style = if (event.isHoliday) " class=\"holiday\"" else ""
                        "<p$style>${escape(event.title)}</p>"
                    }
                val empty = if (item.events.isEmpty()) "<p>${escape(texts.noEvents)}</p>" else ""
                val dateStyle = if (item.isHoliday) " class=\"holiday\"" else ""
                "<table><tr><td$dateStyle>${escape(item.longDate)}</td><td>$events$empty</td></tr></table>"
            }
        }

    private fun title(
        content: AgendaContent,
        texts: AgendaTexts,
    ): String {
        val days = content.items.filterIsInstance<AgendaDayRow>()
        return texts.exportTitle(days.firstOrNull()?.longDate.orEmpty(), days.lastOrNull()?.longDate.orEmpty())
    }

    private fun dayLine(
        day: AgendaDayRow,
        texts: AgendaTexts,
    ): String {
        val flags = listOfNotNull(texts.today.takeIf { day.isToday }, texts.holiday.takeIf { day.isHoliday })
        val events = day.events.map { it.title }.ifEmpty { listOf(texts.noEvents) }
        val title = (listOf(day.longDate) + flags).joinToString(texts.separator)
        return title + ": " + events.joinToString(texts.separator)
    }

    /** [text] safe inside HTML element content and double-quoted attributes. */
    internal fun escape(text: String): String =
        buildString {
            text.forEach { char ->
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(char)
                }
            }
        }
}
