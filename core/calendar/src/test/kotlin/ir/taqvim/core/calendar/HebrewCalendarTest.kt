/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.HebrewCalendar.MAX_YEAR
import ir.taqvim.core.calendar.HebrewCalendar.MIN_YEAR
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-108 Hebrew calendar rules, over ordinary years and years near the numeric limits. */
class HebrewCalendarTest {
    private val hebrew = HebrewCalendar

    /** Years around the epoch, up to a million years out, and near both ends of the supported range. */
    private val years =
        Arb.choice(
            Arb.int(-1_000_000..1_000_000),
            Arb.int(MIN_YEAR..MIN_YEAR + 1_000),
            Arb.int(MAX_YEAR - 1_000..MAX_YEAR),
            Arb.int(MIN_YEAR..MAX_YEAR),
        )

    @Test
    fun `the epoch is Monday 1 Tishri AM 1`() {
        hebrew.toJdn(HebrewDate(1, 1, 1)) shouldBe Jdn(HebrewCalendar.EPOCH_JDN)
        Jdn(HebrewCalendar.EPOCH_JDN).weekday() shouldBe Weekday.MONDAY
        hebrew.fromJdn(Jdn(HebrewCalendar.EPOCH_JDN)) shouldBe HebrewDate(1, 1, 1)
        hebrew.fromJdn(Jdn(HebrewCalendar.EPOCH_JDN - 1)) shouldBe HebrewDate(0, hebrew.monthsInYear(0), 29)
    }

    @Test
    fun `leap years follow the 19-year cycle`(): Unit =
        runBlocking {
            (1..19).filter { hebrew.isLeapYear(it) } shouldBe listOf(3, 6, 8, 11, 14, 17, 19)
            checkAll(PropertyTesting.iterations, Arb.int(Int.MIN_VALUE..Int.MAX_VALUE - 19)) { year ->
                hebrew.isLeapYear(year + 19) shouldBe hebrew.isLeapYear(year)
                (year until year + 19).count { hebrew.isLeapYear(it) } shouldBe 7
                hebrew.monthsInYear(year) shouldBe if (hebrew.isLeapYear(year)) 13 else 12
            }
        }

