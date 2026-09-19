/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

private const val MONTHS_PER_YEAR = 12

/**
 * Conversion between a calendar's dates and [Jdn], plus the calendar's structure. Every Taqvim calendar
 * (Gregorian, Persian, Islamic variants, Nepali) implements this contract; utilities in T-106 build on it.
 */
public interface CalendarArithmetic {
    /** The calendar this arithmetic implements; dates of other systems are rejected. */
    public val system: CalendarSystem

    /** Day number of [date]; throws [IllegalArgumentException] for another system or a non-existent date. */
    public fun toJdn(date: CalendarDate): Jdn

    /** The date of [jdn] in this calendar. */
    public fun fromJdn(jdn: Jdn): CalendarDate

    /** Whether [year] has the calendar's extra (intercalary) day or month. */
    public fun isLeapYear(year: Int): Boolean

    /** Number of months in [year]. */
    public fun monthsInYear(year: Int): Int

    /**
     * The number of months in every year when it never changes — 12 in each Taqvim calendar system (Persian, Islamic,
     * Gregorian, Bikram Sambat), so month arithmetic is direct for any distance. A calendar whose years differ in
     * months must return `null`; month arithmetic then steps year by year.
     */
    public val monthsPerYear: Int?
        get() = MONTHS_PER_YEAR

    /**
     * The month of [year] in which a yearly series begun in [month] of [fromYear] falls. Calendars whose years always
     * have the same months keep the number; a calendar with an intercalary month maps the month by its name, since
     * the same number names a different month in its common and leap years.
     */
    public fun sameMonthIn(
        fromYear: Int,
        month: Int,
        year: Int,
    ): Int = month

    /** Number of days in [month] of [year]; throws [IllegalArgumentException] for an invalid month. */
    public fun monthLength(
        year: Int,
        month: Int,
    ): Int

    /** Whether [year]-[month]-[day] exists in this calendar. */
    public fun isValid(
        year: Int,
        month: Int,
        day: Int,
    ): Boolean = month in 1..monthsInYear(year) && day in 1..monthLength(year, month)

    /** The date [year]-[month]-[day] in this calendar; throws [IllegalArgumentException] when it does not exist. */
    public fun date(
        year: Int,
        month: Int,
        day: Int,
    ): CalendarDate {
        require(isValid(year, month, day)) { "$system date $year-$month-$day does not exist" }
        return CalendarDate(system, year, month, day)
    }
}
