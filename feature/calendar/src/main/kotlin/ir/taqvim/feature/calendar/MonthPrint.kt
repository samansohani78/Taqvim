/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import android.content.Context
import android.content.res.Resources
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.component.DayCellModel

/** Prints an HTML document; the calendar route prints the shown month through it (T-803). */
fun interface MonthPrinter {
    fun print(
        context: Context,
        html: String,
        jobName: String,
    )
}

/** Renders the document in an off-screen [WebView], then hands its print adapter to `PrintManager`. */
object WebViewMonthPrinter : MonthPrinter {
    override fun print(
        context: Context,
        html: String,
        jobName: String,
    ) {
        val webView = WebView(context)
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String?,
                ) {
                    context.getSystemService(PrintManager::class.java)?.print(
                        jobName,
                        view.createPrintDocumentAdapter(jobName),
                        PrintAttributes.Builder().build(),
                    )
                }
            }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}

/** Texts of the printed month besides those of the month page. */
class MonthPrintTexts(
    val events: String,
    val noEvents: String,
    /** Joins the titles of one day's events. */
    val separator: String,
)

/** Event dots are not printed, so the page is built without colors. */
private val PRINT_PALETTE =
    IndicatorPalette(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)

/** The shown month of [content] as a printable HTML document, with texts from [resources]. */
internal fun monthPrintHtml(
    content: CalendarContent,
    resources: Resources,
): String {
    val settings = content.settings()
    val language = languageOf(settings.languageCode)
    val calendars = CalendarCalendars(settings)
    val builder =
        MonthPageBuilder(calendars, language, monthTexts(resources), PRINT_PALETTE, shortWeekdayNames(language))
    val events = content.months.firstOrNull { it.offset == content.monthOffset }?.days
    val page = builder.build(content.monthOffset, content.today, content.selectedDay, events)
    val texts =
        MonthPrintTexts(
            events = resources.getString(R.string.calendar_print_events),
            noEvents = resources.getString(R.string.calendar_print_no_events),
            separator = resources.getString(R.string.calendar_separator),
        )
    return MonthPrintDocument.html(page, events.orEmpty(), calendars, language, texts)
}

/** A printable month (T-803): title and subtitle, the six-week grid, and the events of the month's days. */
internal object MonthPrintDocument {
    private const val STYLE =
        "body{font-family:sans-serif;margin:16px}h1{font-size:20px;margin:0}.sub{color:#555;margin:4px 0 12px}" +
            "table{border-collapse:collapse;width:100%}th,td{border:1px solid #bbb;padding:4px;text-align:center}" +
            "td small{display:block;color:#666}.out{color:#aaa}.holiday{color:#b00020}" +
            "h2{font-size:16px;margin:16px 0 4px}li{margin:2px 0}"

    fun html(
        page: MonthPage,
        days: List<CalendarDay>,
        calendars: CalendarCalendars,
        language: LanguageSpec,
        texts: MonthPrintTexts,
    ): String {
        val title = escape(page.heading.title)
        val direction = if (language.direction == TextDirection.RTL) "rtl" else "ltr"
        return buildString {
            append("<!DOCTYPE html><html lang=\"${escape(language.localeTag)}\" dir=\"$direction\">")
            append("<head><meta charset=\"utf-8\"><title>$title</title><style>$STYLE</style></head><body>")
            append("<h1>$title</h1>")
            page.heading.subtitle?.let { append("<p class=\"sub\">${escape(it)}</p>") }
            append(grid(page))
            append(eventList(page, days, calendars, language, texts))
            append("</body></html>")
        }
    }

    private fun grid(page: MonthPage): String =
        buildString {
            append("<table><thead><tr>")
            page.grid.weekdayLabels.forEach { append("<th>${escape(it)}</th>") }
            append("</tr></thead><tbody>")
            page.grid.cells.chunked(MonthLayout.DAYS_PER_WEEK).forEach { week ->
                append("<tr>")
                week.forEach { append(cell(it)) }
                append("</tr>")
            }
            append("</tbody></table>")
        }

    private fun cell(cell: DayCellModel): String {
        val classes = listOfNotNull("out".takeIf { !cell.inCurrentMonth }, "holiday".takeIf { cell.isHoliday })
        val attribute = if (classes.isEmpty()) "" else " class=\"${classes.joinToString(" ")}\""
        val secondary = cell.secondaryLabels.joinToString("") { "<small>${escape(it)}</small>" }
        return "<td$attribute>${escape(cell.dayLabel)}$secondary</td>"
    }

    private fun eventList(
        page: MonthPage,
        days: List<CalendarDay>,
        calendars: CalendarCalendars,
        language: LanguageSpec,
        texts: MonthPrintTexts,
    ): String {
        val inMonth = page.days.filterIndexed { index, _ -> page.grid.cells[index].inCurrentMonth }.toSet()
        val withEvents = days.filter { it.jdn in inMonth && it.events.isNotEmpty() }.sortedBy { it.jdn.value }
        val items =
            withEvents.joinToString("") { day ->
                val primaryDate = calendars.datesOf(day.jdn).first()
                val date = DateFormatter.format(primaryDate, day.jdn.weekday(), language, DateStyle.LONG)
                val titles = day.events.joinToString(texts.separator) { it.title }
                val style = if (day.isHoliday) " class=\"holiday\"" else ""
                "<li$style>${escape(date)}: ${escape(titles)}</li>"
            }
        val list = if (withEvents.isEmpty()) "<p>${escape(texts.noEvents)}</p>" else "<ul>$items</ul>"
        return "<h2>${escape(texts.events)}</h2>$list"
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
