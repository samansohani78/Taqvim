/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Jdn
import kotlinx.datetime.LocalDate

/**
 * The days the Astronomy screen offers: Gregorian years −9999‥9999. This is the astronomy engine's working range, not
 * a data table: cosinekitty/astronomy 2.1.19 stops finding equinoxes, solstices and eclipses a little beyond it
 * (measured 2026-09-15: from about year 12 147 and −10 722 its searches throw, and its angle normalisation slows
 * without bound far from J2000), and `AstronomyRangeTest` checks the screen across the whole range. Wider years need
 * work in the engine rather than a larger constant.
 */
internal object AstronomyDays {
    private const val LAST_YEAR = 9_999
    private const val DECEMBER = 12
    private const val LAST_DAY_OF_DECEMBER = 31

    /** The first day offered. */
    val FIRST: Jdn = LocalDate(-LAST_YEAR, 1, 1).toJdn()

    /** The last day offered. */
    val LAST: Jdn = LocalDate(LAST_YEAR, DECEMBER, LAST_DAY_OF_DECEMBER).toJdn()

    /** [day] limited to [FIRST]‥[LAST]. */
    fun clamp(day: Jdn): Jdn = day.coerceIn(FIRST, LAST)

    /** The whole years of [calendar] inside [FIRST]‥[LAST]. */
    fun years(calendar: CalendarArithmetic): IntRange = CalendarLimits.years(calendar, FIRST, LAST)
}
