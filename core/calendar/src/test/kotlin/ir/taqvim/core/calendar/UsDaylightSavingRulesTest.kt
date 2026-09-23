/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-110 national rule: every USNO date for 1967–9999, then weekday-of-month properties for far years. */
class UsDaylightSavingRulesTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val usno =
        GoldenFile
            .load("golden/usno/us-daylight-saving-1967-9999.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row -> UsDaylightSavingDates(row[0].toLong(), monthDay(row[1]), monthDay(row[2])) }

    @Test
    fun `the rule gives all 16066 USNO dates from 1967 to 9999`() {
        usno.size shouldBe 8_033
        usno.filterNot { UsDaylightSavingRules.forYear(it.year) == it }.shouldBeEmpty()
    }

    @Test
    fun `years before the Uniform Time Act have no dates`() {
        UsDaylightSavingRules.forYear(UsDaylightSavingRules.FIRST_YEAR - 1).shouldBeNull()
        UsDaylightSavingRules.forYear(Long.MIN_VALUE).shouldBeNull()
    }

    @Test
    fun `from 2007 to ten million the change is on the second Sunday of March and the first of November`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(2_007L..10_000_000L)) { year ->
                val dates = UsDaylightSavingRules.forYear(year).shouldNotBeNull()
                dates.begins.month shouldBe 3
                (dates.begins.day in 8..14) shouldBe true
                dates.ends.month shouldBe 11
                (dates.ends.day in 1..7) shouldBe true
                sunday(year, dates.begins) shouldBe true
                sunday(year, dates.ends) shouldBe true
                (dates.begins.month * 100 + dates.begins.day < dates.ends.month * 100 + dates.ends.day) shouldBe true
            }
        }

    @Test
    fun `the March-November rule holds at the property's own year range edges`() {
        listOf(2_007L, 10_000_000L).forEach { year ->
            val dates = UsDaylightSavingRules.forYear(year).shouldNotBeNull()
            dates.begins.month shouldBe 3
            (dates.begins.day in 8..14) shouldBe true
            dates.ends.month shouldBe 11
            (dates.ends.day in 1..7) shouldBe true
            sunday(year, dates.begins) shouldBe true
            sunday(year, dates.ends) shouldBe true
        }
    }

    @Test
    fun `any year up to Long MAX_VALUE matches its year in the same 400-year cycle`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(10_000L..Long.MAX_VALUE)) { year ->
                val sameCycle = 2_400L + Math.floorMod(year - 2_400L, 400L)
                val dates = UsDaylightSavingRules.forYear(year).shouldNotBeNull()
                dates shouldBe UsDaylightSavingRules.forYear(sameCycle).shouldNotBeNull().copy(year = year)
            }
            UsDaylightSavingRules.forYear(Long.MAX_VALUE).shouldNotBeNull().year shouldBe Long.MAX_VALUE
        }

    @Test
    fun `earlier eras keep their weekday rules in every year`() {
        (1967L..2006L).filterNot { it == 1974L || it == 1975L }.forEach { year ->
            val dates = UsDaylightSavingRules.forYear(year).shouldNotBeNull()
            val beginDays = if (year < 1987L) 24..30 else 1..7
            listOf(dates.begins.month, dates.ends.month) shouldBe listOf(4, 10)
            (dates.begins.day in beginDays) shouldBe true
            (dates.ends.day in 25..31) shouldBe true
            sunday(year, dates.begins) shouldBe true
            sunday(year, dates.ends) shouldBe true
        }
    }

    private fun sunday(
        year: Long,
        day: GregorianMonthDay,
    ): Boolean = LocalDate(Math.toIntExact(year), day.month, day.day).dayOfWeek == DayOfWeek.SUNDAY

    private fun monthDay(iso: String): GregorianMonthDay {
        val (_, month, day) = iso.split('-').map(String::toInt)
        return GregorianMonthDay(month, day)
    }
}
