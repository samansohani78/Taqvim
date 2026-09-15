/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** The mean lunar months shared by the computed lunar calendars (ADR-0027, ADR-0028). */
class MeanLunarMonthsTest {
    // Two edge months 72 012 months apart with a realistic mean month of about 29.5306 days.
    private val first = 1_000_000L
    private val months = 72_012L
    private val mean = MeanLunarMonths(first, first + 2_126_590L, months)

    @Test
    fun `the continuation meets both edges exactly`() {
        mean.fromFirst(0) shouldBe mean.firstStart
        mean.fromLast(0) shouldBe mean.lastStart
        mean.fromFirst(months) shouldBe mean.lastStart
        mean.fromLast(-months) shouldBe mean.firstStart
    }

    @Test
    fun `continued months have 29 or 30 days and one mean line across both edges`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(-30_000_000_000L..30_000_000_000L)) { offset ->
                (mean.fromLast(offset + 1) - mean.fromLast(offset)) shouldBeIn listOf(29L, 30L)
                mean.fromFirst(offset + months) shouldBe mean.fromLast(offset)
            }
        }

    @Test
    fun `the estimated index is within a month of the month containing a day`(): Unit =
        runBlocking {
            val lastIndex = 5_000L
            checkAll(PropertyTesting.iterations, Arb.long(-30_000_000_000L..30_000_000_000L), Arb.int(0..28)) {
                offset,
                day,
                ->
                val estimate = mean.estimateIndex(mean.fromLast(offset) + day, lastIndex)
                (estimate - lastIndex - offset) shouldBeIn listOf(-1L, 0L, 1L)
            }
        }
}
