/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile

/** One published fact a calendar must reproduce: a lunar Hijri date with its civil day or its weekday. */
internal class Anchor(
    val label: String,
    val date: CalendarDate,
    val jdn: Long?,
    val weekdayIso: Int?,
) {
    /** Whether [calendar] puts [date] where the source says it is. */
    fun agrees(calendar: CalendarArithmetic): Boolean = offset(calendar) == 0

    /**
     * Days by which [calendar] misses the anchor. A weekday-only anchor is missed by the shortest shift that would
     * land the date on the announced weekday, so it reports −3‥3 days.
     */
    fun offset(calendar: CalendarArithmetic): Int {
        val computed = calendar.toJdn(date)
        jdn?.let { return (computed.value - it).toInt() }
        val announced = requireNotNull(weekdayIso) { "$label: neither a civil day nor a weekday" }
        val difference = (computed.weekday().isoNumber - announced).mod(DAYS_PER_WEEK)
        return if (difference > DAYS_PER_WEEK / 2) difference - DAYS_PER_WEEK else difference
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/** A region of the calibration report: the published facts, the shipped calendar and the calendars the refit tries. */
internal class Region(
    val name: String,
    val anchors: List<Anchor>,
    val firstYear: Int,
    val lastYear: Int,
    val shipped: Candidate,
    val refit: List<Candidate>,
    val threshold: Double,
    val note: String,
)

/** Agreement of one candidate with one region's anchors. */
internal class Agreement(
    val candidate: Candidate,
    val shipped: Boolean,
    val agreed: Int,
    val total: Int,
    val misses: List<String>,
) {
    val share: Double get() = if (total == 0) 0.0 else agreed.toDouble() / total
    val percent: String get() = "%.1f %%".format(share * PERCENT)

    private companion object {
        const val PERCENT = 100
    }
}

/** Scores [candidate] against [region]'s anchors. */
internal fun score(
    region: Region,
    candidate: Candidate,
): Agreement {
    val calendar = candidate.calendar(region.firstYear, region.lastYear)
    val misses =
        region.anchors.filterNot { it.agrees(calendar) }.map { anchor ->
            "${anchor.label} (${anchor.offset(calendar).let { if (it > 0) "+$it" else "$it" }} d)"
        }
    return Agreement(
        candidate,
        candidate.name == region.shipped.name,
        region.anchors.size - misses.size,
        region.anchors.size,
        misses,
    )
}

/** Every region the report covers, with the data available today. */
internal fun regions(): List<Region> = listOf(iran(), iranHistory(), saudiArabia(), afghanistan())

private const val IRAN_FIRST_YEAR = 1446
private const val IRAN_LAST_YEAR = 1448
private const val IRAN_THRESHOLD = 0.90
private const val IRAN_HISTORY_THRESHOLD = 0.90
private const val SAUDI_FIRST_YEAR = 1420
private const val SAUDI_LAST_YEAR = 1450
private const val SAUDI_THRESHOLD = 0.99
private const val AFGHAN_FIRST_YEAR = 1447
private const val AFGHAN_LAST_YEAR = 1448

private fun hijri(
    year: Int,
    month: Int,
    day: Int,
) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

private fun iran(): Region {
    val table =
        IslamicMonthOverrides.parse(IslamicMonthOverrides.bundledIranOfficialText().orEmpty()).getOrThrow().table
    val published = IranIslamicCalendar(table)
    val anchors =
        monthsOf(IRAN_FIRST_YEAR, IRAN_LAST_YEAR)
            .map { it to startOf(published, it) }
            .filter { (_, start) -> table.covers(start) }
            .map { (month, start) ->
                Anchor(
                    "${month.first}-${"%02d".format(month.second)}",
                    hijri(month.first, month.second, 1),
                    start,
                    null,
                )
            }
    return Region(
        name = "Iran (Calendar Center, official calendars in docs/sources)",
        anchors = anchors,
        firstYear = IRAN_FIRST_YEAR,
        lastYear = IRAN_LAST_YEAR,
        shipped =
            Candidate("five cities, Yallop ≤ D (shipped, ADR-0027)") { first, last ->
                IranIslamicCalendar(IranCrescentCalibration.table(first, last))
            },
        refit =
            crescentCandidates("Tehran", IranCrescentCalibration.SITES.take(1)) +
                crescentCandidates("five cities", IranCrescentCalibration.SITES) +
                crescentCandidates("south-east", IranCrescentCalibration.SITES.subList(2, 4)) +
                listOf(tabular("type II"), tabular("type I", TabularIslamicCalendar.TYPE_I)),
        threshold = IRAN_THRESHOLD,
        note =
            "One anchor per lunar month the official calendars print (their first days). The month table is rebuilt " +
                "from the tabular calendar two months before the range, so a candidate that is a day out stays a day " +
                "out until a crescent check corrects it.",
    )
}

private fun iranHistory(): Region {
    val anchors =
        IranMonthHistory.months.map { month ->
            Anchor(
                "${month.label}${if (month.heldOut) "*" else ""}",
                hijri(month.year, month.month, 1),
                month.startJdn,
                null,
            )
        }
    return Region(
        name = "Iran 1381–1405 SH (Calendar Center, every readable official calendar)",
        anchors = anchors,
        firstYear = anchors.minOf { it.date.year },
        lastYear = anchors.maxOf { it.date.year },
        shipped =
            Candidate("five cities, Yallop ≤ D (shipped, ADR-0027)") { first, last ->
                IranIslamicCalendar(IranCrescentCalibration.table(first, last))
            },
        refit =
            crescentCandidates("Tehran", IranCrescentCalibration.SITES.take(1)) +
                crescentCandidates("five cities", IranCrescentCalibration.SITES) +
                listOf(logisticCandidate(), tabular("type II"), tabular("type I", TabularIslamicCalendar.TYPE_I)),
        threshold = IRAN_HISTORY_THRESHOLD,
        note =
            "One anchor per lunar month start the calendars establish (printed, counted back, or moved by an " +
                "announcement the calendar notes); `*` marks the held-out calendars 1403–1405. The months are " +
                "chained from AH ${anchors.minOf { it.date.year }}, so a day lost stays lost until a crescent check " +
                "restores it; the evening-decision section below scores each month on its own.",
    )
}

/** The logistic model of ADR-0041, fitted on 1381–1400, as a chained month table over the five cities. */
private fun logisticCandidate() =
    Candidate("logistic (ADR-0041), five cities") { first, last ->
        val cache = HashMap<Pair<Coordinates, Long>, Boolean>()
        val sighting =
            CrescentSighting { place, from ->
                cache.getOrPut(place to from.toEpochMilliseconds()) {
                    HistoryEvenings.siteEvening(place, from)?.let(historyModel::seen) == true
                }
            }
        IranIslamicCalendar(ObservationalMonthStarts.table(IranCrescentCalibration.SITES, first, last, sighting))
    }

private fun saudiArabia(): Region {
    val anchors =
        monthsOf(SAUDI_FIRST_YEAR, SAUDI_LAST_YEAR).map { month ->
            Anchor(
                "${month.first}-${"%02d".format(month.second)}",
                hijri(month.first, month.second, 1),
                UmmAlQuraPublished.start(month),
                null,
            )
        }
    return Region(
        name = "Saudi Arabia (printed Umm al-Qura calendar, ICU4J as the oracle)",
        anchors = anchors,
        firstYear = SAUDI_FIRST_YEAR,
        lastYear = SAUDI_LAST_YEAR,
        shipped = UmmAlQuraPublished.shipped,
        refit =
            crescentCandidates("Makkah", listOf(MAKKAH)) +
                listOf(tabular("type II"), tabular("type I", TabularIslamicCalendar.TYPE_I)),
        threshold = SAUDI_THRESHOLD,
        note =
            "One anchor per printed month of AH $SAUDI_FIRST_YEAR–$SAUDI_LAST_YEAR, the years the Umm al-Qura " +
                "calendar has been published for. The shipped calendar computes them from the Umm al-Qura criterion " +
                "(ADR-0028); the two months it misses are the marginal ones `UmmAlQuraCriterionTest` names.",
    )
}

private fun afghanistan(): Region =
    Region(
        name = "Afghanistan (Bakhtar News Agency announcements)",
        anchors = bakhtarAnchors(),
        firstYear = AFGHAN_FIRST_YEAR,
        lastYear = AFGHAN_LAST_YEAR,
        shipped = tabular("tabular type II (shipped, ADR-0010)"),
        refit =
            crescentCandidates("Kabul", listOf(KABUL)) +
                listOf(tabular("type I", TabularIslamicCalendar.TYPE_I)),
        threshold = 1.0,
        note =
            "One anchor per lunar Hijri date an announcement states together with its Solar Hijri day or its " +
                "weekday; a weekday-only anchor constrains the month start within the week only.",
    )

/** The announced Afghan dates, read from the golden fixture the announcements were transcribed into. */
private fun bakhtarAnchors(): List<Anchor> =
    GoldenFile
        .load("golden/islamic-afghanistan/bakhtar-announced-dates.csv")
        .lines
        .drop(1)
        .map { line ->
            val columns = line.split(',')
            val (year, month, day) = columns[HIJRI].split('-').map(String::toInt)
            Anchor(
                label = columns[ID].removePrefix("af.holiday."),
                date = hijri(year, month, day),
                jdn = columns[CIVIL].takeIf { it.isNotEmpty() }?.let(::persianJdn),
                weekdayIso = columns[WEEKDAY].takeIf { it.isNotEmpty() }?.toInt(),
            )
        }

private const val ID = 0
private const val HIJRI = 1
private const val CIVIL = 2
private const val WEEKDAY = 3

private fun persianJdn(iso: String): Long {
    val (year, month, day) = iso.split('-').map(String::toInt)
    return PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day)).value
}

private fun tabular(
    name: String,
    calendar: CalendarArithmetic = TabularIslamicCalendar.TYPE_II,
) = Candidate(name) { _, _ -> calendar }

private val MAKKAH = Coordinates(21.4225, 39.8262)
private val KABUL = Coordinates(34.53, 69.17)
