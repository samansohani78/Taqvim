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

/**
 * The same twelve-animal cycle as it was used in Iran (DT-015), which is **not** aligned with [AnimalYear]'s.
 *
 * R. Abdollahy, "CALENDARS ii. In the Islamic period", *Encyclopaedia Iranica* IV/6-7, pp. 658–677, states the rule
 * outright: "add 6 to the year in question and divide by 12; the remainder … 1 = mouse, 2 = ox, 3 = tiger, and so on
 * up to 12 = pig". The year it applies to is the **Solar Hijri** year, so the animal changes at Nowruz rather than at
 * the Chinese new year, and a Gregorian year spans two of them. Checked against the years in force: 1403 is the
 * dragon, 1404 the snake, 1405 the horse. The twelve names are Melville's (1994).
 *
 * Abdollahy also dates the modern usage: the Majles adopted it in 1329/1911 and dropped it in 1344/1925, "the naming
 * of years for animals is still customary in certain Persian almanacs". [IN_FORCE_YEARS] records that, so a caller
 * can say when the naming was official rather than implying it always was.
 */
public object PersianAnimalYear {
    private const val OFFSET = 6
    private const val CYCLE_YEARS = 12

    /** Solar Hijri years in which the animal naming was official (Majles 1329 SH to its abrogation in 1344 SH). */
    public val IN_FORCE_YEARS: IntRange = 1329..1344

    /** The animal of Solar Hijri [persianYear], by Abdollahy's rule. */
    public fun of(persianYear: Int): ChineseZodiacAnimal =
        ChineseZodiacAnimal.entries[Math.floorMod(persianYear + OFFSET - 1, CYCLE_YEARS)]
}
