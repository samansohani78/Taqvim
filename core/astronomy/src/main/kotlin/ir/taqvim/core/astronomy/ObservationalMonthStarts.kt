/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant

/**
 * Calculated-observational lunar months (A-06): a month has 29 days when, on the evening of its 29th day, Yallop's test
 * rates the new crescent at `visibleUpTo` or better at any of the given places; otherwise 30 days. The resulting table
 * plugs into `IranIslamicCalendar(table)`, which joins the Iran-calibrated crescent months of `IranCrescentCalendar`
 * outside it (docs/adr/0009-iran-islamic-calendar.md, docs/adr/0027-iran-islamic-months-computed.md).
 */
public object ObservationalMonthStarts {
    private const val MONTHS = 12
    private const val SETTLING_MONTHS = 2
    private const val DAY_OF_CRESCENT_CHECK = 29
    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_587.5
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val DEGREES_PER_TURN = 360.0

    /** Month table for [firstYear]‥[lastYear] as seen from a single [place]. */
    public fun table(
        place: Coordinates,
        firstYear: Int,
        lastYear: Int,
        visibleUpTo: CrescentVisibilityClass = CrescentVisibilityClass.C,
    ): IslamicMonthTable = table(listOf(place), firstYear, lastYear, visibleUpTo)

    /**
     * Month table for [firstYear]‥[lastYear] where the crescent counts as seen if any of [places] sees it. Months are
     * counted from the tabular calendar two months before [firstYear]; a start that is off by a day there is corrected
     * by the crescent checks before the first returned month.
     */
    public fun table(
        places: List<Coordinates>,
        firstYear: Int,
        lastYear: Int,
        visibleUpTo: CrescentVisibilityClass = CrescentVisibilityClass.C,
    ): IslamicMonthTable {
        require(places.isNotEmpty()) { "at least one observing place is needed" }
        require(firstYear <= lastYear) { "firstYear must not be after lastYear" }
        val firstTabularMonth = CalendarDate(CalendarSystem.ISLAMIC, firstYear - 1, MONTHS - SETTLING_MONTHS + 1, 1)
        var start = TabularIslamicCalendar.TYPE_II.toJdn(firstTabularMonth).value
        repeat(SETTLING_MONTHS) { start += monthLength(places, start, visibleUpTo) }
        val firstStart = start
        val lengths =
            List((lastYear - firstYear + 1) * MONTHS) {
                monthLength(places, start, visibleUpTo).also { start += it }
            }
        return IslamicMonthTable(firstYear, 1, firstStart, lengths)
    }

    /** 29 or 30: whether any place sees the crescent on the evening of day 29 of the month starting at [startJdn]. */
    private fun monthLength(
        places: List<Coordinates>,
        startJdn: Long,
        visibleUpTo: CrescentVisibilityClass,
    ): Int {
        val checkDay = startJdn + DAY_OF_CRESCENT_CHECK - 1
        val seen =
            places.any { place ->
                Yallop.evening(place, localNoon(checkDay, place))?.let { it.visibility <= visibleUpTo } ?: false
            }
        return if (seen) DAY_OF_CRESCENT_CHECK else DAY_OF_CRESCENT_CHECK + 1
    }

    /** Mean local noon at [place] on the civil day [jdn]. */
    private fun localNoon(
        jdn: Long,
        place: Coordinates,
    ): Instant {
        val julianDay = jdn - place.longitude / DEGREES_PER_TURN
        return Instant.fromEpochMilliseconds(((julianDay - JULIAN_DAY_OF_UNIX_EPOCH) * MILLIS_PER_DAY).toLong())
    }
}

/**
 * A calibration of A-06 against the Calendar Center's published month starts (Ramadan 1446 – Shawwal 1448): the
 * crescent counts as seen when any of five reference cities across Iran rates it at class D or better. This is a fit
 * to the available official data, not a description of the official procedure; re-validate when further official
 * calendars are added (ADR-0009 addendum). `IranCrescentCalendar` in `:core:calendar` computes the same months for
 * every year (ADR-0027); `ObservationalMonthStartsTest` keeps the two equal.
 */
public object IranCrescentCalibration {
    /** Tehran, Mashhad, Zahedan, Bandar Abbas and Shiraz, with the coordinates stated in their official timetables. */
    public val SITES: List<Coordinates> =
        listOf(
            Coordinates(35.70, 51.42),
            Coordinates(36.29, 59.62),
            Coordinates(29.50, 60.86),
            Coordinates(27.18, 56.28),
            Coordinates(29.61, 52.53),
        )

    /** Weakest visibility class that still counts as seen. */
    public val VISIBLE_UP_TO: CrescentVisibilityClass = CrescentVisibilityClass.D

    /** Observational month table for [firstYear]‥[lastYear] with this calibration. */
    public fun table(
        firstYear: Int,
        lastYear: Int,
    ): IslamicMonthTable = ObservationalMonthStarts.table(SITES, firstYear, lastYear, VISIBLE_UP_TO)
}
