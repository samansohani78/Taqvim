/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** The 2820-year structure as described by C. Tøndering (docs/PROVENANCE.md, A-02). */
class BirashkArithmeticTest {
    private val epoch = BirashkArithmetic.EPOCH_YEAR

    private fun leapOffsets(
        start: Int,
        years: Int,
    ): List<Int> = (start until start + years).filter(BirashkArithmetic::isLeapYear).map { it - start }

    @Test
    fun `a period has 683 leap years wherever it starts`() {
        BirashkArithmetic.leapYearsBetween(epoch, epoch + 2820) shouldBe 683L
        BirashkArithmetic.leapYearsBetween(epoch - 3 * 2820, epoch - 2 * 2820) shouldBe 683L
        BirashkArithmetic.leapYearsBetween(epoch + 2820, epoch) shouldBe -683L
    }

    @Test
    fun `cycles begin with four common years and then every fourth year is leap`() {
        leapOffsets(epoch, 29 + 33) shouldBe listOf(4, 8, 12, 16, 20, 24, 28, 33, 37, 41, 45, 49, 53, 57, 61)
        leapOffsets(epoch + 128, 29) shouldBe (4..28 step 4).toList()
    }

    @Test
    fun `the last cycle of a period has 37 years`() {
        val lastCycle = epoch + 2688 + 29 + 33 + 33

        leapOffsets(lastCycle, 37) shouldBe (4..36 step 4).toList()
        BirashkArithmetic.isLeapYear(lastCycle + 37) shouldBe false
        BirashkArithmetic.isLeapYear(lastCycle + 37 + 4) shouldBe true
    }

    @Test
    fun `leap counts are additive and match year-by-year counting`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-10_000..10_000), Arb.int(0..600)) { from, span ->
                BirashkArithmetic.leapYearsBetween(from, from + span) shouldBe
                    (from until from + span).count(BirashkArithmetic::isLeapYear).toLong()
            }
        }
}
