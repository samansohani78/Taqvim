/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

/**
 * Official Iranian lunar month starts from Ramadan 1446 to Shawwal 1448, from the University of Tehran Calendar
 * Center's official calendars of 1404 and 1405 SH (docs/sources; golden fixture
 * `islamic-iran/official-month-starts-1446-1448.csv`). Extended by D-07 as further official calendars are published.
 */
public object IranOfficialMonthStarts {
    /** JDN of 1 Ramadan 1446 (2 March 2025). */
    private const val FIRST_START_JDN = 2_460_737L

    /** Published months from Ramadan 1446 to Ramadan 1448; the table ends at 1 Shawwal 1448. */
    public val TABLE: IslamicMonthTable =
        IslamicMonthTable(
            firstYear = 1446,
            firstMonth = 9,
            firstStartJdn = FIRST_START_JDN,
            lengths =
                listOf(
                    // Ramadan 1446 … Dhu al-Hijjah 1446
                    29,
                    29,
                    29,
                    30,
                    // Muharram 1447 … Dhu al-Hijjah 1447
                    29,
                    30,
                    30,
                    29,
                    30,
                    30,
                    30,
                    29,
                    30,
                    29,
                    29,
                    29,
                    // Muharram 1448 … Ramadan 1448
                    30,
                    29,
                    30,
                    29,
                    30,
                    30,
                    30,
                    29,
                    30,
                ),
        )
}
