/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem

/**
 * Easter Sunday of the Gregorian calendar (the computus of the 1582 reform), by the integer algorithm in J. Meeus,
 * Astronomical Algorithms (2nd ed.), ch. 8 "Date of Easter" (T-109). The algorithm is exact for every Gregorian year
 * from [FIRST_YEAR] on, with no upper bound of its own: the dates repeat every 5 700 000 years. Earlier years used the
 * Julian computus ([JulianComputus], [ChristianMovableFeasts.civilForYear]) and are rejected here. The only upper
 * limit is the [Int] year of [CalendarDate], [LAST_YEAR]; the largest intermediate value is below 2.2 × 10⁷, so no
 * step can overflow up to there.
 */
public object GregorianComputus {
    /** The first year whose Easter fell under the Gregorian calendar. */
    public const val FIRST_YEAR: Int = 1_583

    /** The last year a [CalendarDate] can hold; [easter] is exact through it. */
    public const val LAST_YEAR: Int = Int.MAX_VALUE

    /** Years after which the Gregorian Easter dates repeat. */
    public const val CYCLE_YEARS: Int = 5_700_000

    private const val DAYS_IN_MARCH = 31

    /** Easter Sunday of [year]; throws [IllegalArgumentException] before [FIRST_YEAR]. */
    public fun easter(year: Int): CalendarDate {
        requireInCalendarRange(year >= FIRST_YEAR) { "Gregorian Easter starts in $FIRST_YEAR (was $year)" }
        val monthAndDay = monthAndDayNumber(year)
        return CalendarDate(
            CalendarSystem.GREGORIAN,
            year,
            monthAndDay / DAYS_IN_MARCH,
            monthAndDay % DAYS_IN_MARCH + 1,
        )
    }

    // The numeric constants are the published coefficients of Meeus's algorithm; naming them would obscure the
    // correspondence with the book, so MagicNumber is suppressed for this function only.

    /** Meeus's `h + l − 7m + 114`: month × 31 + (day − 1). All intermediate values stay small for any [Int] year. */
    @Suppress("MagicNumber")
    private fun monthAndDayNumber(year: Int): Int {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        return h + l - 7 * m + 114
    }
}
