/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.WeekNumberModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Texts of a month page in the app language; the UI resolves them from string resources (T-801). */
class MonthTexts(
    /** A month name followed by its year, e.g. "Farvardin 1405". */
    val monthTitle: (month: String, year: String) -> String,
    /** Two months (or month titles) as a range. */
    val monthRange: (first: String, last: String) -> String,
    val today: String,
    val holiday: String,
    /** The number of events of a day, [formattedCount] being the count in the language's digits. */
    val events: (count: Int, formattedCount: String) -> String,
    /** Joins the parts of a spoken day summary and the calendars of the subtitle. */
    val separator: String,
    /** Spoken name of the long-press action of a day. */
    val newEvent: String,
    /** Spoken form of a week number. */
    val week: (number: String) -> String,
)

/** Colors of the event dots under a day number. */
data class IndicatorPalette(
    val holiday: Color,
    val official: Color,
    val personal: Color,
    val device: Color,
    val subscription: Color,
)

/** Title (the month in the primary calendar) and subtitle (the same days in the other calendars) of a month. */
@Immutable
data class MonthHeading(
    val title: String,
    val subtitle: String?,
)

/** A month page ready to draw (T-801). */
@Immutable
data class MonthPage(
    val offset: Int,
    val heading: MonthHeading,
    val grid: MonthGridModel,
    /** The day of each cell of [grid], in cell order. */
    val days: ImmutableList<Jdn>,
    /** Index of the selected day in [days], or -1 when it is not on this page. */
    val selectedIndex: Int,
)

/**
 * Builds [MonthPage]s (T-801), meant to run off the main thread: day numbers in the language's digits, the day in the
 * other calendars, event dots, holidays and weekends, week numbers and the spoken summary of every day.
 */
class MonthPageBuilder(
    private val calendars: CalendarCalendars,
    private val language: LanguageSpec,
    private val texts: MonthTexts,
    private val palette: IndicatorPalette,
    /** Short standalone weekday names in ISO order (Monday first). */
    private val weekdayNames: List<String>,
) {
    private val primary = calendars.arithmetic.first()
    private val weekStart = calendars.settings.weekStart

    init {
        require(weekdayNames.size == MonthLayout.DAYS_PER_WEEK) { "7 weekday names are required (got $weekdayNames)" }
    }

    /** The page [offset] months from the month of [today]; [events] are the grid days' events, `null` while loading. */
    fun build(
        offset: Int,
        today: Jdn,
        selected: Jdn,
        events: List<CalendarDay>?,
    ): MonthPage {
        val monthStart = calendars.monthStartAt(today, offset)
        val month = primary.fromJdn(monthStart)
        val days = MonthLayout.gridDays(monthStart, weekStart).toList()
        val byDay = events.orEmpty().associateBy { it.jdn }
        val grid =
            MonthGridModel(
                weekdayLabels = List(MonthLayout.DAYS_PER_WEEK) { weekdayNames[(weekStart + it).ordinal] },
                cells = days.map { cell(it, month, today, selected, byDay[it]) },
                weekNumbers = if (calendars.settings.showWeekNumbers) weekNumbers(days, month) else null,
                longClickLabel = texts.newEvent,
            )
        return MonthPage(offset, heading(month), grid, days.toImmutableList(), days.indexOf(selected))
    }

    /** Heading of the month containing [month] (a date of the primary calendar). */
    fun heading(month: CalendarDate): MonthHeading {
        val first = primary.toJdn(primary.date(month.year, month.month, 1))
        val last = first + (primary.monthLength(month.year, month.month) - 1)
        val firstDates = calendars.datesOf(first)
        val lastDates = calendars.datesOf(last)
        val others = firstDates.indices.drop(1).map { span(firstDates[it], lastDates[it]) }
        return MonthHeading(
            monthTitle(firstDates.first()),
            others.takeIf { it.isNotEmpty() }?.joinToString(texts.separator),
        )
    }

    private fun cell(
        day: Jdn,
        month: CalendarDate,
        today: Jdn,
        selected: Jdn,
        events: CalendarDay?,
    ): DayCellModel {
        val dates = calendars.datesOf(day)
        val date = dates.first()
        val weekday = day.weekday()
        val officialHoliday = events?.isHoliday == true
        val weekend = events?.isWeekend ?: (weekday in language.weekend)
        return DayCellModel(
            dayLabel = number(date.day),
            contentDescription = description(date, weekday, day == today, officialHoliday, events?.events?.size ?: 0),
            secondaryLabels = dates.drop(1).take(MAX_SECONDARY_DATES).map { number(it.day) },
            indicators =
                events
                    ?.events
                    .orEmpty()
                    .map(::color)
                    .distinct()
                    .take(MAX_EVENT_DOTS),
            isToday = day == today,
            isSelected = day == selected,
            isHoliday = officialHoliday || weekend,
            inCurrentMonth = date.year == month.year && date.month == month.month,
        )
    }

    private fun description(
        date: CalendarDate,
        weekday: Weekday,
        isToday: Boolean,
        isHoliday: Boolean,
        eventCount: Int,
    ): String =
        listOfNotNull(
            DateFormatter.format(date, weekday, language, DateStyle.LONG),
            texts.today.takeIf { isToday },
            texts.holiday.takeIf { isHoliday },
            if (eventCount > 0) texts.events(eventCount, number(eventCount)) else null,
        ).joinToString(texts.separator)

    private fun color(event: DayEventItem): Color =
        if (event.isHoliday) {
            palette.holiday
        } else {
            when (event.kind) {
                DayEventKind.OFFICIAL -> palette.official
                DayEventKind.PERSONAL -> palette.personal
                DayEventKind.DEVICE -> palette.device
                DayEventKind.SUBSCRIPTION -> palette.subscription
            }
        }

    /** One label per row: the week of the year of the row's first day in the month (its first day otherwise). */
    private fun weekNumbers(
        days: List<Jdn>,
        month: CalendarDate,
    ): List<WeekNumberModel> =
        days.chunked(MonthLayout.DAYS_PER_WEEK).map { week ->
            val anchor = week.firstOrNull { inMonth(it, month) } ?: week.first()
            val label = number(MonthLayout.weekOfYear(anchor, primary, weekStart))
            WeekNumberModel(label, texts.week(label))
        }

    private fun inMonth(
        day: Jdn,
        month: CalendarDate,
    ): Boolean = primary.fromJdn(day).let { it.year == month.year && it.month == month.month }

    /** The months from [first] to [last] of one calendar, sharing the year when they have the same one. */
    private fun span(
        first: CalendarDate,
        last: CalendarDate,
    ): String =
        when {
            first.year != last.year -> {
                texts.monthRange(monthTitle(first), monthTitle(last))
            }

            first.month != last.month -> {
                texts.monthTitle(
                    texts.monthRange(monthName(first), monthName(last)),
                    year(first),
                )
            }

            else -> {
                monthTitle(first)
            }
        }

    private fun monthTitle(date: CalendarDate): String = texts.monthTitle(monthName(date), year(date))

    /** The month's name in the language, or its number where the language has no names for that calendar. */
    private fun monthName(date: CalendarDate): String =
        language.monthNames.forSystem(date.system)?.getOrNull(date.month - 1) ?: number(date.month)

    private fun year(date: CalendarDate): String = number(date.year)

    private fun number(value: Int): String = Numerals.format(value.toLong(), language.numerals)

    private companion object {
        /** Other calendars shown under a day number. */
        const val MAX_SECONDARY_DATES = 2

        /** Event dots under a day number. */
        const val MAX_EVENT_DOTS = 3
    }
}
