/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

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
 * https://www.hko.gov.hk/en/gts/time/conversion.htm).
 *
 * Only the cycle position is modelled: the Chinese year begins at the lunar new year (late January to mid-February),
 * which needs a lunisolar calendar that Taqvim does not implement, so a date early in a Gregorian year may still belong
 * to the previous animal (docs/DATA_TODO.md).
 */
public object AnimalYear {
    private const val ANCHOR_GREGORIAN_YEAR = 2020
    private const val CYCLE_YEARS = 12

    /** Animal of the Chinese year that begins (at its lunar new year) during Gregorian [gregorianYear]. */
    public fun ofChineseYearStartingIn(gregorianYear: Int): ChineseZodiacAnimal =
        ChineseZodiacAnimal.entries[Math.floorMod(gregorianYear - ANCHOR_GREGORIAN_YEAR, CYCLE_YEARS)]
}
