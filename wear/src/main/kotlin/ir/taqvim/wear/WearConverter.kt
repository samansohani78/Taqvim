/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem

/** The part of a date a converter step changes. */
enum class ConverterField {
    YEAR,
    MONTH,
    DAY,
}

/** A converted date: the calendar and the date written in it. */
data class ConvertedDate(
    val system: CalendarSystem,
    val text: String,
)

/** Date conversion on the watch: step a date in one calendar and read it in the others (T-1600). */
object WearConverter {
    /**
     * [date] with [field] moved by [delta], wrapping months and days within their year and month and clamping days; years
     * stay within the calendar's [CalendarLimits.years].
     */
    fun step(
        calendar: CalendarArithmetic,
        date: CalendarDate,
        field: ConverterField,
        delta: Int,
    ): CalendarDate =
        when (field) {
            ConverterField.YEAR -> {
                val years = CalendarLimits.years(calendar)
                val year = (date.year.toLong() + delta).coerceIn(years.first.toLong(), years.last.toLong()).toInt()
                clamp(calendar, year, date.month, date.day)
            }

            ConverterField.MONTH -> {
                val months = calendar.monthsInYear(date.year)
                clamp(calendar, date.year, Math.floorMod(date.month - 1 + delta, months) + 1, date.day)
            }

            ConverterField.DAY -> {
                val length = calendar.monthLength(date.year, date.month)
                calendar.date(date.year, date.month, Math.floorMod(date.day - 1 + delta, length) + 1)
            }
        }

    /** The same day as [date] (in [from]) written in [to]. */
    fun switchCalendar(
        from: CalendarArithmetic,
        to: CalendarArithmetic,
        date: CalendarDate,
    ): CalendarDate = to.fromJdn(from.toJdn(date))

    /** [date] of [source] in every calendar of [setup] except [source]. */
    fun results(
        setup: WearSetup,
        source: CalendarArithmetic,
        date: CalendarDate,
    ): List<ConvertedDate> {
        val jdn = source.toJdn(date)
        return setup.calendars.filter { it.system != source.system }.map { calendar ->
            ConvertedDate(
                calendar.system,
                DateFormatter.format(
                    calendar.fromJdn(jdn),
                    jdn.weekday(),
                    setup.language,
                    DateStyle.LONG,
                    setup.numerals,
                ),
            )
        }
    }

    private fun clamp(
        calendar: CalendarArithmetic,
        year: Int,
        month: Int,
        day: Int,
    ): CalendarDate {
        val safeMonth = month.coerceAtMost(calendar.monthsInYear(year))
        return calendar.date(year, safeMonth, day.coerceAtMost(calendar.monthLength(year, safeMonth)))
    }
}
