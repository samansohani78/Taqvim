/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates

/** A lunar Hijri month: its year and month number. */
internal typealias HijriMonth = Pair<Int, Int>

/** Every month of [firstYear]‥[lastYear], in order. */
internal fun monthsOf(
    firstYear: Int,
    lastYear: Int,
): List<HijriMonth> = (firstYear..lastYear).flatMap { year -> (1..MONTHS_IN_YEAR).map { year to it } }

/** JDN of the first day of [month] in [calendar]. */
internal fun startOf(
    calendar: CalendarArithmetic,
    month: HijriMonth,
): Long = calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, month.first, month.second, 1)).value

private const val MONTHS_IN_YEAR = 12

/**
 * Yallop and Odeh answers for one place and evening, shared by every candidate of a refit: the criteria differ only
 * in where they cut the same classification, so the expensive geometry is computed once.
 */
internal object CrescentAnswers {
    private val yallop = HashMap<Pair<Coordinates, Long>, CrescentVisibilityClass?>()
    private val odeh = HashMap<Pair<Coordinates, Long>, OdehZone?>()

    /** Yallop's rule, answered from the cache. */
    fun yallop(visibleUpTo: CrescentVisibilityClass): CrescentSighting =
        CrescentSighting { place, from ->
            yallop
                .getOrPut(place to from.toEpochMilliseconds()) { Yallop.evening(place, from)?.visibility }
                .let { it != null && it <= visibleUpTo }
        }

    /** Odeh's rule, answered from the cache. */
    fun odeh(visibleUpTo: OdehZone): CrescentSighting =
        CrescentSighting { place, from ->
            odeh
                .getOrPut(place to from.toEpochMilliseconds()) { Odeh.evening(place, from)?.zone }
                .let { it != null && it <= visibleUpTo }
        }
}

/** A calendar the refit tries out, named the way the report prints it. */
internal class Candidate(
    val name: String,
    private val build: (Int, Int) -> CalendarArithmetic,
) {
    private val built = HashMap<Pair<Int, Int>, CalendarArithmetic>()

    /** The calendar covering [firstYear]‥[lastYear], built once. */
    fun calendar(
        firstYear: Int,
        lastYear: Int,
    ): CalendarArithmetic = built.getOrPut(firstYear to lastYear) { build(firstYear, lastYear) }
}

/** Crescent candidates: every criterion and threshold the refit tries, over one set of observing places. */
internal fun crescentCandidates(
    places: String,
    sites: List<Coordinates>,
): List<Candidate> =
    CrescentVisibilityClass.entries.take(YALLOP_THRESHOLDS).map { visibleUpTo ->
        Candidate("Yallop ≤ $visibleUpTo, $places") { first, last ->
            IranIslamicCalendar(ObservationalMonthStarts.table(sites, first, last, CrescentAnswers.yallop(visibleUpTo)))
        }
    } +
        OdehZone.entries.take(ODEH_THRESHOLDS).map { visibleUpTo ->
            Candidate("Odeh ≤ $visibleUpTo, $places") { first, last ->
                IranIslamicCalendar(
                    ObservationalMonthStarts.table(sites, first, last, CrescentAnswers.odeh(visibleUpTo)),
                )
            }
        }

private const val YALLOP_THRESHOLDS = 4
private const val ODEH_THRESHOLDS = 3

/** ICU4J's Umm al-Qura calendar, the published Saudi oracle (Unicode License, test scope only). */
internal object UmmAlQuraPublished {
    private fun atMonthStart(month: HijriMonth): IslamicCalendar =
        IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
            clear()
            set(Calendar.EXTENDED_YEAR, month.first)
            set(Calendar.MONTH, month.second - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

    /** JDN of the first day of [month] as the printed Umm al-Qura calendar has it. */
    fun start(month: HijriMonth): Long = atMonthStart(month).get(Calendar.JULIAN_DAY).toLong()

    /** The shipped Saudi calendar as a candidate. */
    val shipped: Candidate = Candidate("Umm al-Qura criterion (shipped, ADR-0028)") { _, _ -> UmmAlQuraCalendar }
}
