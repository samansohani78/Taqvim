/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange

/**
 * The calendars of [settings] that are available and the years of each (T-805). Calendars are addressed by their
 * index in [systems]; each calendar offers every year the app can show a day of ([CalendarLimits.years]).
 */
class YearCalendars(
    val settings: YearSettings,
) {
    /** Arithmetic of the available calendars, in the order of [systems]. */
    val arithmetic: List<CalendarArithmetic> =
        settings.calendars
            .distinct()
            .map { arithmeticFor(it, settings.islamicVariant, settings.islamicOverrides) }
            .ifEmpty { listOf(GregorianCalendarSystem) }

    /** The user's calendars in their order; Gregorian when the user has none. */
    val systems: List<CalendarSystem> = arithmetic.map { it.system }

    private val offeredYears: List<IntRange> = arithmetic.map(CalendarLimits::years)

    /** [index] limited to the available calendars. */
    fun clampIndex(index: Int): Int = index.coerceIn(0, arithmetic.lastIndex)

    /** The years calendar [index] offers. */
    fun years(index: Int): IntRange = offeredYears[index]

    /** The year of calendar [index] that contains [day], limited to [years]. */
    fun yearOf(
        index: Int,
        day: Jdn,
    ): Int = arithmetic[index].fromJdn(day).year.coerceIn(years(index))

    /** The first day of [year] (limited to [years]) in calendar [index]. */
    fun yearStart(
        index: Int,
        year: Int,
    ): Jdn {
        val calendar = arithmetic[index]
        return calendar.toJdn(calendar.date(year.coerceIn(years(index)), 1, 1))
    }

    /** Every day of [year] (limited to [years]) in calendar [index]. */
    fun yearDays(
        index: Int,
        year: Int,
    ): JdnRange {
        val calendar = arithmetic[index]
        val shown = year.coerceIn(years(index))
        val lastMonth = calendar.monthsInYear(shown)
        val last = calendar.toJdn(calendar.date(shown, lastMonth, calendar.monthLength(shown, lastMonth)))
        return yearStart(index, shown)..last
    }

    /** The first day of every month of [year] (limited to [years]) in calendar [index]. */
    fun monthStarts(
        index: Int,
        year: Int,
    ): List<Jdn> {
        val calendar = arithmetic[index]
        val shown = year.coerceIn(years(index))
        return (1..calendar.monthsInYear(shown)).map { calendar.toJdn(calendar.date(shown, it, 1)) }
    }

    companion object {
        /** Years the year selection lists on each side of the shown year; picking a year near an end moves the list. */
        const val PICKER_SPAN: Int = 1_000

        /**
         * The years the year selection lists around [year]: [PICKER_SPAN] each way, limited to [years]. The first listed
         * year is a whole number of [columns] after the first offered year, so a grid's rows stay put as the list moves.
         */
        fun pickerYears(
            year: Int,
            years: IntRange,
            columns: Int = 1,
        ): IntRange {
            val shown = year.coerceIn(years).toLong()
            val wanted = maxOf(years.first.toLong(), shown - PICKER_SPAN)
            val first = years.first + Math.floorDiv(wanted - years.first, columns.toLong()) * columns
            val last = minOf(years.last.toLong(), shown + PICKER_SPAN)
            return first.toInt()..last.toInt()
        }

        /** Arithmetic of [system], with [variant] and the optional official [overrides] for the Islamic calendar. */
        fun arithmeticFor(
            system: CalendarSystem,
            variant: IslamicVariant,
            overrides: IslamicMonthTable? = null,
        ): CalendarArithmetic = IslamicCalendarSelection.arithmeticFor(system, variant, overrides)
    }
}
