/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.GoldenFile
import kotlin.random.Random
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** Columns of the golden fixtures used here. */
private const val DAY_PERSIAN = 0
private const val DAY_HIJRI = 2
private const val DAY_GREGORIAN = 3
private const val MONTH_ID = 0
private const val MONTH_GREGORIAN = 1
private const val MONTH_LENGTH = 3

/** A-05 against the official calendars of 1404 and 1405 (golden/persian, golden/islamic-iran); joins per ADR-0027. */
class IranIslamicCalendarTest {
    private val iran = IranIslamicCalendar()

    private fun rows(path: String): List<List<String>> =
        GoldenFile
            .load("golden/$path")
            .lines
            .drop(1)
            .map { it.split(',') }

    private fun date(
        system: CalendarSystem,
        iso: String,
    ): CalendarDate {
        val (year, month, day) = iso.split('-').map(String::toInt)
        return CalendarDate(system, year, month, day)
    }

    private fun gregorianJdn(iso: String) = GregorianCalendarSystem.toJdn(date(CalendarSystem.GREGORIAN, iso))

    private fun hijri(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, day)

    @TestFactory
    fun `every day of the official calendars has its official lunar date`(): List<DynamicTest> =
        listOf("iran-official-1404-days.csv", "iran-official-1405-days.csv").map { name ->
            DynamicTest.dynamicTest(name) {
                val days = rows("persian/$name")
                days shouldHaveSize 365
                days
                    .filterNot { row ->
                        val jdn = gregorianJdn(row[DAY_GREGORIAN])
                        val expected = date(CalendarSystem.ISLAMIC, row[DAY_HIJRI])
                        iran.fromJdn(jdn) == expected && iran.toJdn(expected) == jdn &&
                            PersianCalendarSystem.toJdn(date(CalendarSystem.PERSIAN, row[DAY_PERSIAN])) == jdn
                    }.shouldBeEmpty()
            }
        }

    @Test
    fun `month starts and lengths match the published table`() {
        val months = rows("islamic-iran/official-month-starts-1446-1448.csv")

        months shouldHaveSize 26
        months
            .filterNot { row ->
                val (year, month) = row[MONTH_ID].split('-').map(String::toInt)
                val start = gregorianJdn(row[MONTH_GREGORIAN])
                val published = row[MONTH_LENGTH].isNotEmpty()
                val lengthMatches = !published || iran.monthLength(year, month) == row[MONTH_LENGTH].toInt()
                iran.toJdn(hijri(year, month, 1)) == start && lengthMatches && iran.isOfficial(start) == published
            }.shouldBeEmpty()
    }

    @Test
    fun `the crescent estimate joins the table at both edges`() {
        val firstOfficial = iran.toJdn(hijri(1446, 9, 1))
        val firstAfter = iran.toJdn(hijri(1448, 10, 1))

        iran.monthLength(1446, 8) shouldBeIn listOf(29, 30)
        iran.fromJdn(Jdn(firstOfficial.value - 1)) shouldBe hijri(1446, 8, iran.monthLength(1446, 8))
        iran.isOfficial(Jdn(firstOfficial.value - 1)) shouldBe false
        iran.isOfficial(firstOfficial) shouldBe true
        iran.isOfficial(firstAfter) shouldBe false
        iran.fromJdn(Jdn(firstAfter.value - 1)) shouldBe hijri(1448, 9, 30)
        IranOfficialMonthStarts.TABLE.next shouldBe (1448 to 10)
        (1440..1445).plus(1449..1455).forEach { year ->
            (1..12).forEach { month ->
                iran.toJdn(hijri(year, month, 1)) shouldBe IranCrescentCalendar.toJdn(hijri(year, month, 1))
            }
        }
    }

    @Test
    fun `tables off the crescent months are joined with 29 and 30 day months on both sides`() {
        val lengths = (1..12).map { IranCrescentCalendar.monthLength(1450, it) }
        val crescentStart = IranCrescentCalendar.toJdn(hijri(1450, 1, 1)).value

        listOf(-1L, 1L, 3L).forEach { shift ->
            val calendar = IranIslamicCalendar(IslamicMonthTable(1450, 1, crescentStart + shift, lengths))

            (1446..1454)
                .flatMap { year -> (1..12).map { year to it } }
                .filterNot { (year, month) ->
                    val length = calendar.monthLength(year, month)
                    val next = if (month == 12) hijri(year + 1, 1, 1) else hijri(year, month + 1, 1)
                    val start = calendar.toJdn(hijri(year, month, 1)).value
                    length in 29..30 && start + length == calendar.toJdn(next).value
                }.shouldBeEmpty()
            calendar.toJdn(hijri(1450, 1, 1)).value shouldBe crescentStart + shift
            calendar.toJdn(hijri(1446, 1, 1)) shouldBe IranCrescentCalendar.toJdn(hijri(1446, 1, 1))
            calendar.toJdn(hijri(1454, 12, 1)) shouldBe IranCrescentCalendar.toJdn(hijri(1454, 12, 1))
        }
    }

    @Test
    fun `every month from 1440 to 1455 has 29 or 30 days and months are contiguous`() {
        (1440..1455)
            .flatMap { year -> (1..12).map { year to it } }
            .filterNot { (year, month) ->
                val length = iran.monthLength(year, month)
                val next = if (month == 12) hijri(year + 1, 1, 1) else hijri(year, month + 1, 1)
                length in 29..30 && iran.toJdn(hijri(year, month, 1)).value + length == iran.toJdn(next).value
            }.shouldBeEmpty()
    }

    @Test
    fun `year lengths come from the published months`() {
        iran.yearLength(1447) shouldBe 354
        iran.isLeapYear(1447) shouldBe false
        iran.monthsInYear(1447) shouldBe 12
    }

    @Test
    fun `100 000 random days round-trip on both sides of the table`() {
        val random = Random(SEED)

        // AH 1270–1550 around the table; years across the whole range are covered by IranCrescentCalendarTest.
        (1..RANDOM_DAYS)
            .map { Jdn(random.nextLong(2_400_000L, 2_500_000L)) }
            .filterNot { iran.toJdn(iran.fromJdn(it)) == it }
            .shouldBeEmpty()
    }

    @Test
    fun `invalid dates and other systems are rejected`() {
        shouldThrow<IllegalArgumentException> { iran.toJdn(hijri(1447, 12, 30)) }
        shouldThrow<IllegalArgumentException> { iran.monthLength(1447, 13) }
        shouldThrow<IllegalArgumentException> { iran.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1446, 13, 0L, listOf(29)) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1446, 1, 0L, emptyList()) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1446, 1, 0L, listOf(28)) }
    }

    private companion object {
        const val SEED = 1447
        const val RANDOM_DAYS = 100_000
    }
}
