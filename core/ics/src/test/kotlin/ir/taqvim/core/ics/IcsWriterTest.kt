/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

class IcsWriterTest {
    private val stamp = Instant.parse("2026-09-13T12:00:00Z")

    @Test
    fun `writes CRLF lines with DTSTAMP, escaping, recurrence and alarms`() {
        IcsWriter.write(SAMPLE, stamp) shouldBe EXPECTED_LINES.joinToString(separator = "\r\n", postfix = "\r\n")
    }

    @Test
    fun `long values are folded at 75 octets and read back`() {
        val summary = "جشن نوروز ".repeat(20) + "🌸 end"
        val event = IcsEvent("long@taqvim.test", IcsDateTime.Date(LocalDate(2026, 3, 21)), summary = summary)
        val calendar = IcsCalendar("p", listOf(event))
        val written = IcsWriter.write(calendar, stamp)

        written.split("\r\n").forEach { it.toByteArray(Charsets.UTF_8).size shouldBeLessThanOrEqual 75 }
        IcsReader.read(written) shouldBe IcsParseResult.Success(calendar, emptyList())
    }

    private companion object {
        val SAMPLE =
            IcsCalendar(
                productId = "-//Taqvim//Test//EN",
                events =
                    listOf(
                        IcsEvent(
                            uid = "w1@taqvim.test",
                            start = IcsDateTime.Date(LocalDate(2026, 3, 21)),
                            summary = "Nowruz, day 1",
                            recurrence = Recurrence(Frequency.YEARLY, byMonthDay = listOf(21)),
                            alarms = listOf(DisplayAlarm(AlarmTrigger.Relative(-1.days), "Tomorrow")),
                        ),
                        IcsEvent(
                            uid = "w2@taqvim.test",
                            start = IcsDateTime.Zoned(LocalDateTime(2026, 3, 22, 9, 30), "Asia/Tehran"),
                            end = IcsDateTime.Floating(LocalDateTime(2026, 3, 22, 10, 0)),
                            exceptionDates = listOf(IcsDateTime.Utc(Instant.parse("2026-03-29T06:00:00Z"))),
                            alarms =
                                listOf(
                                    DisplayAlarm(AlarmTrigger.Relative(10.minutes, relatedToEnd = true), "Done"),
                                    DisplayAlarm(AlarmTrigger.Absolute(Instant.parse("2026-03-22T05:00:00Z")), "Early"),
                                ),
                        ),
                    ),
            )

        val EXPECTED_LINES =
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Taqvim//Test//EN",
                "BEGIN:VEVENT",
                "UID:w1@taqvim.test",
                "DTSTAMP:20260913T120000Z",
                "DTSTART;VALUE=DATE:20260321",
                "SUMMARY:Nowruz\\, day 1",
                "RRULE:FREQ=YEARLY;BYMONTHDAY=21",
                "BEGIN:VALARM",
                "ACTION:DISPLAY",
                "DESCRIPTION:Tomorrow",
                "TRIGGER:-P1D",
                "END:VALARM",
                "END:VEVENT",
                "BEGIN:VEVENT",
                "UID:w2@taqvim.test",
                "DTSTAMP:20260913T120000Z",
                "DTSTART;TZID=Asia/Tehran:20260322T093000",
                "DTEND:20260322T100000",
                "EXDATE:20260329T060000Z",
                "BEGIN:VALARM",
                "ACTION:DISPLAY",
                "DESCRIPTION:Done",
                "TRIGGER;RELATED=END:PT10M",
                "END:VALARM",
                "BEGIN:VALARM",
                "ACTION:DISPLAY",
                "DESCRIPTION:Early",
                "TRIGGER;VALUE=DATE-TIME:20260322T050000Z",
                "END:VALARM",
                "END:VEVENT",
                "END:VCALENDAR",
            )
    }
}
