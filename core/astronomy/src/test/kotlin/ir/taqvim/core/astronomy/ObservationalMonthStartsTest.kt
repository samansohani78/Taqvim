/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IranOfficialMonthStarts
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import kotlin.math.abs
import org.junit.jupiter.api.Test

/** A-06 month starts against the Iranian official table (A-05) and the Umm al-Qura calendar (A-04). */
class ObservationalMonthStartsTest {
    private val published = IranIslamicCalendar(IranOfficialMonthStarts.TABLE)

    private fun firstDay(
        calendar: CalendarArithmetic,
        year: Int,
        month: Int,
    ): Long = calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, year, month, 1)).value

    private fun months(
        firstYear: Int,
        lastYear: Int,
    ) = (firstYear..lastYear).flatMap { year -> (1..12).map { year to it } }

    /** Month starts covered by the published table. */
    private val officialMonths =
        months(1446, 1448).filter { (year, month) ->
            IranOfficialMonthStarts.TABLE.covers(firstDay(published, year, month))
        }

    private fun differencesFromOfficial(estimate: CalendarArithmetic): List<Long> =
        officialMonths.map { (year, month) -> firstDay(estimate, year, month) - firstDay(published, year, month) }

    @Test
    fun `the Iran calibration agrees with at least 90 percent of the official month starts`() {
        val differences = differencesFromOfficial(IranIslamicCalendar(IranCrescentCalibration.table(1446, 1448)))

        officialMonths.size shouldBe 25
        println("A-06 Iran calibration vs official: ${differences.count { it == 0L }} of 25 agree")
        differences.count { it == 0L }.toDouble() / differences.size shouldBeGreaterThanOrEqual 0.9
    }

    @Test
    fun `a single Tehran site at class C is never earlier than the official calendar`() {
        val tehranOnly = ObservationalMonthStarts.table(TEHRAN, 1446, 1448)
        val differences = differencesFromOfficial(IranIslamicCalendar(tehranOnly))

        differences.toSet() shouldContainOnly setOf(0L, 1L)
    }

    @Test
    fun `Makkah estimates stay within a day of Umm al-Qura`() {
        val estimate = IranIslamicCalendar(ObservationalMonthStarts.table(MAKKAH, 1440, 1447))
        val differences =
            months(1440, 1447).map { (year, month) ->
                firstDay(estimate, year, month) - firstDay(UmmAlQuraCalendar, year, month)
            }

        println("A-06 Makkah vs Umm al-Qura: ${differences.count { it == 0L }} of ${differences.size} identical")
        differences.count { abs(it) <= 1 }.toDouble() / differences.size shouldBeGreaterThanOrEqual 0.95
    }

    @Test
    fun `tables are contiguous 29 and 30 day months`() {
        val table = ObservationalMonthStarts.table(TEHRAN, 1447, 1447, CrescentVisibilityClass.B)

        table.monthCount shouldBe 12
        (1..12).all { month -> IranIslamicCalendar(table).monthLength(1447, month) in 29..30 } shouldBe true
        shouldThrow<IllegalArgumentException> { ObservationalMonthStarts.table(TEHRAN, 1448, 1447) }
        shouldThrow<IllegalArgumentException> { ObservationalMonthStarts.table(emptyList(), 1447, 1447) }
        IranCrescentCalibration.SITES.size shouldBe 5
    }

    private companion object {
        val TEHRAN = Coordinates(35.70, 51.42)
        val MAKKAH = Coordinates(21.4225, 39.8262)
    }
}
