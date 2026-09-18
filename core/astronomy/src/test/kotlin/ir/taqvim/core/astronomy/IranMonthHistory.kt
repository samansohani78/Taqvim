/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.searchMoonPhase
import ir.taqvim.core.model.Coordinates
import java.io.File
import java.time.LocalDate
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * One lunar month of the Calendar Center's official calendars 1381–1405 SH
 * (`golden/islamic-iran/official-month-starts-1381-1405.csv` in `:core:calendar`, written by
 * `tools/sources/iran/official_calendar_import.py`): its first civil day, its length when the calendars print the whole
 * month, how the start was established, and the solar year of the calendar it was read from.
 */
internal class HistoryMonth(
    val label: String,
    val year: Int,
    val month: Int,
    val startJdn: Long,
    val length: Int?,
    val basis: String,
    val solarYear: Int,
    val firstDay: LocalDate,
) {
    /** Whether the month belongs to the held-out calendars (1401–1405 SH) of the refit on 1381–1405 (ADR-0041). */
    val heldOut: Boolean get() = solarYear >= FIRST_HELD_OUT_YEAR

    companion object {
        /** First solar year held out of the fit. */
        const val FIRST_HELD_OUT_YEAR = 1401
    }
}

/** The official month history, read once from the fixture `:core:astronomy`'s build points the tests at. */
internal object IranMonthHistory {
    private const val PROPERTY = "taqvim.iran.monthStarts"
    private const val JDN_OF_EPOCH_DAY_0 = 2_440_588L

    val months: List<HistoryMonth> by lazy {
        val file = File(System.getProperty(PROPERTY) ?: error("$PROPERTY is not set"))
        file
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map(::parse)
    }

    /** Months whose length the calendars print: the facts the evening decision is scored on. */
    val decided: List<HistoryMonth> by lazy { months.filter { it.length != null } }

    private fun parse(line: String): HistoryMonth {
        val columns = line.split(',')
        val (year, month) = columns[MONTH].split('-').map(String::toInt)
        val firstDay = LocalDate.parse(columns[GREGORIAN])
        return HistoryMonth(
            label = columns[MONTH],
            year = year,
            month = month,
            startJdn = firstDay.toEpochDay() + JDN_OF_EPOCH_DAY_0,
            length = columns[LENGTH].takeIf { it.isNotEmpty() }?.toInt(),
            basis = columns[BASIS],
            solarYear = columns[SOLAR_YEAR].toInt(),
            firstDay = firstDay,
        )
    }

    private const val MONTH = 0
    private const val GREGORIAN = 1
    private const val LENGTH = 3
    private const val BASIS = 4
    private const val SOLAR_YEAR = 5
}

/** The crescent on the evening of day 29 at one site: Yallop's and Odeh's geometry and the Moon's age. */
internal class SiteEvening(
    val yallop: CrescentObservation,
    val odehV: Double,
    val ageHours: Double,
)

/**
 * The evening of day 29 of one month at every calibration site, the moment `ObservationalMonthStarts` decides whether
 * the month has 29 days. A site where the Sun does not set or the Moon sets first has no entry.
 */
internal class MonthEvening(
    val month: HistoryMonth,
    val sites: List<SiteEvening>,
) {
    /** The site that sees the crescent best (highest Yallop q), or `null` when no site has a crescent at all. */
    val best: SiteEvening? get() = sites.maxByOrNull { it.yallop.q }

    val maxQ: Double? get() = best?.yallop?.q
    val maxV: Double? get() = sites.maxOfOrNull { it.odehV }
}

/** Crescent geometry for the history's evenings, computed once and shared by every criterion. */
internal object HistoryEvenings {
    private const val DAY_OF_CRESCENT_CHECK = 29
    private const val JULIAN_DAY_OF_UNIX_EPOCH = 2_440_587.5
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val DEGREES_PER_TURN = 360.0
    private const val SEARCH_BACK_DAYS = 4
    private const val SEARCH_SPAN_DAYS = 8.0
    private const val MILLIS_PER_HOUR = 3_600_000.0

    /** The five cities of the shipped calibration (ADR-0027). */
    val sites: List<Coordinates> = IranCrescentCalibration.SITES

    val all: List<MonthEvening> by lazy { IranMonthHistory.decided.map(::evening) }

    private fun evening(month: HistoryMonth): MonthEvening {
        val checkDay = month.startJdn + DAY_OF_CRESCENT_CHECK - 1
        return MonthEvening(month, sites.mapNotNull { site -> siteEvening(site, localNoon(checkDay, site)) })
    }

    /** The crescent at [site] on the first evening after [noon], or `null` when there is none. */
    fun siteEvening(
        site: Coordinates,
        noon: Instant,
    ): SiteEvening? {
        val yallop = Yallop.evening(site, noon) ?: return null
        val odeh = Odeh.evening(site, noon) ?: return null
        return SiteEvening(yallop, odeh.v, ageHours(yallop.bestTime))
    }

    /** Hours from the geocentric conjunction nearest before [time] (negative when it has not happened yet). */
    private fun ageHours(time: Instant): Double {
        val from = (time - SEARCH_BACK_DAYS.days).toAstronomyTime()
        val conjunction = checkNotNull(searchMoonPhase(0.0, from, SEARCH_SPAN_DAYS)) { "no new moon near $time" }
        return (time.toEpochMilliseconds() - conjunction.toInstant().toEpochMilliseconds()) / MILLIS_PER_HOUR
    }

    private fun localNoon(
        jdn: Long,
        place: Coordinates,
    ): Instant {
        val julianDay = jdn - place.longitude / DEGREES_PER_TURN
        return Instant.fromEpochMilliseconds(((julianDay - JULIAN_DAY_OF_UNIX_EPOCH) * MILLIS_PER_DAY).toLong())
    }
}
