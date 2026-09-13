/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class JdnTest {
    private val days = Arb.long(-JDN_BOUND..JDN_BOUND)
    private val offsets = Arb.long(-OFFSET_BOUND..OFFSET_BOUND)

    @Test
    fun `arithmetic moves by whole days`() {
        val jdn = Jdn(2_451_545)

        jdn + 1 shouldBe Jdn(2_451_546)
        jdn + 10L shouldBe Jdn(2_451_555)
        jdn - 1 shouldBe Jdn(2_451_544)
        jdn - 10L shouldBe Jdn(2_451_535)
        Jdn(2_451_555) - jdn shouldBe 10L
        jdn - Jdn(2_451_555) shouldBe -10L
    }

    @Test
    fun `increment and decrement`() {
        var jdn = Jdn(0)
        jdn++
        jdn shouldBe Jdn(1)
        jdn--
        jdn--
        jdn shouldBe Jdn(-1)
    }

    @Test
    fun `arithmetic overflow throws instead of wrapping`() {
        shouldThrow<ArithmeticException> { Jdn(Long.MAX_VALUE) + 1 }
        shouldThrow<ArithmeticException> { Jdn(Long.MIN_VALUE) - 1 }
        shouldThrow<ArithmeticException> { Jdn(Long.MIN_VALUE) - Jdn(1) }
    }

    @Test
    fun `ordering and string form`() {
        (Jdn(1) < Jdn(2)) shouldBe true
        Jdn(5).compareTo(Jdn(5)) shouldBe 0
        Jdn(3).toString() shouldBe "Jdn(3)"
    }

    @Test
    fun `weekday of well-known days`() {
        Jdn(0).weekday() shouldBe Weekday.MONDAY
        // 2000-01-01 was a Saturday (ISO 8601 calendars).
        Jdn(2_451_545).weekday() shouldBe Weekday.SATURDAY
        // 1582-10-15, first Gregorian day, was a Friday.
        Jdn(2_299_161).weekday() shouldBe Weekday.FRIDAY
        Jdn(-1).weekday() shouldBe Weekday.SUNDAY
    }

    @Test
    fun `plus then minus returns the same day`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, days, offsets) { value, offset ->
                Jdn(value) + offset - offset shouldBe Jdn(value)
                (Jdn(value) + offset) - Jdn(value) shouldBe offset
            }
        }

    @Test
    fun `weekday repeats every seven days`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, days, Arb.int(-1000..1000)) { value, weeks ->
                (Jdn(value) + 7L * weeks).weekday() shouldBe Jdn(value).weekday()
                (Jdn(value) + 1).weekday() shouldBe Jdn(value).weekday() + 1
            }
        }

    @Test
    fun `ranges are inclusive and iterable`() {
        val range = Jdn(10)..Jdn(13)

        range.toList() shouldContainExactly listOf(Jdn(10), Jdn(11), Jdn(12), Jdn(13))
        range.dayCount shouldBe 4L
        (Jdn(12) in range) shouldBe true
        (Jdn(14) in range) shouldBe false
        range.toString() shouldBe "Jdn(10)..Jdn(13)"
    }

    @Test
    fun `empty ranges have no days and compare equal`() {
        val empty = Jdn(5)..Jdn(4)

        empty.toList().shouldBeEmpty()
        empty.dayCount shouldBe 0L
        empty shouldBe (Jdn(9)..Jdn(1))
        empty.hashCode() shouldBe (Jdn(9)..Jdn(1)).hashCode()
        empty shouldNotBe (Jdn(1)..Jdn(1))
        (Jdn(1)..Jdn(2)) shouldBe (Jdn(1)..Jdn(2))
        (Jdn(1)..Jdn(2)).hashCode() shouldBe (Jdn(1)..Jdn(2)).hashCode()
        (Jdn(1)..Jdn(2)) shouldNotBe (Jdn(1)..Jdn(3))
        (Jdn(1)..Jdn(2)).equals("Jdn(1)..Jdn(2)") shouldBe false
    }

    @Test
    fun `exhausted iterator throws`() {
        val iterator = (Jdn(1)..Jdn(1)).iterator()
        iterator.next() shouldBe Jdn(1)
        iterator.hasNext() shouldBe false
        shouldThrow<NoSuchElementException> { iterator.next() }
    }

    private companion object {
        const val JDN_BOUND = 1_000_000_000_000L
        const val OFFSET_BOUND = 1_000_000_000L
    }
}
