/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/**
 * Western Christian feasts whose Gregorian date moves from year to year (T-109). [daysFromEaster] is the liturgical
 * distance from Easter Sunday; it is null for [FIRST_SUNDAY_OF_ADVENT], which is tied to Christmas instead.
 */
public enum class MovableFeast(
    public val daysFromEaster: Int?,
) {
    /** Start of Lent, 46 days before Easter. */
    ASH_WEDNESDAY(-46),

    /** The Sunday before Easter. */
    PALM_SUNDAY(-7),

    /** The Friday before Easter. */
    GOOD_FRIDAY(-2),

    /** Easter Sunday itself ([GregorianComputus]). */
    EASTER(0),

    /** The 40th day of Easter, a Thursday. */
    ASCENSION_DAY(39),

    /** Whit Sunday, the 50th day of Easter. */
    PENTECOST(49),

    /** The Sunday after Pentecost. */
    TRINITY_SUNDAY(56),

    /** The fourth Sunday before Christmas: the Sunday from 27 November to 3 December. */
    FIRST_SUNDAY_OF_ADVENT(null),
}

/**
 * Dates of every [MovableFeast] in a year (T-109). [forYear] gives Gregorian dates for every year from
 * [GregorianComputus.FIRST_YEAR] through [GregorianComputus.LAST_YEAR] (all feasts fall inside their own year, so no
 * date leaves the [Int] range); [julianForYear] gives Julian dates for any Julian year; [civilForYear] gives the days
 * in the calendar in force, Julian before the 1582 reform and Gregorian after it, for every supported year.
 */
public object ChristianMovableFeasts {
    private const val NOVEMBER = 11
    private const val EARLIEST_ADVENT_DAY = 27

    /** Every [MovableFeast] of Gregorian [year] in enum order; throws before [GregorianComputus.FIRST_YEAR]. */
    public fun forYear(year: Int): Map<MovableFeast, CalendarDate> {
        val easter = GregorianCalendarSystem.toJdn(GregorianComputus.easter(year))
        return MovableFeast.entries.associateWith { feast ->
            feast.daysFromEaster?.let { GregorianCalendarSystem.fromJdn(easter + it) } ?: firstSundayOfAdvent(year)
        }
    }

    /** The Sunday on or after 27 November of [year]. */
    public fun firstSundayOfAdvent(year: Int): CalendarDate {
        val earliest = GregorianCalendarSystem.toJdn(GregorianCalendarSystem.date(year, NOVEMBER, EARLIEST_ADVENT_DAY))
        return GregorianCalendarSystem.fromJdn(nextSunday(earliest))
    }

    /**
     * Every [MovableFeast] of Julian [year] as Julian dates, from [JulianComputus] and the Sunday on or after
     * 27 November (Julian); any year in [JulianCalendar.MIN_YEAR]..[JulianCalendar.MAX_YEAR].
     */
    public fun julianForYear(year: Long): Map<MovableFeast, JulianDate> =
        julianDays(year).mapValues { (_, jdn) -> JulianCalendar.fromJdn(jdn) }

    /**
     * Day numbers of every [MovableFeast] of [year] in the civil calendar in force on the day, the convention USNO
     * uses: Julian dates before 1582-10-15, Gregorian from then on. Easter and the feasts counted from it follow
     * [JulianComputus] through 1582 (Easter 1582 was 15 April, before the reform) and [GregorianComputus] from 1583;
     * Advent follows the Julian calendar before 1582 and the Gregorian from 1582, when the reform had already taken
     * effect. Defined for [JulianCalendar.MIN_YEAR]..[GregorianComputus.LAST_YEAR]; other years are rejected.
     */
    public fun civilForYear(year: Long): Map<MovableFeast, Jdn> {
        require(year <= GregorianComputus.LAST_YEAR) { "year must be ≤ ${GregorianComputus.LAST_YEAR} (was $year)" }
        return when {
            year >= GregorianComputus.FIRST_YEAR -> {
                forYear(year.toInt()).mapValues { (_, date) -> GregorianCalendarSystem.toJdn(date) }
            }

            year == REFORM_YEAR.toLong() -> {
                julianDays(year) + (MovableFeast.FIRST_SUNDAY_OF_ADVENT to firstSundayOfAdvent(REFORM_YEAR).jdn())
            }

            else -> {
                julianDays(year)
            }
        }
    }

    private fun julianDays(year: Long): Map<MovableFeast, Jdn> {
        val easter = JulianComputus.easterJdn(year)
        val advent = nextSunday(JulianCalendar.toJdn(JulianDate(year, NOVEMBER, EARLIEST_ADVENT_DAY)))
        return MovableFeast.entries.associateWith { feast -> feast.daysFromEaster?.let { easter + it } ?: advent }
    }

    private fun nextSunday(day: Jdn): Jdn = day + (Weekday.SUNDAY.ordinal - day.weekday().ordinal)

    private fun CalendarDate.jdn(): Jdn = GregorianCalendarSystem.toJdn(this)

    private const val REFORM_YEAR = 1_582
}
