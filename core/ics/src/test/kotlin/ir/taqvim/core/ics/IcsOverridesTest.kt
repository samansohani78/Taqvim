/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

/** T-1003: overridden instances (RECURRENCE-ID, RFC 5545 §3.8.4.4) and cancelled components (STATUS, §3.8.1.11). */
class IcsOverridesTest {
    private val text =
        listOf(
            "BEGIN:VCALENDAR",
            "PRODID:-//Test//EN",
            "BEGIN:VEVENT",
            "UID:series@taqvim.test",
            "DTSTART;TZID=Asia/Tehran:20260321T090000",
            "RRULE:FREQ=DAILY;COUNT=5",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:series@taqvim.test",
            "RECURRENCE-ID;TZID=Asia/Tehran:20260322T090000",
            "DTSTART;TZID=Asia/Tehran:20260322T110000",
            "SUMMARY:Moved",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:series@taqvim.test",
            "RECURRENCE-ID;TZID=Asia/Tehran:20260323T090000",
            "DTSTART;TZID=Asia/Tehran:20260323T090000",
            "status:cancelled",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n", postfix = "\r\n")

    @Test
    fun `overrides are read with their recurrence id and cancellation`() {
        val result = IcsReader.read(text)

        result.shouldBeInstanceOf<IcsParseResult.Success>()
        val (series, moved, cancelled) = result.calendar.events
        series.recurrenceId shouldBe null
        series.cancelled shouldBe false
        moved.recurrenceId shouldBe IcsDateTime.Zoned(LocalDateTime(2026, 3, 22, 9, 0), "Asia/Tehran")
        moved.summary shouldBe "Moved"
        cancelled.cancelled shouldBe true
        result.warnings shouldBe emptyList()
    }

    @Test
    fun `overrides survive writing and reading`() {
        val override =
            IcsEvent(
                uid = "a@taqvim.test",
                start = IcsDateTime.Zoned(LocalDateTime(2026, 3, 22, 11, 0), "Asia/Tehran"),
                recurrenceId = IcsDateTime.Zoned(LocalDateTime(2026, 3, 22, 9, 0), "Asia/Tehran"),
                cancelled = true,
            )
        val stamp = Instant.parse("2026-01-01T00:00:00Z")
        val written = IcsWriter.write(IcsCalendar("-//Test//EN", listOf(override)), stamp)

        val read = IcsReader.read(written)
        read.shouldBeInstanceOf<IcsParseResult.Success>()
        read.calendar.events shouldBe listOf(override)
        written shouldContain "RECURRENCE-ID;TZID=Asia/Tehran:20260322T090000\r\nSTATUS:CANCELLED"
    }
}
