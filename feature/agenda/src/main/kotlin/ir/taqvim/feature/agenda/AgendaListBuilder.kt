/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** The rows of the loaded months and where today is among them. */
data class AgendaList(
    val items: ImmutableList<AgendaItem>,
    /** Index of today's row in [items], or -1. */
    val todayIndex: Int,
)

/**
 * Builds the rows of the month list and agenda (T-901), meant to run off the main thread: a header per month of the
 * primary calendar (with the months of the other calendars it spans), then its days in the language's dates and digits.
 */
class AgendaListBuilder(
    private val calendars: AgendaCalendars,
    val language: LanguageSpec,
) {
    private val formats = FormatTable.of(language)

    /** The rows of the months of [window] around [today]; [days] are the loaded days with their events. */
    fun build(
        today: Jdn,
        window: AgendaWindow,
        days: List<AgendaDay>,
        mode: AgendaMode,
    ): AgendaList {
        val byDay = days.associateBy { it.jdn }
        val items =
            (window.first..window.last).flatMap { offset ->
                val start = calendars.monthStartAt(today, offset)
                val end = calendars.monthStartAt(today, offset + 1) - 1
                month(offset, JdnRange(start, end), today, byDay, mode)
            }
        return AgendaList(
            items.toImmutableList(),
            items.indexOfFirst { it is AgendaDayRow && it.jdn == today },
        )
    }

    private fun month(
        offset: Int,
        range: JdnRange,
        today: Jdn,
        byDay: Map<Jdn, AgendaDay>,
        mode: AgendaMode,
    ): List<AgendaItem> {
        val rows =
            range
                .filter { day -> mode == AgendaMode.MONTH_LIST || day == today || !byDay[day]?.events.isNullOrEmpty() }
                .map { day(it, today, byDay[it]) }
        val header = header(offset, range)
        return when {
            rows.isNotEmpty() -> listOf(header) + rows
            else -> listOf(header, AgendaEmptyMonth(offset))
        }
    }

    private fun header(
        offset: Int,
        range: JdnRange,
    ): AgendaMonthHeader {
        val firstDates = calendars.datesOf(range.start)
        val lastDates = calendars.datesOf(range.endInclusive)
        val others =
            calendars.arithmetic.indices.drop(1).map { index ->
                val arithmetic = calendars.arithmetic[index]
                MonthSpan(monthName(arithmetic, firstDates[index]), monthName(arithmetic, lastDates[index]))
            }
        val title = monthName(calendars.arithmetic.first(), firstDates.first())
        return AgendaMonthHeader(offset, title, others.toImmutableList())
    }

    private fun day(
        jdn: Jdn,
        today: Jdn,
        events: AgendaDay?,
    ): AgendaDayRow {
        val dates = calendars.datesOf(jdn)
        val weekday = jdn.weekday()
        return AgendaDayRow(
            jdn = jdn,
            dayNumber = digits(dates.first().day.toString()),
            longDate = DateFormatter.format(dates.first(), weekday, language, DateStyle.LONG),
            otherDates =
                dates
                    .drop(1)
                    .map { DateFormatter.format(it, weekday, language, DateStyle.NUMERIC) }
                    .toImmutableList(),
            isToday = jdn == today,
            isHoliday = events?.isHoliday == true,
            isWeekend = events?.isWeekend ?: (weekday in language.weekend),
            events = events?.events.orEmpty().toImmutableList(),
        )
    }

    /** The month of [date] by name in the language, or by number when the language has no names for its calendar. */
    private fun monthName(
        arithmetic: CalendarArithmetic,
        date: CalendarDate,
    ): MonthName {
        val name = formats.monthNames[arithmetic.system]?.getOrNull(date.month - 1) ?: digits(date.month.toString())
        return MonthName(name, digits(date.year.toString()))
    }

    private fun digits(text: String): String = Numerals.localizeDigits(text, language.numerals)

    companion object {
        /** The launch language with [code], or the first launch language when [code] is unknown. */
        fun languageFor(code: String): LanguageSpec = LanguageTable.forCode(code) ?: LanguageTable.languages.first()
    }
}
