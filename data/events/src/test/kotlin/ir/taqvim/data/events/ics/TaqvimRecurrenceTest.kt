/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import org.junit.jupiter.api.Test

/** ADR-0013: the `X-TAQVIM-RECURRENCE` value format. */
class TaqvimRecurrenceTest {
    @Test
    fun `every part is written and read back`() {
        val recurrence =
            CalendarRecurrence(
                CalendarSystem.PERSIAN,
                RecurrenceRule(
                    frequency = Frequency.MONTHLY,
                    interval = 2,
                    until = Jdn(2_461_500),
                    byDay = listOf(WeekdayNum(Weekday.SATURDAY, 1), WeekdayNum(Weekday.FRIDAY, -1)),
                    byMonthDay = listOf(30),
                    invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH,
                    weekStart = Weekday.SATURDAY,
                ),
            )
        val text = TaqvimRecurrence.format(recurrence)

        text shouldBe
            "CALENDAR=PERSIAN;FREQ=MONTHLY;INTERVAL=2;UNTIL=2461500;BYDAY=1SA,-1FR;BYMONTHDAY=30;" +
            "INVALID=LAST_DAY_OF_MONTH;WKST=SA"
        TaqvimRecurrence.parse(text) shouldBe recurrence
    }

    @Test
    fun `optional parts take the RFC 5545 defaults`() {
        TaqvimRecurrence.parse("CALENDAR=ISLAMIC;FREQ=YEARLY") shouldBe
            CalendarRecurrence(CalendarSystem.ISLAMIC, RecurrenceRule(Frequency.YEARLY))
        TaqvimRecurrence.format(
            CalendarRecurrence(CalendarSystem.GREGORIAN, RecurrenceRule(Frequency.DAILY, count = 3)),
        ) shouldBe
            "CALENDAR=GREGORIAN;FREQ=DAILY;INTERVAL=1;COUNT=3;INVALID=SKIP;WKST=MO"
    }

    @Test
    fun `missing, unknown or contradictory parts make the value unreadable`() {
        listOf(
            "FREQ=YEARLY",
            "CALENDAR=PERSIAN",
            "CALENDAR=MARTIAN;FREQ=YEARLY",
            "CALENDAR=PERSIAN;FREQ=HOURLY",
            "CALENDAR=PERSIAN;FREQ=WEEKLY;BYDAY=XX",
            "CALENDAR=PERSIAN;FREQ=WEEKLY;WKST=ZZ",
            "CALENDAR=PERSIAN;FREQ=DAILY;COUNT=2;UNTIL=2461500",
            "CALENDAR=PERSIAN;FREQ=DAILY;INTERVAL=0",
        ).forEach { TaqvimRecurrence.parse(it).shouldBeNull() }
    }
}
