/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.testing.GoldenFile
import kotlin.math.abs
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * T-601, DT-011: the MAKKAH method against Umm al-Qura's own published timetables
 * (golden/umm-al-qura-prayer-times/saudi-cities.csv).
 *
 * The app's Makkah parameters came from documentation about the method rather than from the authority that defines
 * it. This compares them against what KACST actually printed, on every day recovered from the Internet Archive's
 * captures of ummulqura.org.sa, and pins each convention the tables turn out to use. The archive JSON records what
 * became of every capture the archive lists, so the coverage can be audited without the network:
 *
 * - **Fajr 18.5°** holds. Fitting the angle whose median published-minus-computed offset sits in the middle of the
 *   authority's rounding window gives **18.494°** over the whole record, and 18.44°–18.57° taken year by year.
 * - **Asr** is the standard shadow factor. The Hanafi factor misses the published column by about an hour.
 * - **Maghrib is sunset itself**: no interval and no precaution minute, unlike the Singapore or Diyanet authorities.
 * - **No elevation or precaution offset on sunrise**: the app's own horizon (34′ refraction, 16′ semi-diameter)
 *   reproduces the published sunrise to better than a minute, which also makes these tables an independent check of
 *   the solar engine over seventeen years.
 * - **The authority rounds away from the prayer**: sunrise down to the minute, Dhuhr, Asr and Maghrib up. The app
 *   rounds to the nearest minute, so its published minute can differ by one from KACST's; that is a rounding
 *   convention, not a disagreement about the time, and [everyArchivedDayAgreesWithinAMinute] allows exactly that.
 *
 * The one real disagreement is Isha in Ramadan 1430 — see [theIshaIntervalIsNinetyMinutesAndOneTwentyInRamadan].
 * That test also checks the calendar and not only the interval: the Isha column switches to 120 minutes on the
 * authority's own Ramadan days, and every archived day falls in the month the app *computes* for it. Hijri 1430-1447
 * are past `UmmAlQuraCalendar.BUNDLED_LAST_YEAR`, so those months come from the rule rather than a printed table, and
 * these tables are the authority's own word on where they begin and end.
 *
 * Only Makkah is compared: KACST publishes no coordinates for its thirteen cities, and Makkah's are the ones the
 * method is defined at. The golden keeps all thirteen as the complete record (DATA_TODO DT-011).
 */
class UmmAlQuraPublishedTimesTest {
    private data class Day(
        val date: String,
        val jdn: Jdn,
        val published: List<Int>,
    )

