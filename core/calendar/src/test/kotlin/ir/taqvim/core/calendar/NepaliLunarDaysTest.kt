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
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.TithiObservance.MIDNIGHT
import ir.taqvim.core.calendar.TithiObservance.SUNRISE
import ir.taqvim.core.calendar.TithiObservance.SUNSET
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/**
 * ADR-0038: lunar festival days against the Government of Nepal's public holiday notices for BS 2082 and 2083 (Nepal
 * Rajpatra, Ministry of Home Affairs), plus Teej and Gyalpo Lhosar, which the notices also date.
 */
class NepaliLunarDaysTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private data class Case(
        val name: String,
        val year: Int,
        val month: Int,
        val tithi: Int,
        val observance: TithiObservance,
        val expected: List<String>,
        val endTithi: Int? = null,
        val endOffsetDays: Int = 0,
    )

    private val cases =
        listOf(
            Case("Buddha Jayanti", 2082, 1, 15, SUNRISE, listOf("2025-05-12")),
            Case("Raksha Bandhan", 2082, 4, 15, SUNRISE, listOf("2025-08-09")),
            Case("Janmashtami", 2082, 4, 23, SUNRISE, listOf("2025-08-16")),
            Case("Teej", 2082, 5, 3, SUNRISE, listOf("2025-08-26")),
            Case("Ghatasthapana", 2082, 6, 1, SUNRISE, listOf("2025-09-22")),
            Case("Dashain", 2082, 6, 7, SUNRISE, span("2025-09-29", "2025-10-04"), endTithi = 12),
            Case("Tihar", 2082, 6, 30, SUNSET, span("2025-10-20", "2025-10-24"), endTithi = 2, endOffsetDays = 1),
            Case("Chhath", 2082, 7, 6, SUNRISE, listOf("2025-10-27")),
            Case("Dhanya Purnima", 2082, 8, 15, SUNRISE, listOf("2025-12-04")),
            Case("Sonam Lhochhar", 2082, 10, 1, SUNRISE, listOf("2026-01-19")),
            Case("Maha Shivaratri", 2082, 10, 29, MIDNIGHT, listOf("2026-02-15")),
            Case("Gyalpo Lhosar", 2082, 11, 1, SUNRISE, listOf("2026-02-18")),
            Case("Ram Navami", 2082, 12, 9, SUNRISE, listOf("2026-03-27")),
            Case("Buddha Jayanti", 2083, 1, 15, SUNRISE, listOf("2026-05-01")),
            Case("Raksha Bandhan", 2083, 4, 15, SUNRISE, listOf("2026-08-28")),
            Case("Janmashtami", 2083, 4, 23, SUNRISE, listOf("2026-09-04")),
            Case("Teej", 2083, 5, 3, SUNRISE, listOf("2026-09-14")),
            Case("Ghatasthapana", 2083, 6, 1, SUNRISE, listOf("2026-10-11")),
            Case("Dashain", 2083, 6, 7, SUNRISE, span("2026-10-17", "2026-10-23"), endTithi = 12),
            Case("Tihar", 2083, 6, 30, SUNSET, span("2026-11-08", "2026-11-12"), endTithi = 2, endOffsetDays = 1),
            Case("Chhath", 2083, 7, 6, SUNRISE, listOf("2026-11-15")),
            Case("Dhanya Purnima", 2083, 8, 15, SUNRISE, listOf("2026-12-24")),
            Case("Sonam Lhochhar", 2083, 10, 1, SUNRISE, listOf("2027-02-07")),
            Case("Maha Shivaratri", 2083, 10, 29, MIDNIGHT, listOf("2027-03-06")),
            Case("Gyalpo Lhosar", 2083, 11, 1, SUNRISE, listOf("2027-03-09")),
            // The notice for 2083 lists no Ram Navami: it falls in Baisakh 2084.
            Case("Ram Navami", 2083, 12, 9, SUNRISE, emptyList()),
        )

    @Test
    fun `festival days match the official notices of 2082 and 2083`() {
        cases.forEach { case ->
            withClue("${case.name} ${case.year}") {
                days(case).map { it.iso() } shouldBe case.expected
            }
        }
    }

    @Test
    fun `a festival late in Chaitra can fall in the next year`() {
        val ramNavami = NepaliLunarDays.days(2084, 12, 9)
        NepaliCalendarSystem.fromJdn(ramNavami.first()).month shouldBe 1
    }

    @Test
    fun `solar months hold zero, one or two new moons and every year twelve or thirteen`() {
        val counts = (1_900L..2_300L).associateWith { year -> (1..12).map { LunarMonth.newMoonCount(year, it) } }
        counts.values.forEach { it.sum() shouldBeInRange 12..13 }
        counts.values.flatten().toSet() shouldBe setOf(0, 1, 2)
        val kshaya = counts.entries.first { (_, months) -> 0 in months.dropLast(1) }
        val month = kshaya.value.indexOf(0) + 1
        LunarMonth.of(kshaya.key, month) shouldBe null
        NepaliLunarDays.days(kshaya.key.toInt(), month, 15).shouldBeEmpty()
    }

    @Test
    fun `an adhika month names the second lunation`() {
        val (year, month) =
            (2_000L..2_100L)
                .asSequence()
                .flatMap { year -> (1..11).map { year to it } }
                .first { (year, month) -> LunarMonth.newMoonCount(year, month) == 2 }
        val solarStart = NepaliMonthStarts.startJdn(year, month)
        val nextStart = NepaliMonthStarts.startJdn(year, month + 1)
        val firstTithi = NepaliLunarDays.days(year.toInt(), month, 1).single().value
        firstTithi shouldBeInRange solarStart + 20..nextStart
        NepaliLunarDays.days(year.toInt(), month, 15).single().value shouldBeInRange nextStart..nextStart + 20
    }

    @Test
    fun `far years give at most two days per month and keep them in the year`(): Unit =
        runBlocking {
            val years =
                Arb.choice(
                    Arb.int(-100_000..100_000),
                    Arb.int(Int.MIN_VALUE..Int.MIN_VALUE + 1_000),
                    Arb.int(Int.MAX_VALUE - 1_000..Int.MAX_VALUE),
                )
            checkAll(propertyConfig, years, Arb.int(1..12), Arb.int(1..30)) { year, month, tithi ->
                val days = NepaliLunarDays.days(year, month, tithi, SUNRISE)
                days.size shouldBeInRange 0..2
                val first = NepaliCalendarSystem.firstDayOfYear(year).value
                days.forEach { it.value shouldBeInRange first..first + 366 }
            }
        }

    @Test
    fun `invalid arguments are rejected`() {
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 0, 1) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 13, 1) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 1, 0) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 1, 31) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 1, 1, endTithi = 31) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 1, 1, endTithi = 0) }
        shouldThrow<IllegalArgumentException> { NepaliLunarDays.days(2083, 1, 1, endOffsetDays = -1) }
        TithiObservance.entries.map { it.name } shouldContain "MIDNIGHT"
    }

    private fun days(case: Case): List<Jdn> =
        NepaliLunarDays.days(case.year, case.month, case.tithi, case.observance, case.endTithi, case.endOffsetDays)

    private companion object {
        const val UNIX_EPOCH_JDN = 2_440_588L

        fun Jdn.iso(): String = LocalDate.fromEpochDays((value - UNIX_EPOCH_JDN).toInt()).toString()

        fun span(
            from: String,
            to: String,
        ): List<String> {
            val start = LocalDate.parse(from).toEpochDays()
            return (start..LocalDate.parse(to).toEpochDays()).map { LocalDate.fromEpochDays(it).toString() }
        }
    }
}
