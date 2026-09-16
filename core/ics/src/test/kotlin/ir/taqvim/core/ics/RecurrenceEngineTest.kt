/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class RecurrenceEngineTest {
    private val gregorian = RecurrenceEngine(GregorianCalendarSystem)
    private val persian = RecurrenceEngine(PersianCalendarSystem)

    private fun g(iso: String) = date(CalendarSystem.GREGORIAN, iso)

    private fun p(iso: String) = date(CalendarSystem.PERSIAN, iso)

    private fun date(
        system: CalendarSystem,
        iso: String,
    ): CalendarDate {
        val (year, month, day) = iso.split('-').map(String::toInt)
        return CalendarDate(system, year, month, day)
    }

    private fun rule(text: String): RecurrenceRule =
        requireNotNull(
            IcsValues.recurrence(ContentLine(1, "RRULE", emptyMap(), text), mutableListOf()),
        ).toRecurrenceRule()

    private fun gregorianDays(
        start: String,
        rule: RecurrenceRule,
        limit: Int = 100,
    ): List<String> =
        gregorian
            .occurrences(
                g(start),
                rule,
            ).take(limit)
            .map { GregorianCalendarSystem.fromJdn(it).toIsoLikeString() }
            .toList()

    private fun persianDays(
        start: String,
        rule: RecurrenceRule,
        limit: Int,
    ): List<String> =
        persian
            .occurrences(
                p(start),
                rule,
            ).take(limit)
            .map { PersianCalendarSystem.fromJdn(it).toIsoLikeString() }
            .toList()

    /** RFC 5545 §3.8.5.3 examples (DTSTART dates in 1997), verified by brute force. */
    @TestFactory
    fun `RFC 5545 recurrence examples`(): List<DynamicTest> =
        RFC_EXAMPLES.map { (start, text, expected) ->
            DynamicTest.dynamicTest(text) { gregorianDays(start, rule(text)) shouldBe expected }
        }

    @Test
    fun `30 Esfand each year under the three invalid-date policies`() {
        val yearly = RecurrenceRule(Frequency.YEARLY)

        persianDays("1403-12-30", yearly, 3) shouldBe listOf("1403-12-30", "1408-12-30", "1412-12-30")
        persianDays("1403-12-30", yearly.copy(invalidDates = InvalidDatePolicy.NEXT_DAY), 6) shouldBe
            listOf("1403-12-30", "1405-01-01", "1406-01-01", "1407-01-01", "1408-01-01", "1408-12-30")
        persianDays("1403-12-30", yearly.copy(invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH), 6) shouldBe
            listOf("1403-12-30", "1404-12-29", "1405-12-29", "1406-12-29", "1407-12-29", "1408-12-30")
    }

    @Test
    fun `29 February, the 31st and negative month days under each policy`() {
        val yearly = RecurrenceRule(Frequency.YEARLY)
        gregorianDays("2024-02-29", yearly, 2) shouldBe listOf("2024-02-29", "2028-02-29")
        gregorianDays("2024-02-29", yearly.copy(invalidDates = InvalidDatePolicy.NEXT_DAY), 3) shouldBe
            listOf("2024-02-29", "2025-03-01", "2026-03-01")
        gregorianDays("2024-02-29", yearly.copy(invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH), 3) shouldBe
            listOf("2024-02-29", "2025-02-28", "2026-02-28")

        val monthly = RecurrenceRule(Frequency.MONTHLY)
        persianDays("1404-06-31", monthly, 3) shouldBe listOf("1404-06-31", "1405-01-31", "1405-02-31")
        persianDays("1404-06-31", monthly.copy(invalidDates = InvalidDatePolicy.NEXT_DAY), 3) shouldBe
            listOf("1404-06-31", "1404-08-01", "1404-09-01")
        persianDays("1404-06-31", monthly.copy(invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH), 3) shouldBe
            listOf("1404-06-31", "1404-07-30", "1404-08-30")

        val firstOfShortMonths = RecurrenceRule(Frequency.MONTHLY, byMonthDay = listOf(-31))
        persianDays("1404-06-01", firstOfShortMonths, 2) shouldBe listOf("1404-06-01", "1405-01-01")
        persianDays("1404-06-01", firstOfShortMonths.copy(invalidDates = InvalidDatePolicy.NEXT_DAY), 3) shouldBe
            listOf("1404-06-01", "1404-07-01", "1404-08-01")
    }

    @Test
    fun `tabular Islamic leap day recurs only in leap years`() {
        val engine = RecurrenceEngine(TabularIslamicCalendar.TYPE_II)
        val start = CalendarDate(CalendarSystem.ISLAMIC, 1447, 12, 30)

        engine
            .occurrences(start, RecurrenceRule(Frequency.YEARLY))
            .take(2)
            .map { TabularIslamicCalendar.TYPE_II.fromJdn(it) }
            .toList() shouldBe listOf(start, CalendarDate(CalendarSystem.ISLAMIC, 1450, 12, 30))
    }

    @Test
    fun `bounds, start counting and rules that never match`() {
        gregorianDays(
            "2026-03-01",
            RecurrenceRule(Frequency.WEEKLY, count = 3, byDay = listOf(WeekdayNum(Weekday.MONDAY))),
        ) shouldBe
            listOf("2026-03-01", "2026-03-02", "2026-03-09")
        gregorianDays(
            "2026-03-01",
            RecurrenceRule(Frequency.DAILY, interval = 7, until = gregorianJdn("2026-03-15")),
        ) shouldBe
            listOf("2026-03-01", "2026-03-08", "2026-03-15")
        val never =
            RecurrenceRule(Frequency.MONTHLY, byDay = listOf(WeekdayNum(Weekday.MONDAY, 5)), byMonthDay = listOf(1))
        gregorianDays("2026-01-01", never) shouldBe listOf("2026-01-01")
        shouldThrow<IllegalArgumentException> {
            gregorian.occurrences(
                p("1405-01-01"),
                RecurrenceRule(Frequency.DAILY),
            )
        }
    }

    @Test
    fun `BYDAY and BYMONTHDAY limit a daily rule before COUNT`() {
        gregorianDays("2026-09-14", rule("FREQ=DAILY;BYDAY=MO;COUNT=3")) shouldBe
            listOf("2026-09-14", "2026-09-21", "2026-09-28")
        gregorianDays("2026-09-14", rule("FREQ=DAILY;BYDAY=SA,SU;COUNT=5")) shouldBe
            listOf("2026-09-14", "2026-09-19", "2026-09-20", "2026-09-26", "2026-09-27")
        gregorianDays("2026-01-31", rule("FREQ=DAILY;BYMONTHDAY=-1;COUNT=4")) shouldBe
            listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30")
        gregorianDays("2026-01-15", rule("FREQ=DAILY;BYMONTHDAY=15,31;COUNT=5")) shouldBe
            listOf("2026-01-15", "2026-01-31", "2026-02-15", "2026-03-15", "2026-03-31")
        gregorianDays("2026-02-13", rule("FREQ=DAILY;BYDAY=FR;BYMONTHDAY=13"), limit = 3) shouldBe
            listOf("2026-02-13", "2026-03-13", "2026-11-13")
        gregorianDays("2026-09-14", rule("FREQ=DAILY;INTERVAL=2;BYDAY=MO,WE;COUNT=4")) shouldBe
            listOf("2026-09-14", "2026-09-16", "2026-09-28", "2026-09-30")
        // The ordinal the RFC forbids here is ignored: BYDAY=2MO limits by weekday only.
        gregorianDays("2026-09-14", rule("FREQ=DAILY;BYDAY=2MO;COUNT=3")) shouldBe
            listOf("2026-09-14", "2026-09-21", "2026-09-28")
        gregorianDays("2026-09-14", RecurrenceRule(Frequency.DAILY, count = 3)) shouldBe
            listOf("2026-09-14", "2026-09-15", "2026-09-16")
    }

    @Test
    fun `a daily rule in the Persian calendar counts its own month days`() {
        persian
            .occurrences(p("1405-01-31"), rule("FREQ=DAILY;BYMONTHDAY=-1;COUNT=3"))
            .take(3)
            .map { PersianCalendarSystem.fromJdn(it).toIsoLikeString() }
            .toList() shouldBe listOf("1405-01-31", "1405-02-31", "1405-03-31")
    }

    @Test
    fun `rule invariants and the ICS bridge`() {
        shouldThrow<IllegalArgumentException> { RecurrenceRule(Frequency.DAILY, interval = 0) }
        shouldThrow<IllegalArgumentException> { RecurrenceRule(Frequency.DAILY, count = 0) }
        shouldThrow<IllegalArgumentException> { RecurrenceRule(Frequency.DAILY, count = 1, until = Jdn(0)) }
        shouldThrow<IllegalArgumentException> { RecurrenceRule(Frequency.DAILY, byMonthDay = listOf(0)) }

        IcsDateTime.Date(LocalDate(2026, 3, 21)).calendarDay() shouldBe LocalDate(2026, 3, 21)
        IcsDateTime.Floating(LocalDateTime(2026, 3, 21, 23, 0)).calendarDay() shouldBe LocalDate(2026, 3, 21)
        IcsDateTime.Utc(Instant.parse("2026-03-20T23:30:00Z")).calendarDay() shouldBe LocalDate(2026, 3, 20)
        IcsDateTime.Zoned(LocalDateTime(2026, 3, 21, 1, 0), "Asia/Tehran").calendarDay() shouldBe LocalDate(2026, 3, 21)
        rule("FREQ=WEEKLY;UNTIL=20260315T235959Z").until shouldBe gregorianJdn("2026-03-15")
        rule("FREQ=WEEKLY").until shouldBe null
    }

    private fun gregorianJdn(iso: String): Jdn = GregorianCalendarSystem.toJdn(g(iso))

    private companion object {
        val RFC_EXAMPLES: List<Triple<String, String, List<String>>> =
            listOf(
                Triple("1997-09-02", "FREQ=DAILY;COUNT=10", (2..11).map { "1997-09-%02d".format(it) }),
                Triple(
                    "1997-09-05",
                    "FREQ=MONTHLY;COUNT=10;BYDAY=1FR",
                    listOf(
                        "1997-09-05",
                        "1997-10-03",
                        "1997-11-07",
                        "1997-12-05",
                        "1998-01-02",
                        "1998-02-06",
                        "1998-03-06",
                        "1998-04-03",
                        "1998-05-01",
                        "1998-06-05",
                    ),
                ),
                Triple(
                    "1997-09-22",
                    "FREQ=MONTHLY;COUNT=6;BYDAY=-2MO",
                    listOf("1997-09-22", "1997-10-20", "1997-11-17", "1997-12-22", "1998-01-19", "1998-02-16"),
                ),
                Triple(
                    "1997-09-02",
                    "FREQ=MONTHLY;COUNT=10;BYMONTHDAY=2,15",
                    listOf(
                        "1997-09-02",
                        "1997-09-15",
                        "1997-10-02",
                        "1997-10-15",
                        "1997-11-02",
                        "1997-11-15",
                        "1997-12-02",
                        "1997-12-15",
                        "1998-01-02",
                        "1998-01-15",
                    ),
                ),
                Triple(
                    "1997-09-30",
                    "FREQ=MONTHLY;COUNT=10;BYMONTHDAY=1,-1",
                    listOf(
                        "1997-09-30",
                        "1997-10-01",
                        "1997-10-31",
                        "1997-11-01",
                        "1997-11-30",
                        "1997-12-01",
                        "1997-12-31",
                        "1998-01-01",
                        "1998-01-31",
                        "1998-02-01",
                    ),
                ),
            )
    }
}
