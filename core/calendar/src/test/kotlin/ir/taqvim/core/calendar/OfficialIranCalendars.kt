/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.testing.GoldenFile

/**
 * The official calendars of Iran imported from `docs/sources` by `tools/iran/official_calendar_import.py`, read from
 * the index the importer writes. Tests iterate this list instead of naming fixtures, so a calendar the owner adds is
 * covered as soon as the importer has run (docs/adr/0040-islamic-calibration-refit.md).
 */
internal object OfficialIranCalendars {
    private const val INDEX = "golden/islamic-iran/official-calendars.csv"
    private const val SOLAR_YEAR = 0
    private const val DAYS = 5
    private const val FIXTURE = 6

    /** One imported calendar: its Solar Hijri year, the number of days it prints and its daily fixture. */
    data class Imported(
        val solarYear: Int,
        val days: Int,
        val fixture: String,
    )

    /** Every imported calendar, oldest first. */
    val imported: List<Imported> =
        rows(INDEX).map { columns ->
            Imported(columns[SOLAR_YEAR].toInt(), columns[DAYS].toInt(), columns[FIXTURE])
        }

    /** The first day of every lunar Hijri month the imported calendars establish. */
    val monthStarts: List<List<String>> = rows("golden/islamic-iran/official-month-starts.csv")

    /** Data rows of the golden fixture at [resourcePath], split on commas, without its column line. */
    fun rows(resourcePath: String): List<List<String>> =
        GoldenFile
            .load(resourcePath)
            .lines
            .drop(1)
            .map { it.split(',') }
}
