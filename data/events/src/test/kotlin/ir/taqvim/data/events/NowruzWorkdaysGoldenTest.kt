/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.WorkdayCalculator
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.core.workdays.WorkdayResult
import ir.taqvim.data.events.generated.OfficialEvents
import java.io.File
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * T-504 golden for the Nowruz weeks of 1404 and 1405: with the generated official holidays (D-02/D-08) and a Friday
 * weekend (a user setting, not official data), workday answers match the Calendar Center's daily fixtures
 * (`golden/persian/official/{year}.csv`: weekday and official holiday per day).
 */
class NowruzWorkdaysGoldenTest {
    private val directory =
        File(requireNotNull(System.getProperty(DIRECTORY_PROPERTY)) { "$DIRECTORY_PROPERTY is not set" })

    private val calculator =
        WorkdayCalculator(
            EventLookup(OfficialEvents.ALL),
            WorkdayProfile(weekend = setOf(Weekday.FRIDAY), holidaySources = setOf(EventSource.IRAN_OFFICIAL)),
        )

    private data class OfficialDay(
        val jdn: Jdn,
        val workday: Boolean,
    )

    private fun officialDays(year: Int): List<OfficialDay> =
        File(directory, "$year.csv")
            .readLines()
            .filterNot { it.startsWith("#") }
            .drop(1)
            .map { it.split(',') }
            .map { row ->
                val (y, m, d) = row[PERSIAN_COLUMN].split('-').map(String::toInt)
                val jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, y, m, d))
                val offDay = row[WEEKDAY_COLUMN].toInt() == FRIDAY_ISO || row[HOLIDAY_COLUMN].toBooleanStrict()
                OfficialDay(jdn, !offDay)
            }

    @TestFactory
    fun `Nowruz weeks of 1404 and 1405`(): List<DynamicTest> =
        OFFICIAL_YEARS.flatMap { year ->
            val days = officialDays(year)
            val nowruzWeeks = days.take(NOWRUZ_SPAN_DAYS)
            nowruzWeeks.map { day ->
                DynamicTest.dynamicTest("$year next workday after JDN ${day.jdn.value}") {
                    val expected = days.first { it.jdn > day.jdn && it.workday }.jdn
                    calculator.nextWorkday(day.jdn) shouldBe WorkdayResult.Found(expected)
                    calculator.isWorkday(day.jdn) shouldBe day.workday
                }
            } +
                DynamicTest.dynamicTest("$year workdays in Farvardin") {
                    val farvardin = days.take(FARVARDIN_DAYS)
                    val end = Jdn(farvardin.last().jdn.value + 1)
                    calculator.workdaysBetween(farvardin.first().jdn, end) shouldBe
                        farvardin.count { it.workday }.toDouble()
                }
        }

    private companion object {
        const val DIRECTORY_PROPERTY = "taqvim.official.days.directory"
        val OFFICIAL_YEARS = listOf(1404, 1405)
        const val PERSIAN_COLUMN = 0
        const val WEEKDAY_COLUMN = 1
        const val HOLIDAY_COLUMN = 4
        const val FRIDAY_ISO = 5
        const val NOWRUZ_SPAN_DAYS = 14
        const val FARVARDIN_DAYS = 31
    }
}
