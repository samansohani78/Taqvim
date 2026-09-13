/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.HolidayCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.events.generated.OfficialEvents
import java.io.File
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * T-303 golden: with the generated official dataset (D-02/D-08), a day is a holiday exactly when the Calendar
 * Center's official calendar marks it so. Uses the daily fixtures of :core:calendar
 * (`golden/persian/iran-official-{year}-days.csv`); only 1404 and 1405 are available (1403 and 1406 pending).
 */
class OfficialHolidaysGoldenTest {
    private val directory =
        File(requireNotNull(System.getProperty(DIRECTORY_PROPERTY)) { "$DIRECTORY_PROPERTY is not set" })

    private val calendar =
        HolidayCalendar(EventLookup(OfficialEvents.ALL), setOf(EventSource.IRAN_OFFICIAL), weekend = emptySet())

    @TestFactory
    fun `the dataset's holidays are exactly the official holidays`(): List<DynamicTest> =
        OFFICIAL_YEARS.map { year ->
            DynamicTest.dynamicTest("$year SH") {
                val rows =
                    File(directory, "iran-official-$year-days.csv")
                        .readLines()
                        .filterNot { it.startsWith("#") }
                        .drop(1)
                        .map { it.split(',') }
                rows shouldHaveSize DAYS_IN_YEAR
                val mismatches =
                    rows
                        .filter { row ->
                            val (y, m, d) = row[PERSIAN_COLUMN].split('-').map(String::toInt)
                            val jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, y, m, d))
                            calendar.isHoliday(jdn) != row[HOLIDAY_COLUMN].toBooleanStrict()
                        }.map { it[PERSIAN_COLUMN] }
                withClue("days where dataset and official calendar disagree") { mismatches.shouldBeEmpty() }
            }
        }

    private companion object {
        const val DIRECTORY_PROPERTY = "taqvim.official.days.directory"
        val OFFICIAL_YEARS = listOf(1404, 1405)
        const val DAYS_IN_YEAR = 365
        const val PERSIAN_COLUMN = 0
        const val HOLIDAY_COLUMN = 4
    }
}
