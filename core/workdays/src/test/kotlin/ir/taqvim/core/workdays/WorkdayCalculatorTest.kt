/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.workdays

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventFlag
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** T-504 on a synthetic profile, checked against an independent java.time oracle. No real-world data. */
class WorkdayCalculatorTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private fun jdn(date: LocalDate): Jdn = Jdn(date.toEpochDay() + JDN_OF_UNIX_EPOCH)

    private fun date(jdn: Jdn): LocalDate = LocalDate.ofEpochDay(jdn.value - JDN_OF_UNIX_EPOCH)

    private fun holiday(
        id: String,
        month: Int,
        day: Int,
        flags: Set<EventFlag> = emptySet(),
    ) = EventDefinition(
        id = EventId(id),
        calendar = CalendarSystem.GREGORIAN,
        source = EventSource.INTERNATIONAL,
        category = EventCategory.CULTURAL,
        isHoliday = true,
        title = LocalizedText(mapOf(LocalizedText.PERSIAN to "test")),
        rule = EventRule.Fixed(month, day),
        flags = flags,
    )

    private val lookup =
        EventLookup(listOf(holiday("test.new-year", 1, 1), holiday("test.half", 3, 21, setOf(EventFlag.HALF_DAY))))
    private val leave = LeaveRange(jdn(LEAVE_FIRST), jdn(LEAVE_LAST))
    private val profile =
        WorkdayProfile(setOf(Weekday.FRIDAY), setOf(EventSource.INTERNATIONAL), personalLeave = listOf(leave))
    private val calculator = WorkdayCalculator(lookup, profile)

    /** Independent oracle for the synthetic profile. */
    private fun oracleValue(day: LocalDate): Double =
        when {
            day.dayOfWeek == DayOfWeek.FRIDAY -> 0.0
            !day.isBefore(LEAVE_FIRST) && !day.isAfter(LEAVE_LAST) -> 0.0
            day.monthValue == 1 && day.dayOfMonth == 1 -> 0.0
            day.monthValue == 3 && day.dayOfMonth == 21 -> 0.5
            else -> 1.0
        }

    private fun oracleAdd(
        start: LocalDate,
        workdays: Int,
    ): LocalDate {
        val step = if (workdays >= 0) 1L else -1L
        var day = start
        var remaining = kotlin.math.abs(workdays)
        if (remaining == 0) {
            while (oracleValue(day) == 0.0) day = day.plusDays(1)
            return day
        }
        while (remaining > 0) {
            day = day.plusDays(step)
            if (oracleValue(day) > 0.0) remaining--
        }
        return day
    }

    private fun oracleBetween(
        from: LocalDate,
        until: LocalDate,
    ): Double {
        val (low, high, sign) = if (until.isBefore(from)) Triple(until, from, -1.0) else Triple(from, until, 1.0)
        return sign * generateSequence(low) { it.plusDays(1) }.takeWhile { it.isBefore(high) }.sumOf(::oracleValue)
    }

    @TestFactory
    fun `100 cases agree with the oracle`(): List<DynamicTest> {
        val random = Random(SEED)
        val base = LocalDate.of(2025, 1, 1)
        val adds =
            (1..50).map {
                val start = base.plusDays(random.nextLong(0, 1_095))
                val n = random.nextInt(-25, 26)
                DynamicTest.dynamicTest("addWorkdays($start, $n)") {
                    calculator.addWorkdays(jdn(start), n) shouldBe WorkdayResult.Found(jdn(oracleAdd(start, n)))
                }
            }
        val between =
            (1..50).map {
                val from = base.plusDays(random.nextLong(0, 1_095))
                val until = from.plusDays(random.nextLong(-60, 61))
                DynamicTest.dynamicTest("workdaysBetween($from, $until)") {
                    calculator.workdaysBetween(jdn(from), jdn(until)) shouldBe oracleBetween(from, until)
                }
            }
        return adds + between
    }

    @Test
    fun `semantics of next, previous and zero`() {
        val thursday = jdn(LocalDate.of(2026, 9, 10))
        val friday = jdn(LocalDate.of(2026, 9, 11))

        calculator.nextWorkday(thursday) shouldBe WorkdayResult.Found(jdn(LocalDate.of(2026, 9, 12)))
        calculator.previousWorkday(jdn(LocalDate.of(2026, 9, 12))) shouldBe WorkdayResult.Found(thursday)
        calculator.addWorkdays(thursday, 0) shouldBe WorkdayResult.Found(thursday)
        calculator.addWorkdays(friday, 0) shouldBe WorkdayResult.Found(jdn(LocalDate.of(2026, 9, 12)))
        calculator.workValue(jdn(LocalDate.of(2026, 1, 1))) shouldBe 0.0
        calculator.workValue(jdn(LocalDate.of(2026, 3, 21))) shouldBe 0.5
        calculator.isWorkday(jdn(LocalDate.of(2026, 3, 21))) shouldBe true
        calculator.workValue(jdn(LEAVE_FIRST.plusDays(1))) shouldBe 0.0
    }

    @Test
    fun `profile options change the day values`() {
        val halfDay = jdn(LocalDate.of(2026, 3, 21))
        val newYear = jdn(LocalDate.of(2026, 1, 1))

        WorkdayCalculator(lookup, profile.copy(halfDays = HalfDayPolicy.FULL_WORKDAY)).workValue(halfDay) shouldBe 1.0
        WorkdayCalculator(lookup, profile.copy(holidaySources = emptySet())).workValue(newYear) shouldBe 1.0
        WorkdayCalculator(lookup, profile.copy(personalLeave = emptyList())).workValue(jdn(LEAVE_FIRST)) shouldBe
            oracleValue(LEAVE_FIRST).let { if (LEAVE_FIRST.dayOfWeek == DayOfWeek.FRIDAY) 0.0 else 1.0 }
    }

    @Test
    fun `a profile without workdays reports a failure instead of looping`() {
        val never = WorkdayCalculator(lookup, profile.copy(weekend = Weekday.entries.toSet()))
        val day = jdn(LocalDate.of(2026, 9, 13))

        never.nextWorkday(day) shouldBe WorkdayResult.NoWorkday(WorkdayCalculator.MAX_SEARCH_DAYS)
        never.addWorkdays(day, 3).shouldBeInstanceOf<WorkdayResult.NoWorkday>()
        never.addWorkdays(day, 0).shouldBeInstanceOf<WorkdayResult.NoWorkday>()
        shouldThrow<IllegalArgumentException> { LeaveRange(day, Jdn(day.value - 1)) }
    }

    @Test
    fun `adding workdays always lands on a workday`(): Unit =
        runBlocking {
            checkAll(propertyConfig, Arb.int(0..1_500), Arb.int(-30..30)) { offset, n ->
                val start = jdn(LocalDate.of(2025, 1, 1).plusDays(offset.toLong()))
                val result = calculator.addWorkdays(start, n)
                result.shouldBeInstanceOf<WorkdayResult.Found>()
                calculator.isWorkday(result.jdn) shouldBe true
                oracleValue(date(result.jdn)) shouldBe calculator.workValue(result.jdn)
            }
        }

    @Test
    fun `adding workdays always lands on a workday at the offset and count bounds`() {
        listOf(0, 1_500).forEach { offset ->
            listOf(-30, 0, 30).forEach { n ->
                val start = jdn(LocalDate.of(2025, 1, 1).plusDays(offset.toLong()))
                val result = calculator.addWorkdays(start, n)
                result.shouldBeInstanceOf<WorkdayResult.Found>()
                calculator.isWorkday(result.jdn) shouldBe true
                oracleValue(date(result.jdn)) shouldBe calculator.workValue(result.jdn)
            }
        }
    }

    private companion object {
        const val JDN_OF_UNIX_EPOCH = 2_440_588L
        const val SEED = 504
        val LEAVE_FIRST: LocalDate = LocalDate.of(2026, 6, 10)
        val LEAVE_LAST: LocalDate = LocalDate.of(2026, 6, 14)
    }
}
