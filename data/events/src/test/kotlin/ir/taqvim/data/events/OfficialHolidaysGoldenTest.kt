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
 * Center's official calendar marks it so, through the runtime path the app uses — `HolidayCalendar` over the
 * generated catalogue and the **computed** Iranian lunar calendar. Uses the daily fixtures of :core:calendar
 * (`golden/persian/official/{year}.csv`), of which 25 years are imported (1381–1405, main@33d0418, main@c92223b).
 *
 * [OFFICIAL_YEARS] lists the 13 of them whose computed lunar months fall on the same days as the months the calendar
 * printed, so an Islamic-dated holiday lands on the official day: 1381 and 1394–1405
 * (`docs/data-todo/iran-holiday-history.md`, the "lunar / announced" column). In 1382–1393 the computed calendar
 * starts some months a day earlier or later, and in 1383–1385 an announcement moved a month after the calendar was
 * printed, so those years' Islamic holidays legitimately differ by a day here; the records themselves are checked
 * against all 25 years, with the printed lunar dates, by `IranOfficialHolidayHistoryTest` in :tools:dataset. 1406 is
 * not yet published by the Calendar Center.
 */
class OfficialHolidaysGoldenTest {
    private val directory =
        File(requireNotNull(System.getProperty(DIRECTORY_PROPERTY)) { "$DIRECTORY_PROPERTY is not set" })

    private val calendar =
        HolidayCalendar(
            EventLookup(OfficialEvents.ALL, astronomy = SkyAstronomicalEventSource),
            setOf(EventSource.IRAN_OFFICIAL),
            weekend = emptySet(),
        )

    @TestFactory
    fun `the dataset's holidays are exactly the official holidays`(): List<DynamicTest> =
        OFFICIAL_YEARS.map { year ->
            DynamicTest.dynamicTest("$year SH") {
                val rows =
                    File(directory, "$year.csv")
                        .readLines()
                        .filterNot { it.startsWith("#") }
                        .drop(1)
                        .map { it.split(',') }
                rows shouldHaveSize daysIn(year)
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

    /** The length of Persian [year], so a leap year such as 1403 is not read as a short one. */
    private fun daysIn(year: Int): Int =
        (
            PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year + 1, 1, 1)) -
                PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))
        ).toInt()

    private companion object {
        const val DIRECTORY_PROPERTY = "taqvim.official.days.directory"
        val OFFICIAL_YEARS = listOf(1381) + (1394..1405).toList()
        const val PERSIAN_COLUMN = 0
        const val HOLIDAY_COLUMN = 4
    }
}
