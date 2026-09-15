/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class PersianCalendarSystemTest {
    private val persian = PersianCalendarSystem

    private fun persianDate(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.PERSIAN, year, month, day)

    private fun yearLength(year: Int): Long =
        persian.firstDayOfYear(year + 1).value - persian.firstDayOfYear(year).value

    @Test
    fun `month lengths follow the 31, 30 and 29 or 30 day pattern`() {
        (1..6).forEach { persian.monthLength(1404, it) shouldBe 31 }
        (7..11).forEach { persian.monthLength(1404, it) shouldBe 30 }
        persian.monthLength(1403, 12) shouldBe 30
        persian.monthLength(1404, 12) shouldBe 29
        persian.monthsInYear(1404) shouldBe 12
        shouldThrow<IllegalArgumentException> { persian.monthLength(1404, 13) }
        shouldThrow<IllegalArgumentException> { persian.monthLength(1404, 0) }
    }

    @Test
    fun `invalid dates and other systems are rejected`() {
        shouldThrow<IllegalArgumentException> { persian.toJdn(persianDate(1404, 12, 30)) }
        shouldThrow<IllegalArgumentException> { persian.toJdn(persianDate(1404, 7, 31)) }
        shouldThrow<IllegalArgumentException> { persian.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2025, 3, 21)) }
    }

    @Test
    fun `month and year boundaries convert exactly`() {
        val lastDayOf1403 = persian.toJdn(persianDate(1403, 12, 30))

        persian.fromJdn(lastDayOf1403) shouldBe persianDate(1403, 12, 30)
        persian.fromJdn(Jdn(lastDayOf1403.value + 1)) shouldBe persianDate(1404, 1, 1)
        persian.fromJdn(Jdn(persian.toJdn(persianDate(1404, 6, 31)).value + 1)) shouldBe persianDate(1404, 7, 1)
        persian.toJdn(persianDate(1404, 1, 1)) shouldBe
            GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2025, 3, 21))
    }

    @Test
    fun `the arithmetic fallback continues both table edges without gaps`() {
        val first = PersianCalendarSystem.TABLE_FIRST_YEAR
        val last = PersianCalendarSystem.TABLE_LAST_YEAR

        ((first - 3)..(first + 2)).plus((last - 2)..(last + 3)).forEach { year ->
            yearLength(year) shouldBe if (persian.isLeapYear(year)) 366L else 365L
        }
    }

    @Test
    fun `every year has 365 or 366 days consistent with its leap flag`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-20_000..20_000)) { year ->
                yearLength(year) shouldBe if (persian.isLeapYear(year)) 366L else 365L
            }
        }

    @Test
    fun `valid dates round-trip in and beyond the table`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-20_000..20_000),
                Arb.int(1..12),
                Arb.int(1..31),
            ) { year, month, rawDay ->
                val date = persianDate(year, month, minOf(rawDay, persian.monthLength(year, month)))
                persian.fromJdn(persian.toJdn(date)) shouldBe date
            }
        }

    @Test
    fun `100 000 random days round-trip from before to after the table`() {
        val random = Random(SEED)

        (1..RANDOM_DAYS)
            .map { Jdn(random.nextLong(-1_000_000L, 5_000_000L)) }
            .filterNot { jdn ->
                val date = persian.fromJdn(jdn)
                persian.toJdn(date) == jdn && date.day <= persian.monthLength(date.year, date.month)
            }.shouldBeEmpty()
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a million conversions take less than 300 ms`() {
        val start = persian.firstDayOfYear(1300).value
        val days = List(CONVERSIONS) { Jdn(start + it % CONVERSION_SPAN) }
        repeat(WARM_UP_RUNS) { convertAll(days) }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { convertAll(days) } }

        best shouldBeLessThan BUDGET_MILLIS
    }

    private fun convertAll(days: List<Jdn>): Long = days.sumOf { persian.fromJdn(it).day.toLong() }

    private companion object {
        const val SEED = 1404
        const val RANDOM_DAYS = 100_000
        const val CONVERSIONS = 1_000_000
        const val CONVERSION_SPAN = 73_000
        const val WARM_UP_RUNS = 3
        const val MEASURED_RUNS = 5
        const val BUDGET_MILLIS = 300L
    }
}
