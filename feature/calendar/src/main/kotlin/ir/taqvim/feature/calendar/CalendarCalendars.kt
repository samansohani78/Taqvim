/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.calendar.monthsBetween
import ir.taqvim.core.calendar.originOf
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn

/**
 * The calendars of [settings] that are available, and month paging in the primary one (T-800). Month offsets count
 * whole months of the primary calendar from the month containing today.
 */
class CalendarCalendars(
    val settings: CalendarSettings,
) {
    /** Arithmetic of the available calendars, in the order of [systems]. */
    val arithmetic: List<CalendarArithmetic> =
        settings.calendars
            .distinct()
            .map { arithmeticFor(it, settings.islamicVariant, settings.islamicOverrides) }
            .ifEmpty { listOf(GregorianCalendarSystem) }

    /** The user's calendars in their order, primary first; Gregorian when the user has none. */
    val systems: List<CalendarSystem> = arithmetic.map { it.system }

    private val primary: CalendarArithmetic = arithmetic.first()

    /** Calendars that can be secondary: every calendar except the primary, in declaration order. */
    val secondaryChoices: List<CalendarSystem> = CalendarSystem.entries.filter { it != primary.system }

    /** Days in [month] of [year] of the primary calendar, the month clamped to the months of [year]. */
    fun primaryMonthLength(
        year: Int,
        month: Int,
    ): Int = primary.monthLength(year, month.coerceIn(1, primary.monthsInYear(year)))

    /** The day [year]-[month]-[day] of the primary calendar, with the month and the day clamped to what exists. */
    fun primaryDay(
        year: Int,
        month: Int,
        day: Int,
    ): Jdn {
        val clampedMonth = month.coerceIn(1, primary.monthsInYear(year))
        return primary.toJdn(primary.date(year, clampedMonth, day.coerceIn(1, primaryMonthLength(year, clampedMonth))))
    }

    /** [day] in every available calendar, primary first. */
    fun datesOf(day: Jdn): List<CalendarDate> = arithmetic.map { it.fromJdn(day) }

    /** Where each of [datesOf] comes from: computed, an official override or a printed calendar (ADR-0037). */
    fun originsOf(day: Jdn): List<DateOrigin> = arithmetic.map { it.originOf(day) }

    /** The first day, in the primary calendar, of the month containing [day]. */
    fun monthStart(day: Jdn): CalendarDate {
        val date = primary.fromJdn(day)
        return primary.date(date.year, date.month, 1)
    }

    /** Whole primary-calendar months from the month of [today] to the month of [day]; negative before today. */
    fun monthOffset(
        today: Jdn,
        day: Jdn,
    ): Int = primary.monthsBetween(monthStart(today), monthStart(day))

    /** Years of the primary calendar the month pager reaches from the month of [today] ([CalendarLimits.pagedYears]). */
    fun pagedYears(today: Jdn): IntRange = CalendarLimits.pagedYears(primary, today)

    /** The first day of the month [offset] months after the month of [today]. */
    fun monthStartAt(
        today: Jdn,
        offset: Int,
    ): Jdn = primary.toJdn(primary.addMonths(monthStart(today), offset))

    companion object {
        /** Arithmetic of [system], with [variant] and the optional official [overrides] for the Islamic calendar. */
        fun arithmeticFor(
            system: CalendarSystem,
            variant: IslamicVariant,
            overrides: IslamicMonthTable? = null,
        ): CalendarArithmetic = IslamicCalendarSelection.arithmeticFor(system, variant, overrides)
    }
}
