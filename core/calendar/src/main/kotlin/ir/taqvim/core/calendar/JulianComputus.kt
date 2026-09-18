/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn

/**
 * Easter Sunday of the Julian calendar (the computus used before the 1582 reform and still by the Orthodox churches),
 * by the algorithm for the Julian calendar in J. Meeus, Astronomical Algorithms (2nd ed.), ch. 8 "Date of Easter"
 * (T-109).
 * The rule is periodic — the dates repeat every [CYCLE_YEARS] years — so it is continued proleptically, with floored
 * remainders, to every year the [JulianCalendar] holds, [JulianCalendar.MIN_YEAR]..[JulianCalendar.MAX_YEAR];
 * other years are rejected with [IllegalArgumentException]. Historically the rule applies from the Dionysian tables
 * (6th century) to the reform.
 */
public object JulianComputus {
    /** Years after which the Julian Easter dates repeat (19-year lunar cycle × 28-year solar cycle). */
    public const val CYCLE_YEARS: Long = 532L

    private const val DAYS_IN_MARCH = 31

    /** Easter Sunday of Julian [year] as a Julian date. */
    public fun easter(year: Long): JulianDate {
        requireInCalendarRange(year in JulianCalendar.MIN_YEAR..JulianCalendar.MAX_YEAR) {
            "Julian Easter needs a year in ${JulianCalendar.MIN_YEAR}..${JulianCalendar.MAX_YEAR} (was $year)"
        }
        val monthAndDay = monthAndDayNumber(year)
        return JulianDate(year, monthAndDay / DAYS_IN_MARCH, monthAndDay % DAYS_IN_MARCH + 1)
    }

    /** Day number of Julian Easter of [year]. */
    public fun easterJdn(year: Long): Jdn = JulianCalendar.toJdn(easter(year))

    /**
     * The last year whose Orthodox Easter still falls in an [Int] Gregorian year: Julian dates run about 44 000
     * Gregorian years ahead by then.
     */
    public const val ORTHODOX_LAST_YEAR: Int = 2_147_439_551

    /**
     * Orthodox Easter of [year]: the Julian computus, expressed as a Gregorian date. Defined from
     * [GregorianComputus.FIRST_YEAR], when the two calendars diverged, through [ORTHODOX_LAST_YEAR].
     */
    public fun orthodoxEaster(year: Int): CalendarDate {
        requireInCalendarRange(year in GregorianComputus.FIRST_YEAR..ORTHODOX_LAST_YEAR) {
            "Orthodox Easter needs a year in ${GregorianComputus.FIRST_YEAR}..$ORTHODOX_LAST_YEAR (was $year)"
        }
        return GregorianCalendarSystem.fromJdn(easterJdn(year.toLong()))
    }

    // The numeric constants are the published coefficients of Meeus's algorithm; naming them would obscure the
    // correspondence with the book, so MagicNumber is suppressed for this function only.

    /** Meeus's `d + e + 114`: month × 31 + (day − 1). Remainders are floored so negative years stay periodic. */
    @Suppress("MagicNumber")
    private fun monthAndDayNumber(year: Long): Int {
        val a = Math.floorMod(year, 4L)
        val b = Math.floorMod(year, 7L)
        val c = Math.floorMod(year, 19L)
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        return (d + e + 114).toInt()
    }
}
