/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** A-07 Bikram Sambat (ADR-0030) against Nepal's official National Panchang and structural properties for any year. */
class NepaliCalendarSystemTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations / 10)

    private val nepali = NepaliCalendarSystem

    private val panchang =
        GoldenFile
            .load("golden/nepal/national-panchang-2082-2083.csv")
            .lines
            .drop(1)
            .map { it.split(',') }

    private val farYears =
        Arb.choice(
            Arb.int(-100_000 until NepaliMonthStarts.FIRST_CACHED_YEAR),
            Arb.int(NepaliMonthStarts.LAST_CACHED_YEAR + 1..100_000),
            Arb.int(Int.MIN_VALUE..Int.MIN_VALUE + 10_000),
            Arb.int(Int.MAX_VALUE - 10_000..Int.MAX_VALUE - 1),
        )

    @Test
    fun `every month of the official panchang 2082 and 2083 starts on the printed day`() {
        panchang.forEach { row ->
            val date = CalendarDate(CalendarSystem.NEPALI, row[YEAR].toInt(), row[MONTH].toInt(), 1)
            nepali.toJdn(date) shouldBe LocalDate.parse(row[FIRST_DAY]).toJdn()
            nepali.fromJdn(LocalDate.parse(row[FIRST_DAY]).toJdn()) shouldBe date
        }
    }

    @Test
    fun `sankranti times match the panchang to the minute`() {
        panchang.filter { it[TIME].isNotEmpty() }.forEach { row ->
            val instant = NepaliMonthStarts.sankranti(row[YEAR].toLong(), row[MONTH].toInt())
            val (hour, minute) = row[TIME].split(':').map { it.toInt() }
            instant.jdn shouldBe LocalDate.parse(row[SANKRANTI_DATE]).toJdn().value
            (instant.fraction * MINUTES_PER_DAY) shouldBe ((hour * 60 + minute).toDouble() plusOrMinus 1.0)
        }
    }

    @Test
    fun `night sankrantis of Makara and Karka move the month start`() {
        // Makara 2082: 14 January 2026, 21:10 NPT, after sunset → Magh starts the next day.
        NepaliMonthStarts.sankranti(2082, 10).jdn shouldBe LocalDate(2026, 1, 14).toJdn().value
        nepali.toJdn(CalendarDate(CalendarSystem.NEPALI, 2082, 10, 1)) shouldBe LocalDate(2026, 1, 15).toJdn()
        // Karka 2074: 17 July 2017, 04:10 NPT, before sunrise → Shrawan starts the previous day.
        NepaliMonthStarts.sankranti(2074, 4).jdn shouldBe LocalDate(2017, 7, 17).toJdn().value
        nepali.toJdn(CalendarDate(CalendarSystem.NEPALI, 2074, 4, 1)) shouldBe LocalDate(2017, 7, 16).toJdn()
    }

    @Test
    fun `Kathmandu sunrise and sunset agree with the panchang within three minutes`() {
        mapOf(
            LocalDate(2026, 4, 14) to (5 * 60 + 43 to 18 * 60 + 27),
            LocalDate(2026, 7, 17) to (5 * 60 + 20 to 18 * 60 + 59),
            LocalDate(2026, 9, 17) to (5 * 60 + 50 to 18 * 60 + 5),
        ).forEach { (day, times) ->
            val jdn = day.toJdn().value
            (KathmanduDaylight.sunrise(jdn) * MINUTES_PER_DAY) shouldBe (times.first.toDouble() plusOrMinus 3.0)
            (KathmanduDaylight.sunset(jdn) * MINUTES_PER_DAY) shouldBe (times.second.toDouble() plusOrMinus 3.0)
        }
    }

    @Test
    fun `cached years have solar months and contiguous years`() {
        (NepaliMonthStarts.FIRST_CACHED_YEAR..NepaliMonthStarts.LAST_CACHED_YEAR step 7).forEach { year ->
            checkYear(year)
        }
    }

    @Test
    fun `far years keep solar months and round-trip`(): Unit =
        runBlocking {
            checkAll(propertyConfig, farYears) { year -> checkYear(year) }
        }

    @Test
    fun `cached and computed starts meet at the cache edges`() {
        listOf(NepaliMonthStarts.FIRST_CACHED_YEAR - 1, NepaliMonthStarts.LAST_CACHED_YEAR).forEach { year ->
            (nepali.firstDayOfYear(year + 1) - nepali.firstDayOfYear(year)) shouldBeIn listOf(365L, 366L)
            checkYear(year)
        }
    }

    @Test
    fun `every year of Int is supported and nothing beyond`() {
        val first = nepali.firstDayOfYear(Int.MIN_VALUE)
        val last =
            nepali.toJdn(
                CalendarDate(CalendarSystem.NEPALI, Int.MAX_VALUE, 12, nepali.monthLength(Int.MAX_VALUE, 12)),
            )
        nepali.fromJdn(first) shouldBe CalendarDate(CalendarSystem.NEPALI, Int.MIN_VALUE, 1, 1)
        nepali.fromJdn(last).year shouldBe Int.MAX_VALUE
        shouldThrow<IllegalArgumentException> { nepali.fromJdn(first - 1) }
        shouldThrow<IllegalArgumentException> { nepali.fromJdn(last + 1) }
    }

    @Test
    fun `invalid dates and other calendars are rejected`() {
        shouldThrow<IllegalArgumentException> { nepali.monthLength(2083, 13) }
        shouldThrow<IllegalArgumentException> { nepali.toJdn(CalendarDate(CalendarSystem.NEPALI, 2083, 1, 32)) }
        shouldThrow<IllegalArgumentException> { nepali.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)) }
        shouldThrow<IllegalArgumentException> { NepaliMonthStarts.startJdn(2083, 0) }
        nepali.monthsInYear(2083) shouldBe 12
        nepali.isLeapYear(2083) shouldBe false
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a million conversions take less than 300 ms`() {
        val start = nepali.firstDayOfYear(2000).value
        val days = List(CONVERSIONS) { Jdn(start + it % CONVERSION_SPAN) }
        repeat(WARM_UP_RUNS) { convertAll(days) }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { convertAll(days) } }

        best shouldBeLessThan TimingTest.budget(BUDGET_MILLIS)
    }

    private fun convertAll(days: List<Jdn>): Long = days.sumOf { nepali.fromJdn(it).day.toLong() }

    private fun checkYear(year: Int) {
        val lengths = (1..12).map { nepali.monthLength(year, it) }
        lengths.forEach { it shouldBeInRange 29..32 }
        lengths.sum() shouldBeIn listOf(365, 366)
        (lengths.sum() == 366) shouldBe nepali.isLeapYear(year)
        (1..12).forEach { month ->
            val first = CalendarDate(CalendarSystem.NEPALI, year, month, 1)
            val last = CalendarDate(CalendarSystem.NEPALI, year, month, lengths[month - 1])
            nepali.fromJdn(nepali.toJdn(first)) shouldBe first
            nepali.fromJdn(nepali.toJdn(last)) shouldBe last
            nepali.fromJdn(nepali.toJdn(last) + 1).day shouldBe 1
        }
    }

    private companion object {
        const val YEAR = 0
        const val MONTH = 1
        const val FIRST_DAY = 3
        const val SANKRANTI_DATE = 5
        const val TIME = 6
        const val MINUTES_PER_DAY = 1_440.0
        const val CONVERSIONS = 1_000_000
        const val CONVERSION_SPAN = 73_000
        const val WARM_UP_RUNS = 3
        const val MEASURED_RUNS = 5
        const val BUDGET_MILLIS = 300L
    }
}
