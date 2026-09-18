/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import ir.taqvim.core.testing.GoldenFile

/**
 * The official calendars of Iran imported from `docs/sources/iran` by `tools/sources/iran/official_calendar_import.py`,
 * read from the index the importer writes. Tests iterate this list instead of naming fixtures, so a calendar the owner
 * adds is covered as soon as the importer has run (docs/adr/0040-islamic-calibration-refit.md).
 */
internal object OfficialIranCalendars {
    private const val INDEX = "golden/islamic-iran/official-calendars.csv"
    private const val SOLAR_YEAR = 0
    private const val DAYS = 5
    private const val FIXTURE = 6
    private const val OVERRIDE = 7

    /**
     * One imported calendar: its Solar Hijri year, the number of days it prints, its daily fixture, and whether its
     * lunar months are part of the optional official override (ADR-0037).
     */
    data class Imported(
        val solarYear: Int,
        val days: Int,
        val fixture: String,
        val inOverride: Boolean,
    )

    /** Every imported calendar, oldest first. */
    val imported: List<Imported> =
        rows(INDEX).map { columns ->
            Imported(columns[SOLAR_YEAR].toInt(), columns[DAYS].toInt(), columns[FIXTURE], columns[OVERRIDE] == "true")
        }

    /** The imported calendars whose lunar months make up the bundled official override. */
    val override: List<Imported> = imported.filter(Imported::inOverride)

    /** The first day of every lunar Hijri month of the override calendars. */
    val monthStarts: List<List<String>> = rows("golden/islamic-iran/official-month-starts.csv")

    /**
     * The first day of every lunar Hijri month all imported calendars establish, with its basis: printed, derived
     * (counted back from the first imported day) or announced (moved by a notice in the calendar).
     */
    val history: List<List<String>> = rows("golden/islamic-iran/official-month-starts-1381-1405.csv")

    /** Data rows of the golden fixture at [resourcePath], split on commas, without its column line. */
    fun rows(resourcePath: String): List<List<String>> =
        GoldenFile
            .load(resourcePath)
            .lines
            .drop(1)
            .map { it.split(',') }
}