    @Test
    fun `years have one of the six lengths and their months add up`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, years) { year ->
                val length = hebrew.yearLength(year)
                length shouldBeIn if (hebrew.isLeapYear(year)) listOf(383, 384, 385) else listOf(353, 354, 355)
                (1..hebrew.monthsInYear(year)).sumOf { hebrew.monthLength(year, it) } shouldBe length
                val heshvanKislev = hebrew.monthLength(year, 2) to hebrew.monthLength(year, 3)
                heshvanKislev shouldBe mapOf(3 to (29 to 29), 4 to (29 to 30), 5 to (30 to 30)).getValue(length % 10)
            }
        }

    @Test
    fun `Rosh Hashanah never falls on Sunday, Wednesday or Friday, nor Pesach on Monday, Wednesday or Friday`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, years) { year ->
                hebrew.newYear(year).weekday() shouldNotBeIn listOf(Weekday.SUNDAY, Weekday.WEDNESDAY, Weekday.FRIDAY)
                val pesach = hebrew.toJdn(JewishObservances.date(JewishObservance.PESACH, year))
                pesach.weekday() shouldNotBeIn listOf(Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY)
            }
        }

    @Test
    fun `days round-trip across the whole supported range`(): Unit =
        runBlocking {
            val days =
                Arb.choice(
                    Arb.long(HebrewCalendar.FIRST_JDN..HebrewCalendar.LAST_JDN),
                    Arb.long(-400_000_000L..400_000_000L),
                    Arb.long(HebrewCalendar.LAST_JDN - 1_000_000L..HebrewCalendar.LAST_JDN),
                    Arb.long(HebrewCalendar.FIRST_JDN..HebrewCalendar.FIRST_JDN + 1_000_000L),
                )
            checkAll(PropertyTesting.iterations, days) { value ->
                val date = hebrew.fromJdn(Jdn(value))
                hebrew.toJdn(date) shouldBe Jdn(value)
            }
        }

    @Test
    fun `the calendar repeats every 689 472 years`(): Unit =
        runBlocking {
            val cycle = HebrewCalendar.FULL_CYCLE_YEARS
            checkAll(PropertyTesting.iterations, Arb.int(MIN_YEAR..MAX_YEAR - cycle)) { year ->
                hebrew.newYear(year + cycle) - hebrew.newYear(year) shouldBe HebrewCalendar.FULL_CYCLE_DAYS
                hebrew.yearLength(year + cycle) shouldBe hebrew.yearLength(year)
                hebrew.newYear(year + cycle).weekday() shouldBe hebrew.newYear(year).weekday()
            }
            HebrewCalendar.FULL_CYCLE_DAYS % 7 shouldBe 0L
        }

    @Test
    fun `the range ends are exact and everything beyond them is rejected`() {
        hebrew.newYear(MIN_YEAR) shouldBe Jdn(HebrewCalendar.FIRST_JDN)
        hebrew.newYear(MAX_YEAR + 1) - 1 shouldBe Jdn(HebrewCalendar.LAST_JDN)
        hebrew.fromJdn(Jdn(HebrewCalendar.FIRST_JDN)) shouldBe HebrewDate(MIN_YEAR, 1, 1)
        hebrew.fromJdn(Jdn(HebrewCalendar.LAST_JDN)) shouldBe HebrewDate(MAX_YEAR, hebrew.monthsInYear(MAX_YEAR), 29)
        shouldThrow<IllegalArgumentException> { hebrew.fromJdn(Jdn(HebrewCalendar.LAST_JDN + 1)) }
        shouldThrow<IllegalArgumentException> { hebrew.fromJdn(Jdn(HebrewCalendar.FIRST_JDN - 1)) }
        shouldThrow<IllegalArgumentException> { hebrew.fromJdn(Jdn(Long.MIN_VALUE)) }
        shouldThrow<IllegalArgumentException> { hebrew.newYear(Int.MIN_VALUE) }
        shouldThrow<IllegalArgumentException> { hebrew.yearLength(Int.MAX_VALUE) }
        shouldThrow<IllegalArgumentException> { hebrew.isValid(Int.MAX_VALUE, 1, 1) }
        shouldThrow<IllegalArgumentException> { hebrew.monthLength(Int.MIN_VALUE, 1) }
    }

    @Test
    fun `months are numbered from Tishri with Adar II only in leap years`() {
        val common = 5786
        val leap = 5787
        hebrew.isLeapYear(common) shouldBe false
        hebrew.isLeapYear(leap) shouldBe true
        val commonMonths = HebrewMonth.entries.filter { it != HebrewMonth.ADAR_II }
        commonMonths.map { hebrew.monthNumber(common, it) } shouldBe (1..12).toList()
        HebrewMonth.entries.map { hebrew.monthNumber(leap, it) } shouldBe (1..13).toList()
        hebrew.monthLength(leap, 6) shouldBe 30
        hebrew.monthLength(common, 6) shouldBe 29
        (7..13).map { hebrew.monthLength(leap, it) } shouldBe listOf(29, 30, 29, 30, 29, 30, 29)
        (7..12).map { hebrew.monthLength(common, it) } shouldBe listOf(30, 29, 30, 29, 30, 29)
        listOf(1, 4, 5).map { hebrew.monthLength(common, it) } shouldBe listOf(30, 29, 30)
        shouldThrow<IllegalArgumentException> { hebrew.monthNumber(common, HebrewMonth.ADAR_II) }
    }

    @Test
    fun `invalid dates are rejected`() {
        hebrew.isValid(5786, 13, 1) shouldBe false
        hebrew.isValid(5786, 12, 30) shouldBe false
        hebrew.isValid(5786, 0, 1) shouldBe false
        shouldThrow<IllegalArgumentException> { hebrew.monthLength(5786, 13) }
        shouldThrow<IllegalArgumentException> { hebrew.toJdn(HebrewDate(5786, 12, 30)) }.message.orEmpty() shouldContain
            "does not exist"
        shouldThrow<IllegalArgumentException> { HebrewDate(5786, 0, 1) }
        shouldThrow<IllegalArgumentException> { HebrewDate(5786, 1, 0) }
    }
}
