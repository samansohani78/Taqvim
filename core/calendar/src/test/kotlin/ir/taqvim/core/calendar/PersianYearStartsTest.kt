/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.PropertyTesting
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** ADR-0026: Persian year starts beyond the astronomical years, against an independent exact-integer model. */
class PersianYearStartsTest {
    private val persian = PersianCalendarSystem
    private val first = PersianYearStarts.FIRST_ASTRONOMICAL_YEAR
    private val last = PersianYearStarts.LAST_ASTRONOMICAL_YEAR
    private val firstEquinox = equinoxMillis(first)
    private val lastEquinox = equinoxMillis(last)

    /** Years from both astronomical edges out to ±100 000 and up to both ends of [Int]. */
    private val farYears =
        Arb.choice(
            Arb.int(-100_000 until first),
            Arb.int(last + 1..100_000),
            Arb.int(Int.MIN_VALUE..Int.MIN_VALUE + 10_000),
            Arb.int(Int.MAX_VALUE - 10_000..Int.MAX_VALUE),
        )

    private fun equinoxMillis(year: Int): BigInteger {
        val equinox = checkNotNull(CalendarAstronomy.marchEquinox(year + GREGORIAN_YEAR_OFFSET))
        return BigInteger.valueOf(equinox.toEpochMilliseconds())
    }

    private fun BigInteger.floorDiv(divisor: BigInteger): BigInteger = (this - mod(divisor)) / divisor

    /** 1 Farvardin [year] from the mean March equinox, computed with unbounded integers. */
    private fun meanEquinoxStart(year: Long): Long {
        val fromLast = year > last
        val years = BigInteger.valueOf(year - if (fromLast) last else first)
        val span = BigInteger.valueOf((last - first).toLong())
        val anchor = if (fromLast) lastEquinox else firstEquinox
        val equinox = anchor + (years * (lastEquinox - firstEquinox)).floorDiv(span)
        val local = equinox + BigInteger.valueOf(TEHRAN_OFFSET_MILLIS)
        val day = BigInteger.valueOf(UNIX_EPOCH_JDN) + local.floorDiv(DAY)
        return (if (local.mod(DAY) < NOON) day else day + BigInteger.ONE).toLong()
    }

    @Test
    fun `the mean equinox gives exactly the astronomical start at both edges`() {
        meanEquinoxStart(first.toLong()) shouldBe PersianYearStarts.startJdn(first.toLong())
        meanEquinoxStart(last.toLong()) shouldBe PersianYearStarts.startJdn(last.toLong())
        ((first - 5)..(first + 5)).plus((last - 5)..(last + 5)).forEach { year ->
            val length = PersianYearStarts.startJdn(year + 1L) - PersianYearStarts.startJdn(year.toLong())
            length shouldBeIn listOf(365L, 366L)
        }
    }

    @Test
    fun `years beyond the astronomical range match exact integer arithmetic`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, farYears) { year ->
                PersianYearStarts.startJdn(year.toLong()) shouldBe meanEquinoxStart(year.toLong())
            }
            PersianYearStarts.startJdn(Int.MAX_VALUE + 1L) shouldBe meanEquinoxStart(Int.MAX_VALUE + 1L)
        }

    @Test
    fun `far years have 365 or 366 days and their dates round-trip`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, farYears, Arb.int(1..12), Arb.int(1..31)) { year, month, rawDay ->
                val length = PersianYearStarts.startJdn(year + 1L) - PersianYearStarts.startJdn(year.toLong())
                length shouldBe if (persian.isLeapYear(year)) 366L else 365L
                persian.monthLength(year, 12) shouldBe if (length == 366L) 30 else 29
                val day = minOf(rawDay, persian.monthLength(year, month))
                val date = CalendarDate(CalendarSystem.PERSIAN, year, month, day)
                persian.fromJdn(persian.toJdn(date)) shouldBe date
                persian.fromJdn(persian.firstDayOfYear(year)) shouldBe CalendarDate(CalendarSystem.PERSIAN, year, 1, 1)
            }
        }

    @Test
    fun `every Int year is supported and no day outside them`() {
        val lastYear = Int.MAX_VALUE
        val lastDay = CalendarDate(CalendarSystem.PERSIAN, lastYear, 12, persian.monthLength(lastYear, 12))
        val firstDay = persian.firstDayOfYear(Int.MIN_VALUE)

        persian.fromJdn(persian.toJdn(lastDay)) shouldBe lastDay
        persian.fromJdn(firstDay) shouldBe CalendarDate(CalendarSystem.PERSIAN, Int.MIN_VALUE, 1, 1)
        shouldThrow<IllegalArgumentException> { persian.fromJdn(persian.toJdn(lastDay) + 1) }
        shouldThrow<IllegalArgumentException> { persian.fromJdn(firstDay - 1) }
    }

    private companion object {
        const val GREGORIAN_YEAR_OFFSET = 621
        const val TEHRAN_OFFSET_MILLIS = 12_600_000L
        const val UNIX_EPOCH_JDN = 2_440_588L
        val DAY: BigInteger = BigInteger.valueOf(86_400_000L)
        val NOON: BigInteger = BigInteger.valueOf(43_200_000L)
    }
}
