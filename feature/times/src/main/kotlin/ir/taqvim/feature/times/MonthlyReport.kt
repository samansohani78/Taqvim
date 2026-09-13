/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerTimesResult

/** One day of the monthly report. */
data class ReportRow(
    val date: CalendarDate,
    val weekday: Weekday,
    val times: PrayerTimesResult,
)

/** Prayer times of every day of one month of the settings' calendar (29–31 rows). */
data class MonthlyReport(
    val year: Int,
    val month: Int,
    val rows: List<ReportRow>,
)

/** Builds [MonthlyReport]s (T-1100). */
object MonthlyReportBuilder {
    /** The report of [month] of [year] in the settings' calendar. */
    fun build(
        year: Int,
        month: Int,
        settings: TimesSettings,
    ): MonthlyReport {
        val calendar = settings.calendar
        val first = calendar.toJdn(calendar.date(year, month, 1))
        val rows =
            (0 until calendar.monthLength(year, month)).map { offset ->
                val day = first + offset
                ReportRow(calendar.fromJdn(day), day.weekday(), PrayerSchedule.calculate(day, settings))
            }
        return MonthlyReport(year, month, rows)
    }

    /** The report of the month containing [day]. */
    fun forDay(
        day: Jdn,
        settings: TimesSettings,
    ): MonthlyReport {
        val date = settings.calendar.fromJdn(day)
        return build(date.year, date.month, settings)
    }
}

/** Localized texts of the printable report; [title] receives the place and the first and last dates. */
data class ReportLabels(
    val title: (place: String, from: String, to: String) -> String,
    val date: String,
    val prayers: Map<PrayerKind, String>,
    val undefined: String,
    val polarDay: String,
    val polarNight: String,
)

/** The report as a self-contained HTML document for `PrintManager` (T-1100). */
object ReportHtml {
    private const val STYLE =
        "body{font-family:sans-serif;margin:16px}h1{font-size:16px}" +
            "table{border-collapse:collapse;width:100%;font-size:11px}" +
            "th,td{border:1px solid #999;padding:3px 4px;text-align:center}th{background:#eee}"

    /** [report] for the place of [settings] in its language, digits and direction. */
    fun render(
        report: MonthlyReport,
        labels: ReportLabels,
        settings: TimesSettings,
    ): String {
        val language = settings.language
        val dates = report.rows.map { DateFormatter.format(it.date, it.weekday, language, DateStyle.NUMERIC) }
        val title = labels.title(settings.placeName, dates.first(), dates.last())
        val direction = if (language.direction == TextDirection.RTL) "rtl" else "ltr"
        return buildString {
            append("<!DOCTYPE html><html lang=\"${escape(language.localeTag)}\" dir=\"$direction\">")
            append("<head><meta charset=\"utf-8\"><title>${escape(title)}</title><style>$STYLE</style></head>")
            append("<body><h1>${escape(title)}</h1><table><thead><tr><th>${escape(labels.date)}</th>")
            PrayerKind.entries.forEach { append("<th>${escape(labels.prayers[it].orEmpty())}</th>") }
            append("</tr></thead><tbody>")
            report.rows.zip(dates).forEach { (row, date) -> append(rowHtml(row, date, labels, settings)) }
            append("</tbody></table></body></html>")
        }
    }

    private fun rowHtml(
        row: ReportRow,
        date: String,
        labels: ReportLabels,
        settings: TimesSettings,
    ): String {
        val cells =
            when (val times = row.times) {
                is PrayerTimesResult.Available -> {
                    PrayerSchedule.entries(times.times).joinToString("") { entry ->
                        val text = entry.time?.localized(settings.language.numerals) ?: labels.undefined
                        "<td>${escape(text)}</td>"
                    }
                }

                is PrayerTimesResult.Unavailable -> {
                    val reason =
                        when (times.reason) {
                            PrayerTimesResult.Reason.POLAR_DAY -> labels.polarDay
                            PrayerTimesResult.Reason.POLAR_NIGHT -> labels.polarNight
                        }
                    "<td colspan=\"${PrayerKind.entries.size}\">${escape(reason)}</td>"
                }
            }
        return "<tr><td>${escape(date)}</td>$cells</tr>"
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
