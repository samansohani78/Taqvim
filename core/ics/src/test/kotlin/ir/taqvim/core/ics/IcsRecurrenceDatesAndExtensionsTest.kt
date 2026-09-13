/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

/** T-1003 additions to T-502: RDATE lists (RFC 5545 §3.8.5.2) and X- properties (§3.8.8.2), read and written. */
class IcsRecurrenceDatesAndExtensionsTest {
    private fun calendar(vararg eventLines: String): String =
        (
            listOf("BEGIN:VCALENDAR", "PRODID:-//Test//EN", "BEGIN:VEVENT", "UID:a@taqvim.test") +
                listOf("DTSTART;VALUE=DATE:20260701") + eventLines + listOf("END:VEVENT", "END:VCALENDAR")
        ).joinToString("\r\n", postfix = "\r\n")

    private fun success(text: String): IcsParseResult.Success {
        val result = IcsReader.read(text)
        result.shouldBeInstanceOf<IcsParseResult.Success>()
        return result
    }

    @Test
    fun `RDATE lists are read as dates or date-times and PERIOD values are skipped`() {
        val result =
            success(
                calendar(
                    "RDATE;VALUE=DATE:20260710,20260720",
                    "RDATE:20260801T090000Z",
                    "RDATE;VALUE=PERIOD:20260901T090000Z/PT1H",
                ),
            )

        result.calendar.events
            .single()
            .recurrenceDates shouldBe
            listOf(
                IcsDateTime.Date(LocalDate(2026, 7, 10)),
                IcsDateTime.Date(LocalDate(2026, 7, 20)),
                IcsDateTime.Utc(Instant.parse("2026-08-01T09:00:00Z")),
            )
        result.warnings.map { it.message } shouldBe listOf("RDATE PERIOD values are not supported")
    }

    @Test
    fun `X- properties are kept by upper-case name with unescaped text and the first of a name wins`() {
        val event =
            success(calendar("X-TAQVIM-RECURRENCE:CALENDAR=PERSIAN\\;FREQ=YEARLY", "x-custom:one", "X-CUSTOM:two"))
                .calendar.events
                .single()

        event.extensions shouldBe mapOf("X-TAQVIM-RECURRENCE" to "CALENDAR=PERSIAN;FREQ=YEARLY", "X-CUSTOM" to "one")
    }

    @Test
    fun `recurrence dates and extensions survive writing and reading`() {
        val event =
            IcsEvent(
                uid = "b@taqvim.test",
                start = IcsDateTime.Zoned(LocalDateTime(2026, 3, 20, 9, 0), "Asia/Tehran"),
                alarms = listOf(DisplayAlarm(AlarmTrigger.Relative((-10).minutes), "reminder")),
                recurrenceDates = listOf(IcsDateTime.Zoned(LocalDateTime(2027, 3, 21, 9, 0), "Asia/Tehran")),
                extensions = mapOf("X-TAQVIM-RECURRENCE" to "CALENDAR=PERSIAN;FREQ=YEARLY;INVALID=NEXT_DAY"),
            )
        val text = IcsWriter.write(IcsCalendar("-//Test//EN", listOf(event)), Instant.parse("2026-01-01T00:00:00Z"))

        success(text).calendar.events shouldBe listOf(event)
        text shouldContain "RDATE;TZID=Asia/Tehran:20270321T090000"
        text shouldContain "X-TAQVIM-RECURRENCE:CALENDAR=PERSIAN\\;FREQ=YEARLY\\;INVALID=NEXT_DAY"
        text.indexOf("X-TAQVIM-RECURRENCE") shouldBeLessThan text.indexOf("BEGIN:VALARM")
    }

    @Test
    fun `extension names must be X-names`() {
        shouldThrow<IllegalArgumentException> {
            IcsEvent("c", IcsDateTime.Date(LocalDate(2026, 1, 1)), extensions = mapOf("SUMMARY" to "x"))
        }
    }
}