    private val days: List<Day> =
        GoldenFile
            .load(GOLDEN)
            .lines
            .drop(1)
            .map { it.split(',') }
            .filter { it[CITY] == "Makkah" && it[0] !in STALE_TABLES }
            .map { row ->
                val (year, month, day) = row[0].split('-').map(String::toInt)
                Day(
                    row[0],
                    GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day)),
                    (FIRST_TIME..LAST_TIME).map(row::get).map(::minutes),
                )
            }

    private fun minutes(text: String): Int {
        val (hour, minute) = text.split(':').map(String::toInt)
        return hour * 60 + minute
    }

    /** Published minus computed in minutes, across midnight, positive when the authority's time is later. */
    private fun distance(
        published: Int,
        computed: MinuteOfDay?,
    ): Int {
        val value = computed?.value ?: return Int.MAX_VALUE
        val difference = Math.floorMod(published - value, MINUTES_PER_DAY)
        return if (difference <= MINUTES_PER_DAY / 2) difference else difference - MINUTES_PER_DAY
    }

    /** The app's exact (unrounded) times under [fajrAngle] and [asr], all other parameters as shipped. */
    private fun exact(
        day: Day,
        fajrAngle: Double = SHIPPED_FAJR_ANGLE,
        asr: AsrJuristic = AsrJuristic.STANDARD,
    ): ExactPrayerTimes {
        val shipped = PrayerMethod.MAKKAH.parameters()
        val result =
            PrayerTimesCalculator.exact(
                day.jdn,
                MAKKAH,
                UTC_OFFSET_MINUTES,
                PrayerSettings(method = PrayerMethod.MAKKAH, asr = asr),
                shipped.copy(fajrAngle = fajrAngle),
            )
        check(result is ExactResult.Times) { "${day.date}: the Sun is always up and always sets at Makkah" }
        return result.times
    }

    /** Published minus exact for one prayer, over every archived day. */
    private fun offsets(
        index: Int,
        fajrAngle: Double = SHIPPED_FAJR_ANGLE,
        asr: AsrJuristic = AsrJuristic.STANDARD,
        pick: (ExactPrayerTimes) -> Double?,
    ): List<Double> = days.mapNotNull { day -> pick(exact(day, fajrAngle, asr))?.let { day.published[index] - it } }

    private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

    @TestFactory
    fun everyArchivedDayAgreesWithinAMinute(): List<DynamicTest> =
        days.groupBy { it.date.take(YEAR_LENGTH) }.toSortedMap().map { (year, sample) ->
            DynamicTest.dynamicTest(year) {
                val mismatches =
                    sample.flatMap { day ->
                        val result =
                            PrayerTimesCalculator.calculate(
                                day.jdn,
                                MAKKAH,
                                UTC_OFFSET_MINUTES,
                                PrayerSettings(method = PrayerMethod.MAKKAH),
                            )
                        check(result is PrayerTimesResult.Available) { day.date }
                        val times = result.times
                        listOf(
                            "fajr" to times.fajr,
                            "sunrise" to times.sunrise,
                            "dhuhr" to times.dhuhr,
                            "asr" to times.asr,
                            "maghrib" to times.maghrib,
                            "isha" to times.isha,
                        ).mapIndexedNotNull { index, (prayer, computed) ->
                            val off = distance(day.published[index], computed)
                            "${day.date} $prayer off by $off min".takeIf {
                                abs(off) > TOLERANCE_MINUTES && day.date to prayer !in KNOWN_DISAGREEMENTS
                            }
                        }
                    }
                withClue(mismatches.take(REPORTED).joinToString("\n")) { mismatches.shouldBeEmpty() }
            }
        }

    @Test
    fun theAuthorityRoundsAwayFromEachPrayer() {
        // Printing a whole minute is all the rounding may cost, so each prayer's published-minus-computed offset
        // should fill a window exactly one minute wide, lying wholly on one side of the exact time: sunrise is
        // printed down to the minute and the three times after it up. The median is the sharp test — it sits within
        // a few seconds of each window's centre — and it is what makes these tables an independent check of the
        // solar engine over seventeen years, far tighter than the minute [everyArchivedDayAgreesWithinAMinute] allows.
        val windows =
            listOf(
                Triple("sunrise", offsets(SUNRISE) { it.sunrise }, -1.0..0.0),
                Triple("dhuhr", offsets(DHUHR) { it.dhuhr }, 0.0..1.0),
                Triple("asr", offsets(ASR) { it.asr }, 0.0..1.0),
                Triple("maghrib", offsets(MAGHRIB) { it.maghrib }, 0.0..1.0),
            )
        val reported =
            windows.mapNotNull { (prayer, values, window) ->
                val centre = (window.start + window.endInclusive) / 2
                val inside = values.count { it in window } / values.size.toDouble()
                val worst = values.maxOf { maxOf(it - window.endInclusive, window.start - it, 0.0) }
                val clue =
                    "$prayer: median %.2f, %.1f%% inside $window, worst %.2f min outside"
                        .format(median(values), inside * PERCENT, worst)
                clue.takeIf {
                    abs(median(values) - centre) > MEDIAN_TOLERANCE || inside < INSIDE_FRACTION || worst > WORST_OUTSIDE
                }
            }
        withClue(reported.joinToString("\n")) { reported.shouldBeEmpty() }
        days.size shouldBeGreaterThan MINIMUM_DAYS
    }

    @Test
    fun theTablesUseTheStandardAsrShadowAndNotTheHanafiOne() {
        median(offsets(ASR, asr = AsrJuristic.STANDARD) { it.asr }) shouldBeLessThan 1.0
        median(offsets(ASR, asr = AsrJuristic.HANAFI) { it.asr }) shouldBeLessThan -HANAFI_GAP_MINUTES
    }

    @Test
    fun maghribIsSunsetItselfWithNoIntervalOrPrecaution() {
        PrayerMethod.MAKKAH.parameters().maghrib shouldBe MaghribRule.AtSunset
        // Were a precaution minute hidden in the published column, the Maghrib window would sit a minute later
        // than Dhuhr's and Asr's; measured over the whole record the three agree to a tenth of a minute.
        val maghrib = median(offsets(MAGHRIB) { it.maghrib })
        val dhuhr = median(offsets(DHUHR) { it.dhuhr })
        withClue("maghrib $maghrib vs dhuhr $dhuhr") { abs(maghrib - dhuhr) shouldBeLessThan HALF_MINUTE }
    }

    @Test
    fun theFajrAngleThePublishedTablesImplyIsEighteenAndAHalfDegrees() {
        // The published Fajr is the ceiling of the exact one, so the right angle puts the median offset at half a
        // minute. An angle a half-degree out moves the median by about two minutes, well outside the window.
        val shipped = median(offsets(FAJR, fajrAngle = SHIPPED_FAJR_ANGLE) { it.fajr })
        val lower = median(offsets(FAJR, fajrAngle = SHIPPED_FAJR_ANGLE - PROBE) { it.fajr })
        val higher = median(offsets(FAJR, fajrAngle = SHIPPED_FAJR_ANGLE + PROBE) { it.fajr })
        withClue("median offset at 18.0/18.5/19.0 deg: $lower / $shipped / $higher") {
            shipped shouldBeGreaterThan 0.0
            shipped shouldBeLessThan 1.0
            abs(shipped - HALF_MINUTE) shouldBeLessThan abs(lower - HALF_MINUTE)
            abs(shipped - HALF_MINUTE) shouldBeLessThan abs(higher - HALF_MINUTE)
        }
        PrayerMethod.MAKKAH.parameters().fajrAngle shouldBe SHIPPED_FAJR_ANGLE
    }

    @Test
    fun theIshaIntervalIsNinetyMinutesAndOneTwentyInRamadan() {
        // Isha is printed as a whole interval after the published Maghrib, so it can be read off the table exactly
        // and needs no computation at all. Every day of the record gives 90 minutes, or 120 inside Ramadan — with
        // one exception: Ramadan 1430, the earliest Ramadan preserved, was still on 90. The switch is therefore
        // dated to Ramadan 1431 by this record. The app applies 120 in every Ramadan, so its Isha is half an hour
        // late for 1430 and earlier; one archived day is too little to date a rule in the shipped parameters, and
        // DT-011 asks for more captures of 1430 and before.
        val byMonth =
            days.groupBy { UmmAlQuraCalendar.fromJdn(it.jdn).let { date -> date.year to date.month } }
        val intervals =
            byMonth.mapValues { (_, sample) -> sample.map { it.published[ISHA] - it.published[MAGHRIB] }.toSet() }
        val ramadan = intervals.filterKeys { it.second == RAMADAN }
        val rest = intervals.filterKeys { it.second != RAMADAN }

        withClue(rest.toString()) { rest.values.flatten().toSet() shouldBe setOf(ORDINARY_INTERVAL) }
        withClue(ramadan.toString()) {
            ramadan
                .filterKeys { it.first >= FIRST_LONG_RAMADAN_YEAR }
                .values
                .flatten()
                .toSet() shouldBe
                setOf(RAMADAN_INTERVAL)
            ramadan
                .filterKeys { it.first < FIRST_LONG_RAMADAN_YEAR }
                .values
                .flatten()
                .toSet() shouldBe
                setOf(ORDINARY_INTERVAL)
        }
    }

    private companion object {
        const val GOLDEN = "golden/umm-al-qura-prayer-times/saudi-cities.csv"

        /** Makkah as the app places it, and the coordinates the method is defined at. */
        val MAKKAH = Coordinates(21.4225, 39.8262)

        /** Saudi Arabia keeps UTC+3 all year. */
        const val UTC_OFFSET_MINUTES = 3 * 60
        const val MINUTES_PER_DAY = 1_440
        const val SHIPPED_FAJR_ANGLE = 18.5
        const val PROBE = 0.5
        const val HALF_MINUTE = 0.5

        /**
         * The rounding windows are a whole minute wide. Measured over the whole record: every median sits within
         * 0.03 min of its window's centre, 90–99 % of days fall inside the window, and the worst day is 0.5 min
         * outside it. The bounds below leave that room and no more.
         */
        const val MEDIAN_TOLERANCE = 0.15
        const val INSIDE_FRACTION = 0.88
        const val WORST_OUTSIDE = 0.75
        const val PERCENT = 100.0
        const val TOLERANCE_MINUTES = 1
        const val HANAFI_GAP_MINUTES = 30.0
        const val MINIMUM_DAYS = 550
        const val REPORTED = 10
        const val YEAR_LENGTH = 4

        const val CITY = 1
        const val FIRST_TIME = 2
        const val LAST_TIME = 7
        const val FAJR = 0
        const val SUNRISE = 1
        const val DHUHR = 2
        const val ASR = 3
        const val MAGHRIB = 4
        const val ISHA = 5

        const val RAMADAN = 9
        const val ORDINARY_INTERVAL = 90
        const val RAMADAN_INTERVAL = 120

        /** The first Hijri year whose Ramadan the archived tables give a 120-minute Isha. */
        const val FIRST_LONG_RAMADAN_YEAR = 1431

        /** Ramadan 1430 was still on the 90-minute interval; the app applies 120 to every Ramadan. */
        val KNOWN_DISAGREEMENTS = setOf("2009-09-10" to "isha")

        /**
         * Two captures whose table is two days stale while their header date is the capture day, so the check the
         * archive tool can make — does the page show the day it was captured on — cannot see it. Both published
         * Maghribs match the computed Maghrib of exactly two days earlier to the minute (2018-09-01 prints 18:39,
         * which is 2018-08-30's, and 2018-10-15 prints 17:58, which is 2018-10-13's), and Isha follows Maghrib, so
         * each shows the same two-minute gap. They are what the page printed, so the archive and the golden keep
         * them; they say nothing about the method and are left out of the comparison.
         */
        val STALE_TABLES = setOf("2018-09-01", "2018-10-15")
    }
}
