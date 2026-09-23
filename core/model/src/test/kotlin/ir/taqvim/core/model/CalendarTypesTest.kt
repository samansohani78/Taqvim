/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CalendarTypesTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    @Test
    fun `calendar systems and islamic variants are the planned sets`() {
        CalendarSystem.entries shouldContainExactly
            listOf(
                CalendarSystem.PERSIAN,
                CalendarSystem.ISLAMIC,
                CalendarSystem.GREGORIAN,
                CalendarSystem.NEPALI,
                CalendarSystem.HEBREW,
            )
        IslamicVariant.entries.map { it.name } shouldContainExactly
            listOf("IRAN_OFFICIAL", "UMM_AL_QURA", "TABULAR_16", "TABULAR_15", "CALCULATED_OBSERVATIONAL")
    }

    @Test
    fun `calendar date formats in its own calendar`() {
        CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1).toString() shouldBe "PERSIAN 1405-01-01"
        CalendarDate(CalendarSystem.GREGORIAN, 7, 12, 31).toIsoLikeString() shouldBe "0007-12-31"
        CalendarDate(CalendarSystem.GREGORIAN, -44, 3, 15).toIsoLikeString() shouldBe "-0044-03-15"
        CalendarDate(CalendarSystem.ISLAMIC, 1447, 9, 30) shouldBe CalendarDate(CalendarSystem.ISLAMIC, 1447, 9, 30)
    }

    @Test
    fun `calendar date rejects non-positive month and day`() {
        shouldThrow<IllegalArgumentException> {
            CalendarDate(CalendarSystem.NEPALI, 2082, 0, 1)
        }.message.orEmpty() shouldContain
            "month"
        shouldThrow<IllegalArgumentException> {
            CalendarDate(CalendarSystem.NEPALI, 2082, 1, 0)
        }.message.orEmpty() shouldContain
            "day"
    }

    @Test
    fun `weekdays follow ISO order and arithmetic`() {
        Weekday.MONDAY.isoNumber shouldBe 1
        Weekday.SUNDAY.isoNumber shouldBe 7
        Weekday.ofIsoNumber(5) shouldBe Weekday.FRIDAY
        Weekday.SATURDAY + 2 shouldBe Weekday.MONDAY
        Weekday.MONDAY + -1 shouldBe Weekday.SUNDAY
        Weekday.FRIDAY.daysAfter(Weekday.SATURDAY) shouldBe 6
        Weekday.SATURDAY.daysAfter(Weekday.FRIDAY) shouldBe 1
        shouldThrow<IllegalArgumentException> { Weekday.ofIsoNumber(0) }
        shouldThrow<IllegalArgumentException> { Weekday.ofIsoNumber(8) }
    }

    @Test
    fun `weekday plus seven is identity`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(0..6), Arb.int(-10_000..10_000)) { index, weeks ->
                val weekday = Weekday.entries[index]
                weekday + 7 * weeks shouldBe weekday
                (weekday + index).daysAfter(weekday) shouldBe index
            }
        }

    @Test
    fun `weekday plus seven is identity at the first and last weekday and the widest week offsets`() {
        listOf(0, 6).forEach { index ->
            listOf(-10_000, 0, 10_000).forEach { weeks ->
                val weekday = Weekday.entries[index]
                weekday + 7 * weeks shouldBe weekday
                (weekday + index).daysAfter(weekday) shouldBe index
            }
        }
    }

    @Test
    fun `coordinates validate their ranges`() {
        Coordinates(35.6892, 51.389).elevationMeters shouldBe 0.0
        Coordinates(-90.0, 180.0, -430.0).latitude shouldBe -90.0
        listOf(
            { Coordinates(90.1, 0.0) },
            { Coordinates(-90.1, 0.0) },
            { Coordinates(Double.NaN, 0.0) },
            { Coordinates(0.0, 180.5) },
            { Coordinates(0.0, -180.5) },
            { Coordinates(0.0, Double.POSITIVE_INFINITY) },
            { Coordinates(0.0, 0.0, Double.NaN) },
        ).forEach { shouldThrow<IllegalArgumentException> { it() } }
    }

    @Test
    fun `minute of day is exact and wraps around midnight`() {
        val fajr = MinuteOfDay.of(4, 37)

        fajr.value shouldBe 277
        fajr.hour shouldBe 4
        fajr.minute shouldBe 37
        fajr.toString() shouldBe "04:37"
        MinuteOfDay.of(23, 59).plusWrapping(2) shouldBe MinuteOfDay(1)
        MinuteOfDay(0).plusWrapping(-1) shouldBe MinuteOfDay(1439)
        (MinuteOfDay(10) < MinuteOfDay(11)) shouldBe true
        MinuteOfDay.MINUTES_PER_DAY shouldBe 1440
    }

    @Test
    fun `minute of day rejects out-of-range values`() {
        shouldThrow<IllegalArgumentException> { MinuteOfDay(-1) }
        shouldThrow<IllegalArgumentException> { MinuteOfDay(1440) }
        shouldThrow<IllegalArgumentException> { MinuteOfDay.of(24, 0) }
        shouldThrow<IllegalArgumentException> { MinuteOfDay.of(-1, 0) }
        shouldThrow<IllegalArgumentException> { MinuteOfDay.of(0, 60) }
        shouldThrow<IllegalArgumentException> { MinuteOfDay.of(0, -1) }
    }

    @Test
    fun `wrapping keeps minute of day in range`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(0..1439), Arb.int(-100_000..100_000)) { value, delta ->
                val moved = MinuteOfDay(value).plusWrapping(delta)
                moved.plusWrapping(-delta) shouldBe MinuteOfDay(value)
            }
        }

    @Test
    fun `wrapping keeps minute of day in range at midnight, end of day and the widest deltas`() {
        listOf(0, 1439).forEach { value ->
            listOf(-100_000, 0, 100_000).forEach { delta ->
                val moved = MinuteOfDay(value).plusWrapping(delta)
                moved.plusWrapping(-delta) shouldBe MinuteOfDay(value)
            }
        }
    }
}
