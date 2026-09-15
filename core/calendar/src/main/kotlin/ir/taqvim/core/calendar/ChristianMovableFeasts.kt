/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
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
 * Gregorian dates of every [MovableFeast] in a year (T-109), for every year from [GregorianComputus.FIRST_YEAR]
 * through [GregorianComputus.LAST_YEAR]: all feasts fall inside their own year, so no date leaves the [Int] range.
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
        return GregorianCalendarSystem.fromJdn(earliest + (Weekday.SUNDAY.ordinal - earliest.weekday().ordinal))
    }
}
