/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import java.math.BigInteger
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** ADR-0027: the Iran-calibrated crescent months for every year. */
class IranCrescentCalendarTest {
    private val crescent = IranCrescentCalendar
    private val months = IranCrescentMonths

    private fun hijri(
        year: Int,
        month: Int,
        day: Int = 1,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    /** Years beyond the crescent range out to ±1 000 000 and up to both ends of [Int]. */
    private val farYears =
        Arb.choice(
            Arb.int(-1_000_000 until IranCrescentMonths.FIRST_ASTRONOMICAL_YEAR),
            Arb.int(IranCrescentMonths.LAST_ASTRONOMICAL_YEAR + 2..1_000_000),
            Arb.int(Int.MIN_VALUE..Int.MIN_VALUE + 10_000),
            Arb.int(Int.MAX_VALUE - 10_000..Int.MAX_VALUE),
        )

    private fun assertMonthsOf29Or30Days(indices: LongRange) {
        indices.forEach { index -> (months.startJdn(index + 1) - months.startJdn(index)) shouldBeIn listOf(29L, 30L) }
    }

    @Test
    fun `the crescent months agree with at least 23 of the 25 official month starts`() {
        val table = IranOfficialMonthStarts.TABLE
        val agreeing =
            (0 until table.monthCount).count { index ->
                val (year, month) = table.yearMonthAt(index)
                crescent.toJdn(hijri(year, month)).value == table.startAt(index)
            }

        println("ADR-0027 crescent months vs official: $agreeing of ${table.monthCount} agree")
        agreeing shouldBeGreaterThanOrEqual 23
    }

    @Test
    fun `a month has 29 days when the crescent is seen on the evening of day 29, otherwise 30`() {
        (months.monthIndex(1440, 1) until months.monthIndex(1451, 1)).forEach { index ->
            val start = months.startJdn(index)
            val seen = IranCrescentSighting.seenOnEvening(start + 28)
            months.startJdn(index + 1) shouldBe start + if (seen) 29 else 30
        }
    }

    @Test
    fun `months of AH 1400 to 1500 and of years across the crescent range have 29 or 30 days`() {
        assertMonthsOf29Or30Days(months.monthIndex(1400, 1) until months.monthIndex(1501, 1))
        listOf(-3000, -2999, -1600, -1, 1, 1000, 2500, 2999, 3000).forEach { year ->
            assertMonthsOf29Or30Days(months.monthIndex(year.toLong(), 1) until months.monthIndex(year + 1L, 1))
        }
        IranCrescentSighting.SITES.size shouldBe 5
        months.monthOf(months.monthIndex(1447, 9)) shouldBe 9
    }

    @Test
    fun `the mean month continues both edges exactly`() {
        val first = months.FIRST_INDEX
        val last = months.LAST_INDEX
        val firstStart = BigInteger.valueOf(months.startJdn(first))
        val lastStart = BigInteger.valueOf(months.startJdn(last))
        val span = BigInteger.valueOf(last - first)
        val days = lastStart - firstStart

        fun exact(
            anchor: BigInteger,
            offset: Long,
        ): Long {
            val scaled = BigInteger.valueOf(offset) * days
            return (anchor + (scaled - scaled.mod(span)) / span).toLong()
        }

        assertMonthsOf29Or30Days(first - 3..first + 2)
        assertMonthsOf29Or30Days(last - 3..last + 2)
        listOf(-1L, -1_000L, -2_000_000_000L).forEach { months.startJdn(first + it) shouldBe exact(firstStart, it) }
        listOf(1L, 1_000L, 2_000_000_000L).forEach { months.startJdn(last + it) shouldBe exact(lastStart, it) }
    }

    @Test
    fun `far years have 29 or 30 day months and their dates round-trip`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, farYears, Arb.int(1..12), Arb.int(1..30)) { year, month, rawDay ->
                val length = crescent.monthLength(year, month)
                length shouldBeIn listOf(29, 30)
                val yearDays = (1..12).sumOf { crescent.monthLength(year, it) }
                yearDays shouldBeIn listOf(354, 355)
                crescent.isLeapYear(year) shouldBe (yearDays == 355)
                val date = hijri(year, month, minOf(rawDay, length))
                crescent.fromJdn(crescent.toJdn(date)) shouldBe date
                crescent.monthsInYear(year) shouldBe 12
            }
        }

    @Test
    fun `every Int year is supported and no day outside them`() {
        val lastYear = Int.MAX_VALUE
        val lastDay = hijri(lastYear, 12, crescent.monthLength(lastYear, 12))
        val firstDay = crescent.toJdn(hijri(Int.MIN_VALUE, 1))

        crescent.fromJdn(crescent.toJdn(lastDay)) shouldBe lastDay
        crescent.fromJdn(firstDay) shouldBe hijri(Int.MIN_VALUE, 1)
        shouldThrow<IllegalArgumentException> { crescent.fromJdn(crescent.toJdn(lastDay) + 1) }
        shouldThrow<IllegalArgumentException> { crescent.fromJdn(firstDay - 1) }
        shouldThrow<IllegalArgumentException> { crescent.toJdn(hijri(1447, 12, 31)) }
        shouldThrow<IllegalArgumentException> { crescent.monthLength(1447, 13) }
        shouldThrow<IllegalArgumentException> { crescent.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)) }
    }

    @Test
    fun `anchored months complete or delay single odd sighting months and restart on anchors`() {
        val lengths =
            listOf(30, 29, 30, 31, 29, 30, 29, 28, 30, 29, 30, 30, 31, 29, 29, 30, 30, 29, 31, 29, 30, 29, 30, 29)
        val sightings = lengths.runningFold(1_000L) { start, length -> start + length }
        val sighting = { index: Long -> sightings[index.toInt()] }
        val anchored = sightings.indices.map { AnchoredMonthStarts.start(it.toLong(), 0, sighting) }
        val chained =
            sightings.drop(1).runningFold(sightings.first()) { previous, next ->
                next.coerceIn(previous + 29, previous + 30)
            }

        anchored shouldBe chained
        anchored.zipWithNext { a, b -> b - a }.forEach { it shouldBeIn listOf(29L, 30L) }
        AnchoredMonthStarts.isAnchor(2, sighting) shouldBe true
        AnchoredMonthStarts.isAnchor(5, sighting) shouldBe false
        anchored.indices.filter { anchored[it] != sightings[it] } shouldBe listOf(4, 8, 13, 19)
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a million cached conversions take less than a second`() {
        val start = crescent.toJdn(hijri(1300, 1)).value
        val days = List(CONVERSIONS) { Jdn(start + it % CONVERSION_SPAN) }
        val firstUse = measureTimeMillis { crescent.fromJdn(Jdn(start + CONVERSION_SPAN / 2)) }
        repeat(WARM_UP_RUNS) { convertAll(days) }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { convertAll(days) } }

        println("ADR-0027 conversions: first use ${firstUse}ms, 1 000 000 cached fromJdn best ${best}ms")
        best shouldBeLessThan TimingTest.budget(BUDGET_MILLIS)
    }

    private fun convertAll(days: List<Jdn>): Long = days.sumOf { crescent.fromJdn(it).day.toLong() }

    private companion object {
        const val CONVERSIONS = 1_000_000
        const val CONVERSION_SPAN = 73_000
        const val WARM_UP_RUNS = 3
        const val MEASURED_RUNS = 3
        const val BUDGET_MILLIS = 1_000L
    }
}
