/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GregorianCalendarSystemTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val gregorian = GregorianCalendarSystem

    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.GREGORIAN, year, month, day)

    @Test
    fun `reference days convert exactly`() {
        // Meeus, Astronomical Algorithms (2nd ed.), ch. 7: JD 2 451 545.0 is 2000-01-01 12:00 TT.
        gregorian.toJdn(date(2000, 1, 1)) shouldBe Jdn(2_451_545)
        // First day of the Gregorian reform (Inter gravissimas, 1582): Friday 1582-10-15.
        gregorian.toJdn(date(1582, 10, 15)) shouldBe Jdn(2_299_161)
        // JDN 0 is 24 November 4714 BC in the proleptic Gregorian calendar (astronomical year −4713).
        gregorian.fromJdn(Jdn(0)) shouldBe date(-4713, 11, 24)
        // Nowruz 1404: 2025-03-21 (JDN 2 460 756); the day before is JDN 2 460 755.
        gregorian.fromJdn(Jdn(2_460_756)) shouldBe date(2025, 3, 21)
        gregorian.fromJdn(Jdn(2_460_755)) shouldBe date(2025, 3, 20)
    }

    @Test
    fun `year zero is a leap year and follows 1 BC numbering`() {
        gregorian.toJdn(date(0, 1, 1)) shouldBe Jdn(GregorianCalendarSystem.JDN_OF_YEAR_ZERO)
        gregorian.toJdn(date(0, 2, 29)) shouldBe Jdn(1_721_119)
        gregorian.fromJdn(Jdn(1_721_059)) shouldBe date(-1, 12, 31)
        gregorian.isLeapYear(0) shouldBe true
    }

    @Test
    fun `negative years round-trip and keep 400-year periodicity`() {
        val jdn = gregorian.toJdn(date(-1_000, 3, 1))

        gregorian.fromJdn(jdn) shouldBe date(-1_000, 3, 1)
        gregorian.toJdn(date(-600, 3, 1)) - jdn shouldBe GregorianCalendarSystem.DAYS_PER_CYCLE
        gregorian.toJdn(date(-4_801, 1, 1)) - gregorian.toJdn(date(-5_201, 1, 1)) shouldBe 146_097L
    }

    @Test
    fun `leap years follow the Gregorian rule`() {
        listOf(1600, 2000, 2024, -4, 4, 2400).forEach { gregorian.isLeapYear(it) shouldBe true }
        listOf(1700, 1900, 2023, 2100, -1, -100, 1).forEach { gregorian.isLeapYear(it) shouldBe false }
    }

    @Test
    fun `month lengths and validity`() {
        gregorian.monthsInYear(2026) shouldBe 12
        gregorian.monthLength(2024, 2) shouldBe 29
        gregorian.monthLength(2023, 2) shouldBe 28
        gregorian.monthLength(2023, 4) shouldBe 30
        gregorian.monthLength(2023, 12) shouldBe 31
        gregorian.isValid(2023, 2, 29) shouldBe false
        gregorian.isValid(2024, 2, 29) shouldBe true
        gregorian.isValid(2024, 13, 1) shouldBe false
        gregorian.isValid(2024, 0, 1) shouldBe false
        gregorian.date(2024, 2, 29) shouldBe date(2024, 2, 29)
        shouldThrow<IllegalArgumentException> { gregorian.monthLength(2024, 13) }
        shouldThrow<IllegalArgumentException> { gregorian.date(2023, 2, 29) }.message.orEmpty() shouldContain
            "does not exist"
    }

    @Test
    fun `invalid inputs are rejected`() {
        shouldThrow<IllegalArgumentException> { gregorian.toJdn(date(2023, 2, 29)) }.message.orEmpty() shouldContain
            "2023-02-29"
        shouldThrow<IllegalArgumentException> { gregorian.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)) }
            .message
            .orEmpty() shouldContain "PERSIAN"
        shouldThrow<ArithmeticException> { gregorian.fromJdn(Jdn(Long.MAX_VALUE / 2)) }
    }

    @Test
    fun `jdn round-trips through the calendar`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(-1_000_000_000L..1_000_000_000L)) { value ->
                gregorian.toJdn(gregorian.fromJdn(Jdn(value))) shouldBe Jdn(value)
            }
        }

    @Test
    fun `jdn round-trips at the property's own range edges`() {
        listOf(-1_000_000_000L, 0L, 1_000_000_000L).forEach { value ->
            gregorian.toJdn(gregorian.fromJdn(Jdn(value))) shouldBe Jdn(value)
        }
    }

    @Test
    fun `dates round-trip and consecutive days differ by one`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(-2_000_000..2_000_000), Arb.int(1..12), Arb.int(1..31)) {
                year,
                month,
                rawDay,
                ->
                val day = minOf(rawDay, gregorian.monthLength(year, month))
                val jdn = gregorian.toJdn(date(year, month, day))
                gregorian.fromJdn(jdn) shouldBe date(year, month, day)
                val next = gregorian.fromJdn(jdn + 1)
                val expectedNext =
                    when {
                        day < gregorian.monthLength(year, month) -> date(year, month, day + 1)
                        month < 12 -> date(year, month + 1, 1)
                        else -> date(year + 1, 1, 1)
                    }
                next shouldBe expectedNext
            }
        }

    @Test
    fun `dates round-trip and consecutive days differ by one at the property's own range edges`() {
        listOf(-2_000_000, 2_000_000).forEach { year ->
            listOf(1 to 1, 12 to 31).forEach { (month, rawDay) ->
                val day = minOf(rawDay, gregorian.monthLength(year, month))
                val jdn = gregorian.toJdn(date(year, month, day))
                gregorian.fromJdn(jdn) shouldBe date(year, month, day)
                val next = gregorian.fromJdn(jdn + 1)
                val expectedNext =
                    when {
                        day < gregorian.monthLength(year, month) -> date(year, month, day + 1)
                        month < 12 -> date(year, month + 1, 1)
                        else -> date(year + 1, 1, 1)
                    }
                next shouldBe expectedNext
            }
        }
    }
}
