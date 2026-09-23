/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.IslamicCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
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
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class TabularIslamicCalendarTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val typeII = TabularIslamicCalendar.TYPE_II
    private val typeI = TabularIslamicCalendar.TYPE_I

    private fun hijri(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    @Test
    fun `epoch is Friday 1 Muharram 1 AH`() {
        typeII.toJdn(hijri(1, 1, 1)) shouldBe Jdn(TabularIslamicCalendar.EPOCH_JDN)
        typeI.toJdn(hijri(1, 1, 1)) shouldBe Jdn(TabularIslamicCalendar.EPOCH_JDN)
        Jdn(TabularIslamicCalendar.EPOCH_JDN).weekday() shouldBe Weekday.FRIDAY
        typeII.variant shouldBe IslamicVariant.TABULAR_16
        typeI.variant shouldBe IslamicVariant.TABULAR_15
    }

    @Test
    fun `leap years follow each variant's cycle`() {
        (1..30).filter { typeII.isLeapYear(it) } shouldBe listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        (1..30).filter { typeI.isLeapYear(it) } shouldBe listOf(2, 5, 7, 10, 13, 15, 18, 21, 24, 26, 29)
        typeII.isLeapYear(32) shouldBe true
        typeII.isLeapYear(0) shouldBe false
        typeII.isLeapYear(-1) shouldBe true
    }

    @Test
    fun `months alternate and the last month grows in leap years`() {
        typeII.monthsInYear(1447) shouldBe 12
        (1..12).map { typeII.monthLength(1446, it) } shouldBe listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        typeII.monthLength(2, 12) shouldBe 30
        typeII.isValid(1, 12, 30) shouldBe false
        typeII.isValid(2, 12, 30) shouldBe true
        shouldThrow<IllegalArgumentException> { typeII.monthLength(1447, 13) }
    }

    @Test
    fun `a cycle is 10 631 days`() {
        typeII.toJdn(hijri(31, 1, 1)) - typeII.toJdn(hijri(1, 1, 1)) shouldBe TabularIslamicCalendar.DAYS_PER_CYCLE
        typeI.toJdn(hijri(1, 1, 1)) - typeI.toJdn(hijri(-29, 1, 1)) shouldBe 10_631L
    }

    @Test
    fun `invalid dates and other systems are rejected`() {
        val invalid = shouldThrow<IllegalArgumentException> { typeII.toJdn(hijri(1447, 2, 30)) }
        invalid.message.orEmpty() shouldContain "1447-02-30"
        shouldThrow<IllegalArgumentException> { typeII.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)) }
    }

    @Test
    fun `type I and type II differ by exactly one day during cycle year 16`(): Unit =
        runBlocking {
            val years = Arb.int(-3_000..3_000)
            checkAll(propertyConfig, years, Arb.int(1..12), Arb.int(1..29)) { year, month, day ->
                val cycleYear = Math.floorMod(year - 1, 30) + 1
                val expected = if (cycleYear == 16) 1L else 0L
                typeI.toJdn(hijri(year, month, day)) - typeII.toJdn(hijri(year, month, day)) shouldBe expected
            }
        }

    @Test
    fun `type I and type II differ by exactly one day at the property's own range edges`() {
        val years = listOf(-3_000, 3_000)
        val months = listOf(1, 12)
        val days = listOf(1, 29)
        val corners = years.flatMap { year -> months.flatMap { month -> days.map { day -> Triple(year, month, day) } } }
        corners.forEach { (year, month, day) ->
            val cycleYear = Math.floorMod(year - 1, 30) + 1
            val expected = if (cycleYear == 16) 1L else 0L
            typeI.toJdn(hijri(year, month, day)) - typeII.toJdn(hijri(year, month, day)) shouldBe expected
        }
    }

    @Test
    fun `both variants round-trip`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(-5_000_000L..10_000_000L)) { value ->
                typeII.toJdn(typeII.fromJdn(Jdn(value))) shouldBe Jdn(value)
                typeI.toJdn(typeI.fromJdn(Jdn(value))) shouldBe Jdn(value)
            }
        }

    @Test
    fun `both variants round-trip at the property's own range edges`() {
        listOf(-5_000_000L, 10_000_000L).forEach { value ->
            typeII.toJdn(typeII.fromJdn(Jdn(value))) shouldBe Jdn(value)
            typeI.toJdn(typeI.fromJdn(Jdn(value))) shouldBe Jdn(value)
        }
    }

    @Test
    fun `type II agrees with ICU4J civil Islamic calendar on 100 000 random days`() {
        val icu =
            IslamicCalendar(TimeZone.GMT_ZONE, ULocale.ROOT).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_CIVIL
            }
        val random = Random(SEED)
        val mismatches =
            (1..SAMPLES).count {
                val epoch = TabularIslamicCalendar.EPOCH_JDN
                val jdn = random.nextLong(epoch, epoch + SPAN_DAYS)
                icu.clear()
                icu.set(Calendar.JULIAN_DAY, jdn.toInt())
                val expected = icu.currentHijriDate()
                typeII.fromJdn(Jdn(jdn)) != expected || typeII.toJdn(expected) != Jdn(jdn)
            }

        mismatches shouldBe 0
    }

    private companion object {
        const val SAMPLES = 100_000
        const val SPAN_DAYS = 1_500L * 355L
        const val SEED = 1_447L
    }
}
