/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange

/**
 * The calendars of [settings] that are available and the years of each (T-805). Calendars are addressed by their
 * index in [systems]; years are limited to [YEARS] in every calendar.
 */
class YearCalendars(
    val settings: YearSettings,
) {
    /** Arithmetic of the available calendars, in the order of [systems]. */
    val arithmetic: List<CalendarArithmetic> =
        settings.calendars
            .distinct()
            .mapNotNull { arithmeticFor(it, settings.islamicVariant) }
            .ifEmpty { listOf(GregorianCalendarSystem) }

    /** The available calendars in the user's order; Gregorian when none of them is available. */
    val systems: List<CalendarSystem> = arithmetic.map { it.system }

    /** [index] limited to the available calendars. */
    fun clampIndex(index: Int): Int = index.coerceIn(0, arithmetic.lastIndex)

    /** The year of calendar [index] that contains [day], limited to [YEARS]. */
    fun yearOf(
        index: Int,
        day: Jdn,
    ): Int = arithmetic[index].fromJdn(day).year.coerceIn(YEARS)

    /** The first day of [year] (limited to [YEARS]) in calendar [index]. */
    fun yearStart(
        index: Int,
        year: Int,
    ): Jdn {
        val calendar = arithmetic[index]
        return calendar.toJdn(calendar.date(year.coerceIn(YEARS), 1, 1))
    }

    /** Every day of [year] (limited to [YEARS]) in calendar [index]. */
    fun yearDays(
        index: Int,
        year: Int,
    ): JdnRange {
        val calendar = arithmetic[index]
        val shown = year.coerceIn(YEARS)
        val lastMonth = calendar.monthsInYear(shown)
        val last = calendar.toJdn(calendar.date(shown, lastMonth, calendar.monthLength(shown, lastMonth)))
        return yearStart(index, shown)..last
    }

    /** The first day of every month of [year] (limited to [YEARS]) in calendar [index]. */
    fun monthStarts(
        index: Int,
        year: Int,
    ): List<Jdn> {
        val calendar = arithmetic[index]
        val shown = year.coerceIn(YEARS)
        return (1..calendar.monthsInYear(shown)).map { calendar.toJdn(calendar.date(shown, it, 1)) }
    }

    companion object {
        /** The last year offered in every calendar. */
        const val MAX_YEAR: Int = 3000

        /** Years the year view offers in every calendar. */
        val YEARS: IntRange = 1..MAX_YEAR

        /** Arithmetic of [system], with [variant] for the Islamic calendar; `null` when not available yet (Nepali). */
        fun arithmeticFor(
            system: CalendarSystem,
            variant: IslamicVariant,
        ): CalendarArithmetic? =
            when (system) {
                CalendarSystem.PERSIAN -> PersianCalendarSystem
                CalendarSystem.ISLAMIC -> IslamicCalendarSelection.calendarFor(variant)
                CalendarSystem.GREGORIAN -> GregorianCalendarSystem
                CalendarSystem.NEPALI -> null
            }
    }
}
