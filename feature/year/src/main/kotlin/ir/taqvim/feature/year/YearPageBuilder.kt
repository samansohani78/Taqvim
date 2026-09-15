/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.runtime.Immutable
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Texts of a year page in the app language; the UI resolves them from string resources (T-805). */
class YearTexts(
    /** A month name followed by its year, e.g. "Farvardin 1405". */
    val monthTitle: (month: String, year: String) -> String,
    /** Two years as a range, e.g. "2026 – 2027". */
    val range: (first: String, last: String) -> String,
    val today: String,
    /** The number of holidays of a month, [formattedCount] being the count in the language's digits. */
    val holidays: (count: Int, formattedCount: String) -> String,
    /** Joins the parts of a spoken month summary and the calendars of the subtitle. */
    val separator: String,
)

/** How the number of a day in a mini month is drawn. */
enum class MiniDayTone {
    NORMAL,

    /** An official holiday or a weekend day. */
    OFF_DAY,
}

/** One day of a mini month. */
@Immutable
data class MiniDay(
    val label: String,
    val tone: MiniDayTone,
    val isToday: Boolean,
)

/** One month of a year page. */
@Immutable
data class MiniMonth(
    val firstDay: Jdn,
    val name: String,
    /** Spoken summary: the month and year, today, and the number of holidays. */
    val description: String,
    /** [YearPageBuilder.CELLS] cells, row by row from the week start; `null` outside the month. */
    val cells: ImmutableList<MiniDay?>,
)

/** Title (the year in the shown calendar) and subtitle (the same days' years in the other calendars). */
@Immutable
data class YearHeading(
    val title: String,
    val subtitle: String?,
)

/** A year page ready to draw (T-805). */
@Immutable
data class YearPage(
    val calendarIndex: Int,
    val year: Int,
    val heading: YearHeading,
    /** Narrow weekday names in column order, from the week start. */
    val weekdayLabels: ImmutableList<String>,
    val months: ImmutableList<MiniMonth>,
)

/**
 * Builds [YearPage]s (T-805), meant to run off the main thread: every month of a year as a six-week grid with day
 * numbers in the language's digits, today, holidays and weekends, and a spoken summary per month.
 */
class YearPageBuilder(
    private val calendars: YearCalendars,
    private val language: LanguageSpec,
    private val texts: YearTexts,
    /** Narrow standalone weekday names in ISO order (Monday first). */
    private val weekdayNames: List<String>,
) {
    private val weekStart = calendars.settings.weekStart

    init {
        require(weekdayNames.size == DAYS_PER_WEEK) { "7 weekday names are required (got $weekdayNames)" }
    }

    /** The year of calendar [index] that contains [day]. */
    fun yearOf(
        index: Int,
        day: Jdn,
    ): Int = calendars.yearOf(index, day)

    /** The years calendar [index] offers. */
    fun years(index: Int): IntRange = calendars.years(index)

    /** [year] of calendar [index]; [days] are its days' flags, `null` while loading (weekends follow the language). */
    fun build(
        index: Int,
        year: Int,
        today: Jdn,
        days: List<YearDay>?,
    ): YearPage {
        val calendar = calendars.arithmetic[index]
        val flags = days.orEmpty().associateBy { it.jdn }
        val starts = calendars.monthStarts(index, year)
        return YearPage(
            calendarIndex = index,
            year = calendar.fromJdn(starts.first()).year,
            heading = heading(index, year),
            weekdayLabels = List(DAYS_PER_WEEK) { weekdayNames[(weekStart + it).ordinal] }.toImmutableList(),
            months = starts.map { month(calendar, it, today, flags) }.toImmutableList(),
        )
    }

    /** Heading of [year] of calendar [index]. */
    fun heading(
        index: Int,
        year: Int,
    ): YearHeading {
        val days = calendars.yearDays(index, year)
        val others =
            calendars.arithmetic.indices.filter { it != index }.map { other ->
                val first = calendars.arithmetic[other].fromJdn(days.start).year
                val last = calendars.arithmetic[other].fromJdn(days.endInclusive).year
                if (first == last) number(first) else texts.range(number(first), number(last))
            }
        val shown = calendars.arithmetic[index].fromJdn(days.start).year
        return YearHeading(number(shown), others.takeIf { it.isNotEmpty() }?.joinToString(texts.separator))
    }

    /** [value] in the language's digits. */
    fun number(value: Int): String = Numerals.format(value.toLong(), language.numerals)

    private fun month(
        calendar: CalendarArithmetic,
        first: Jdn,
        today: Jdn,
        flags: Map<Jdn, YearDay>,
    ): MiniMonth {
        val date = calendar.fromJdn(first)
        val length = calendar.monthLength(date.year, date.month)
        val lead = first.weekday().daysAfter(weekStart)
        val cells =
            List(CELLS) { cell ->
                (cell - lead).takeIf { it in 0 until length }?.let { day(first + it, it + 1, today, flags) }
            }
        val name = monthName(date)
        val holidays = (0 until length).count { flags[first + it]?.isHoliday == true }
        val description =
            listOfNotNull(
                texts.monthTitle(name, number(date.year)),
                texts.today.takeIf { today in first..(first + (length - 1)) },
                texts.holidays(holidays, number(holidays)).takeIf { holidays > 0 },
            ).joinToString(texts.separator)
        return MiniMonth(first, name, description, cells.toImmutableList())
    }

    private fun day(
        jdn: Jdn,
        dayOfMonth: Int,
        today: Jdn,
        flags: Map<Jdn, YearDay>,
    ): MiniDay {
        val flag = flags[jdn]
        val offDay = if (flag == null) jdn.weekday() in language.weekend else flag.isHoliday || flag.isWeekend
        return MiniDay(number(dayOfMonth), if (offDay) MiniDayTone.OFF_DAY else MiniDayTone.NORMAL, jdn == today)
    }

    /** The month's name in the language, or its number where the language has no names for that calendar. */
    private fun monthName(date: CalendarDate): String =
        language.monthNames.forSystem(date.system)?.getOrNull(date.month - 1) ?: number(date.month)

    companion object {
        const val DAYS_PER_WEEK: Int = 7
        const val WEEKS: Int = 6
        const val CELLS: Int = DAYS_PER_WEEK * WEEKS
    }
}
