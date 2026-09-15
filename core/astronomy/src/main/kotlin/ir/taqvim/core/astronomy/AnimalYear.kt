/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate

/** The twelve animals of the Chinese year cycle, in cycle order. */
public enum class ChineseZodiacAnimal {
    RAT,
    OX,
    TIGER,
    RABBIT,
    DRAGON,
    SNAKE,
    HORSE,
    GOAT,
    MONKEY,
    ROOSTER,
    DOG,
    PIG,
}

/**
 * The 12-year animal cycle (T-406), anchored on the Chinese year that began on 25 January 2020, the Year of the Rat
 * (Hong Kong Observatory, Gregorian-Lunar Calendar Conversion Table 2020,
 * https://www.hko.gov.hk/en/gts/time/conversion.htm; [ChineseNewYear] computes that day).
 *
 * A Chinese year begins at its New Year (late January to mid-February), so a date before [ChineseNewYear] belongs to
 * the previous year's animal.
 */
public object AnimalYear {
    private const val ANCHOR_GREGORIAN_YEAR = 2020
    private const val CYCLE_YEARS = 12

    /** Animal of the Chinese year that begins (at its lunar new year) during Gregorian [gregorianYear]. */
    public fun ofChineseYearStartingIn(gregorianYear: Int): ChineseZodiacAnimal =
        ChineseZodiacAnimal.entries[Math.floorMod(gregorianYear - ANCHOR_GREGORIAN_YEAR, CYCLE_YEARS)]

    /** Animal of the Chinese year that [date] (Gregorian) belongs to, switching on [ChineseNewYear]. */
    public fun forDate(date: CalendarDate): ChineseZodiacAnimal {
        val day = GregorianCalendarSystem.toJdn(date)
        val startYear = if (day < ChineseNewYear.day(date.year)) date.year - 1 else date.year
        return ofChineseYearStartingIn(startYear)
    }
}
