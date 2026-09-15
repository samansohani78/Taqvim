/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ChineseCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import io.github.cosinekitty.astronomy.searchSunLongitude
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** A month start as ICU4J's Chinese calendar numbers it on that day. */
private data class IcuMonthDay(
    val month: Int,
    val leap: Boolean,
    val day: Int,
)

/** T-406 Chinese New Year and month numbering against ICU4J (1901–2100), USNO new moons and the calendar rules. */
class ChineseNewYearTest {
    private val icu = ChineseCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)

    /** USNO new moons 1700–2100 (golden/usno), rounded to the minute. */
    private val usnoNewMoons =
        GoldenFile
            .load("golden/usno/moon-phases-1700-2100.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .filter { it[1] == "new_moon" }
            .map { Instant.parse(it[2]) }

    private fun icuOn(day: Jdn): IcuMonthDay {
        icu.timeInMillis = (day.value - UNIX_EPOCH_JDN) * MILLIS_PER_DAY + MILLIS_PER_DAY / 2
        return IcuMonthDay(
            icu.get(Calendar.MONTH) + 1,
            icu.get(Calendar.IS_LEAP_MONTH) == 1,
            icu.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Signed minutes from the nearest civil midnight in China. */
    private fun minutesFromMidnight(instant: Instant): Long {
        val sinceStart = (instant - ChineseNewYear.startOfDay(ChineseNewYear.civilDay(instant))).inWholeMinutes
        return if (sinceStart > MINUTES_PER_DAY / 2) sinceStart - MINUTES_PER_DAY else sinceStart
    }

    private fun gregorian(
        year: Int,
        month: Int,
        day: Int,
    ): CalendarDate = CalendarDate(CalendarSystem.GREGORIAN, year, month, day)

    @Test
    fun `the 2020 Year of the Rat began on 25 January`() {
        ChineseNewYear.date(2020) shouldBe gregorian(2020, 1, 25)
        AnimalYear.forDate(gregorian(2020, 1, 24)) shouldBe ChineseZodiacAnimal.PIG
        AnimalYear.forDate(gregorian(2020, 1, 25)) shouldBe ChineseZodiacAnimal.RAT
        AnimalYear.forDate(gregorian(2020, 12, 31)) shouldBe ChineseZodiacAnimal.RAT
    }

    @Test
    fun `every month of 1901 to 2100 matches ICU except where ICU's lower precision decides a near-midnight event`() {
        val differences =
            (1901..2100).flatMap { year ->
                ChineseNewYear.monthsOfSui(year).filter { month ->
                    icuOn(month.day) != IcuMonthDay(month.number, month.leap, 1)
                }
            }

        // Each difference is a new moon or principal term within a quarter of an hour of midnight in China, where
        // ICU's astronomy is too coarse; the new moons agree with USNO's independent instants.
        differences.map { GregorianCalendarSystem.fromJdn(it.day).year } shouldContainExactly
            listOf(1906, 1917, 1917, 1922, 1922, 1954, 1955, 1987, 1987, 1999, 2012, 2018, 2027, 2030, 2070, 2097)
        differences.forEach { month -> nearMidnightEvent(month.day) shouldBeLessThanOrEqual NEAR_MIDNIGHT_MINUTES }
        NEW_MOON_DIFFERENCE_DAYS.forEach { (year, month, day) ->
            val start = ChineseNewYear.startOfDay(GregorianCalendarSystem.toJdn(gregorian(year, month, day)))
            val usno = usnoNewMoons.single { it > start - 1.days && it < start + 1.days }
            ChineseNewYear.civilDay(usno) shouldBe ChineseNewYear.civilDay(start)
        }
    }

    @Test
    fun `Chinese New Year of 1901 to 2100 matches ICU apart from the three near-midnight new moons`() {
        val differing = (1901..2100).filter { year -> icuOn(ChineseNewYear.day(year)) != IcuMonthDay(1, false, 1) }

        differing shouldContainExactly listOf(1954, 2027, 2030)
    }

    @Test
    fun `the 2033 problem year puts leap month 11 before a New Year on 19 February 2034`() {
        val months = ChineseNewYear.monthsOfSui(2034)

        months.map { it.number to it.leap }.take(4) shouldContainExactly
            listOf(11 to false, 11 to true, 12 to false, 1 to false)
        ChineseNewYear.date(2034) shouldBe gregorian(2034, 2, 19)
        icuOn(ChineseNewYear.day(2034)) shouldBe IcuMonthDay(1, false, 1)
    }

    @Test
    fun `civil days switch from Beijing mean time to UTC+8 at 1929`() {
        val lastMeanTimeDay = GregorianCalendarSystem.toJdn(gregorian(1928, 12, 31))
        ChineseNewYear.startOfDay(lastMeanTimeDay) shouldBe Instant.parse("1928-12-30T16:14:20Z")
        ChineseNewYear.startOfDay(lastMeanTimeDay + 1) shouldBe Instant.parse("1928-12-31T16:00:00Z")
        ChineseNewYear.civilDay(Instant.parse("1928-12-31T15:59:59Z")) shouldBe lastMeanTimeDay
        ChineseNewYear.civilDay(Instant.parse("2026-01-01T15:59:59Z")) shouldBe
            GregorianCalendarSystem.toJdn(gregorian(2026, 1, 1))
    }

    @Test
    fun `New Year falls between 21 January and 20 February in every year from 1901 to 2100`() {
        // Not a rule of the calendar: the window drifts outside this span (e.g. 21 February 2319, 20 January −342).
        (1901..2100).forEach { year ->
            val earliest = GregorianCalendarSystem.toJdn(gregorian(year, 1, 21)).value
            val latest = GregorianCalendarSystem.toJdn(gregorian(year, 2, 20)).value
            ChineseNewYear.day(year).value shouldBeInRange earliest..latest
        }
    }

    @Test
    fun `months follow the calendar rules for any year from 3000 BC to AD 5000`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-3_000..5_000)) { year ->
                val months = ChineseNewYear.monthsOfSui(year)
                val following = ChineseNewYear.monthsOfSui(year + 1)
                val solstice = ChineseNewYear.civilDay(Sky.seasons(year - 1).decemberSolstice)
                // Month 11 contains the December solstice, and consecutive years join without a gap.
                months.first().day.value shouldBeLessThanOrEqual solstice.value
                solstice.value shouldBeLessThan months[1].day.value
                (following.first().day - months.last().day) shouldBeInRange 29L..30L
                months.size shouldBeInRange 12..13
                months.count { it.leap } shouldBe months.size - 12
                months.zipWithNext().forEach { (a, b) -> (b.day - a.day) shouldBeInRange 29L..30L }
                val newYear = ChineseNewYear.day(year)
                (ChineseNewYear.day(year + 1) - newYear) shouldBeInRange YEAR_DAYS
                AnimalYear.forDate(GregorianCalendarSystem.fromJdn(newYear)) shouldBe
                    AnimalYear.ofChineseYearStartingIn(year)
                AnimalYear.forDate(GregorianCalendarSystem.fromJdn(newYear - 1)) shouldBe
                    AnimalYear.ofChineseYearStartingIn(year - 1)
            }
        }

    /** Minutes between China midnight and the nearest new moon or principal term around [day]. */
    private fun nearMidnightEvent(day: Jdn): Long {
        val start = ChineseNewYear.startOfDay(day)
        val window = start - SEARCH_WINDOW_DAYS.days
        val newMoons =
            Sky
                .moonQuarters(window, start + SEARCH_WINDOW_DAYS.days)
                .filter { it.quarter == MoonQuarter.NEW_MOON }
        val terms =
            (0 until 12).mapNotNull { k ->
                searchSunLongitude(k * 30.0, window.toAstronomyTime(), 2.0 * SEARCH_WINDOW_DAYS)?.toInstant()
            }
        return (newMoons.map { it.instant } + terms).minOf { abs(minutesFromMidnight(it)) }
    }

    private companion object {
        const val UNIX_EPOCH_JDN = 2_440_588L
        const val MILLIS_PER_DAY = 86_400_000L
        const val MINUTES_PER_DAY = 1_440L
        const val NEAR_MIDNIGHT_MINUTES = 15L
        const val SEARCH_WINDOW_DAYS = 32
        val YEAR_DAYS = 353L..385L

        /** Month starts that differ from ICU because of a new moon near midnight (the rest are principal terms). */
        val NEW_MOON_DIFFERENCE_DAYS =
            listOf(
                Triple(1954, 2, 3),
                Triple(1955, 2, 22),
                Triple(1999, 1, 17),
                Triple(2012, 8, 17),
                Triple(2018, 11, 8),
                Triple(2027, 2, 6),
                Triple(2030, 2, 3),
                Triple(2070, 3, 12),
                Triple(2097, 8, 8),
                Triple(1906, 4, 23),
            )
    }
}
