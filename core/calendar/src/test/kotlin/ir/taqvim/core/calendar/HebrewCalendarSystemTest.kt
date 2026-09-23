/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** F07: [HebrewCalendarSystem] adapts [HebrewCalendar] to [CalendarArithmetic]. */
class HebrewCalendarSystemTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val hebrew = HebrewCalendarSystem

    @Test
    fun `days round trip and agree with HebrewCalendar`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.long(1_000_000L..4_000_000L)) { day ->
                val date = hebrew.fromJdn(Jdn(day))
                date.system shouldBe CalendarSystem.HEBREW
                hebrew.toJdn(date) shouldBe Jdn(day)
                HebrewCalendar.fromJdn(Jdn(day)) shouldBe HebrewDate(date.year, date.month, date.day)
            }
        }

    @Test
    fun `leap years have 13 months and month arithmetic steps year by year`() {
        hebrew.monthsPerYear shouldBe null
        hebrew.isLeapYear(5784) shouldBe true
        hebrew.monthsInYear(5784) shouldBe 13
        hebrew.monthsInYear(5785) shouldBe 12
        (1..13).sumOf { hebrew.monthLength(5784, it) } shouldBe HebrewCalendar.yearLength(5784)
        hebrew.isValid(5785, 13, 1) shouldBe false
        hebrew.addMonths(hebrew.date(5784, 12, 1), 2) shouldBe hebrew.date(5785, 1, 1)
        hebrew.monthsBetween(hebrew.date(5784, 1, 1), hebrew.date(5786, 1, 1)) shouldBe 25
    }

    @Test
    fun `dates of another calendar are rejected`() {
        shouldThrow<IllegalArgumentException> { hebrew.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2026, 1, 1)) }
        hebrew.fromJdn(Jdn(HebrewCalendar.EPOCH_JDN)) shouldBe CalendarDate(CalendarSystem.HEBREW, 1, 1, 1)
    }
}
