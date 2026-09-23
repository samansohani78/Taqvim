/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** ADR-0028: the Umm al-Qura criterion against the published calendar, and the calendar beyond it. */
class UmmAlQuraCriterionTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val uaq = UmmAlQuraCalendar
    private val months = UmmAlQuraMonths

    private fun previousIcuStart(
        year: Int,
        month: Int,
    ): Long = if (month == 1) UmmAlQuraIcu.start(year - 1, 12) else UmmAlQuraIcu.start(year, month - 1)

    /** Months of [years] whose start the rule, applied to the published start of the month before, does not give. */
    private fun ruleMismatches(
        years: IntRange,
        requireConjunction: Boolean,
    ): List<Pair<Int, Int>> =
        years.flatMap { year ->
            (1..12).mapNotNull { month ->
                val predicted = UmmAlQuraCriterion.nextMonthStart(previousIcuStart(year, month), requireConjunction)
                if (predicted != UmmAlQuraIcu.start(year, month)) year to month else null
            }
        }

    @Test
    fun `the rule since 1423 gives every published month but two marginal ones`() {
        // Jumada II 1427: conjunction 24 s before sunset at the Kaaba (published month: 30 days).
        // Jumada II 1446: moonset 4 s before sunset (published month: 29 days).
        ruleMismatches(1423..1450, requireConjunction = true) shouldBe listOf(1427 to 6, 1446 to 6)
    }

    @Test
    fun `the moonset rule of 1420 to 1422 gives every published month`() {
        ruleMismatches(1420..1422, requireConjunction = false).shouldBeEmpty()
    }

    @Test
    fun `the calendar computes 1420 to 1422 with the moonset rule and 1423 on with the criterion`() {
        var start = months.start(months.index(1420, 1))
        (months.index(1420, 1)..months.index(1422, 12)).forEach { index ->
            withClue(index) { months.start(index) shouldBe start }
            start = UmmAlQuraCriterion.nextMonthStart(start, requireConjunction = months.yearOf(index + 1) >= 1423)
        }
        months.start(months.index(1423, 1)) shouldBe start
        (1420..1450).forEach { year -> uaq.isBundled(year) shouldBe false }
        months.isBundled(months.index(1419, 12)) shouldBe true
        months.isBundled(months.index(1420, 1)) shouldBe false
    }

    @Test
    fun `after 1450 ICU4J's projection is never earlier than the criterion`() {
        val late =
            (1451..1600).flatMap { year ->
                (1..12).mapNotNull { month ->
                    val difference =
                        UmmAlQuraIcu.start(year, month) -
                            UmmAlQuraCriterion.nextMonthStart(previousIcuStart(year, month))
                    if (difference == 0L) null else Triple(year, month, difference)
                }
            }

        withClue({ "${late.size} months differ" }) {
            late.map { it.third }.toSet() shouldBe setOf(1L)
            // Rabi II 1451: conjunction 14 h before sunset and moonset 10 min after it, yet ICU4J gives 30 days.
            late.map { it.first to it.second } shouldContain (1451 to 4)
        }
    }

    @Test
    fun `computed months follow the rule month after month on both sides of the published years`() {
        var start = months.start(months.index(1451, 1))
        (months.index(1451, 1)..months.index(1650, 12)).forEach { index ->
            months.start(index) shouldBe start
            start = UmmAlQuraCriterion.nextMonthStart(start)
        }
        start = months.start(months.index(1200, 1))
        (months.index(1200, 1)..months.index(1300, 1)).forEach { index ->
            withClue(index) { months.start(index) shouldBe start }
            start = UmmAlQuraCriterion.nextMonthStart(start)
        }
    }

    @Test
    fun `mean months join both astronomical edges`() {
        listOf(months.firstAstronomicalIndex, months.lastAstronomicalIndex).forEach { edge ->
            (edge - 36..edge + 36).forEach { index ->
                (months.start(index + 1) - months.start(index)).toInt() shouldBeInRange 29..30
            }
        }
        (UmmAlQuraMonths.FIRST_ASTRONOMICAL_YEAR - 3..UmmAlQuraMonths.FIRST_ASTRONOMICAL_YEAR + 2).forEach {
            yearLength(
                it,
            )
        }
        (UmmAlQuraMonths.LAST_ASTRONOMICAL_YEAR - 2..UmmAlQuraMonths.LAST_ASTRONOMICAL_YEAR + 3).forEach {
            yearLength(
                it,
            )
        }
    }

    private fun yearLength(year: Int) {
        (1..12).sumOf { uaq.monthLength(year, it) } shouldBeInRange 353..356
    }

    @Test
    fun `far years keep lunar months and round-trip`(): Unit =
        runBlocking {
            val farYears =
                Arb.choice(
                    Arb.int(-100_000 until UmmAlQuraMonths.FIRST_ASTRONOMICAL_YEAR),
                    Arb.int(UmmAlQuraMonths.LAST_ASTRONOMICAL_YEAR + 1..100_000),
                    Arb.int(Int.MIN_VALUE..Int.MIN_VALUE + 10_000),
                    Arb.int(Int.MAX_VALUE - 10_000..Int.MAX_VALUE),
                )
            checkAll(propertyConfig, farYears, Arb.int(1..12)) { year, month ->
                val length = uaq.monthLength(year, month)
                length shouldBeInRange 29..30
                (1..12).sumOf { uaq.monthLength(year, it) } shouldBeInRange 354..355
                val last = hijri(year, month, length)
                uaq.fromJdn(uaq.toJdn(last)) shouldBe last
                // The day after the last month of Int.MAX_VALUE is outside the supported range (tested below).
                if (year != Int.MAX_VALUE || month != 12) uaq.fromJdn(Jdn(uaq.toJdn(last).value + 1)).day shouldBe 1
            }
        }

    @Test
    fun `every year of Int is supported and nothing beyond`() {
        val first = uaq.toJdn(hijri(Int.MIN_VALUE, 1, 1))
        val last = uaq.toJdn(hijri(Int.MAX_VALUE, 12, uaq.monthLength(Int.MAX_VALUE, 12)))

        uaq.fromJdn(first) shouldBe hijri(Int.MIN_VALUE, 1, 1)
        uaq.fromJdn(last).year shouldBe Int.MAX_VALUE
        shouldThrow<IllegalArgumentException> { uaq.fromJdn(Jdn(first.value - 1)) }
        shouldThrow<IllegalArgumentException> { uaq.fromJdn(Jdn(last.value + 1)) }
    }
}
