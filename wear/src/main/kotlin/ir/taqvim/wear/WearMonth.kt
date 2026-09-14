/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/** One day of the watch month grid. */
data class WearMonthCell(
    val day: Int,
    val label: String,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val isWeekend: Boolean,
)

/** A month of the primary calendar as week rows starting on the user's week start; `null` cells are padding. */
data class WearMonth(
    val title: String,
    val weekdayLabels: List<String>,
    val weeks: List<List<WearMonthCell?>>,
)

/** Builds [WearMonth] grids for the month screen and the month tile (T-1600). */
object WearMonthBuilder {
    private const val DAYS_PER_WEEK = 7

    /** The primary-calendar month [offsetMonths] away from the month of [today]. */
    fun build(
        setup: WearSetup,
        lookup: EventLookup,
        today: Jdn,
        offsetMonths: Int = 0,
    ): WearMonth {
        val calendar = setup.primary
        val todayDate = calendar.fromJdn(today)
        val first = calendar.addMonths(calendar.date(todayDate.year, todayDate.month, 1), offsetMonths)
        val firstJdn = calendar.toJdn(first)
        val length = calendar.monthLength(first.year, first.month)
        val holidays =
            (0 until length)
                .filter { offset -> lookup.eventsOn(firstJdn + offset, setup.enabledSources).any { it.isHoliday } }
                .toSet()
        val cells =
            (0 until length).map { offset ->
                val jdn = firstJdn + offset
                WearMonthCell(
                    day = offset + 1,
                    label = digits((offset + 1).toString(), setup),
                    isToday = jdn == today,
                    isHoliday = offset in holidays,
                    isWeekend = jdn.weekday() in setup.weekend,
                )
            }
        val leading = columnOf(firstJdn.weekday(), setup.weekStart)
        val padded = List<WearMonthCell?>(leading) { null } + cells
        val weeks = padded.chunked(DAYS_PER_WEEK).map { it + List(DAYS_PER_WEEK - it.size) { null } }
        return WearMonth(
            title = monthName(first, setup) + " " + digits(first.year.toString(), setup),
            weekdayLabels = weekdayLabels(setup),
            weeks = weeks,
        )
    }

    /** Column of [weekday] in a week starting on [weekStart], `0..6`. */
    fun columnOf(
        weekday: Weekday,
        weekStart: Weekday,
    ): Int = Math.floorMod(weekday.ordinal - weekStart.ordinal, DAYS_PER_WEEK)

    private fun weekdayLabels(setup: WearSetup): List<String> {
        val names = FormatTable.of(setup.language).weekdays[setup.primary.system].orEmpty()
        return (0 until DAYS_PER_WEEK).map { column ->
            val weekday = Weekday.entries[(setup.weekStart.ordinal + column) % DAYS_PER_WEEK]
            names.getOrNull(weekday.ordinal)?.take(1).orEmpty()
        }
    }
}
