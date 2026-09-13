/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.ics.AlarmTrigger
import ir.taqvim.core.ics.DisplayAlarm
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1003 (U): iCalendar events → personal events, in the Asia/Tehran device zone. */
class IcsEventMappingTest {
    private val mapping = IcsEventMapping(TimeZone.of("Asia/Tehran"))

    private fun import(event: IcsEvent) = mapping.toImported(event, nowEpochMillis = 1_000)

    private fun day(
        year: Int,
        month: Int,
        day: Int,
    ): Long = LocalDate(year, month, day).toJdn().value

    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = IcsDateTime.Date(LocalDate(year, month, day))

    private fun issues(event: IcsEvent) = import(event).warnings.map { it.issue }

    @Test
    fun `all-day events end the day before their exclusive DTEND`() {
        val imported =
            import(IcsEvent("a", date(2026, 3, 21), date(2026, 3, 24), summary = "Trip", description = "Notes"))
        val event = imported.event

        listOf(event.startJdn, event.endJdn) shouldBe listOf(day(2026, 3, 21), day(2026, 3, 23))
        listOf(event.startMinute, event.endMinute) shouldBe listOf(null, null)
        listOf(event.title, event.notes, event.timeZoneId, event.icsUid) shouldBe
            listOf("Trip", "Notes", "Asia/Tehran", "a")
        listOf(event.createdAtEpochMillis, event.updatedAtEpochMillis) shouldBe listOf(1_000L, 1_000L)
        event.calendarSystem shouldBe CalendarSystem.GREGORIAN
        imported.recurrence.shouldBeNull()
        imported.warnings.shouldBeEmpty()

        import(IcsEvent("b", date(2026, 3, 21))).event.endJdn shouldBe day(2026, 3, 21)
        import(IcsEvent("c", date(2026, 3, 21), date(2026, 3, 21))).event.endJdn shouldBe day(2026, 3, 21)
        issues(IcsEvent("d", date(2026, 3, 21), date(2026, 3, 20))) shouldBe listOf(ImportIssue.INVALID_END)
    }

    @Test
    fun `timed events keep their zone, UTC and floating times go to the device zone`() {
        val berlin =
            import(
                IcsEvent(
                    "berlin",
                    IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 18, 0), "Europe/Berlin"),
                    IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 19, 30), "Europe/Berlin"),
                ),
            ).event
        listOf(berlin.startMinute, berlin.endMinute, berlin.timeZoneId) shouldBe listOf(1_080, 1_170, "Europe/Berlin")

        val utc =
            import(
                IcsEvent(
                    "utc",
                    IcsDateTime.Utc(Instant.parse("2026-07-10T06:00:00Z")),
                    IcsDateTime.Utc(Instant.parse("2026-07-10T21:00:00Z")),
                ),
            ).event
        listOf(utc.startJdn, utc.startMinute, utc.endJdn, utc.endMinute) shouldBe
            listOf(day(2026, 7, 10), 570, day(2026, 7, 11), 30)

        val floating = import(IcsEvent("f", IcsDateTime.Floating(LocalDateTime(2026, 1, 1, 8, 0)))).event
        listOf(floating.startMinute, floating.endMinute, floating.timeZoneId) shouldBe listOf(480, 480, "Asia/Tehran")
    }

    @Test
    fun `an end before the start or of another type is replaced by the start`() {
        val start = IcsDateTime.Floating(LocalDateTime(2026, 1, 1, 8, 0))

        issues(IcsEvent("a", start, date(2026, 1, 2))) shouldBe listOf(ImportIssue.INVALID_END)
        issues(IcsEvent("b", start, IcsDateTime.Floating(LocalDateTime(2026, 1, 1, 7, 0)))) shouldBe
            listOf(ImportIssue.INVALID_END)
        import(IcsEvent("c", date(2026, 1, 1), start)).event.endJdn shouldBe day(2026, 1, 1)
    }

    @Test
    fun `a date-time UNTIL of a timed event becomes its day in the event zone`() {
        val imported =
            import(
                IcsEvent(
                    "weekly",
                    IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 1, 0), "Asia/Tehran"),
                    recurrence =
                        Recurrence(Frequency.WEEKLY, until = IcsDateTime.Utc(Instant.parse("2026-07-23T21:30:00Z"))),
                ),
            )

        imported.recurrence shouldBe RecurrenceRule(Frequency.WEEKLY, until = LocalDate(2026, 7, 24).toJdn())
        import(
            IcsEvent("yearly", date(2026, 3, 21), recurrence = Recurrence(Frequency.YEARLY, until = date(2030, 3, 21))),
        ).recurrence shouldBe RecurrenceRule(Frequency.YEARLY, until = LocalDate(2030, 3, 21).toJdn())
    }

    @Test
    fun `a Taqvim rule wins over RRULE and RDATE, and unusable parts are reported`() {
        val persian = "CALENDAR=PERSIAN;FREQ=YEARLY;INTERVAL=1;INVALID=NEXT_DAY;WKST=SA"
        val withRule =
            IcsEvent(
                "p",
                date(2025, 3, 20),
                recurrence = Recurrence(Frequency.DAILY),
                recurrenceDates = listOf(date(2026, 3, 21)),
                extensions = mapOf(TaqvimRecurrence.PROPERTY to persian),
            )
        val imported = import(withRule)

        imported.event.calendarSystem shouldBe CalendarSystem.PERSIAN
        imported.recurrence shouldBe TaqvimRecurrence.parse(persian)?.rule
        imported.warnings.shouldBeEmpty()

        val broken = import(withRule.copy(extensions = mapOf(TaqvimRecurrence.PROPERTY to "FREQ=NEVER")))
        broken.recurrence shouldBe RecurrenceRule(Frequency.DAILY)
        broken.warnings.map { it.issue } shouldBe
            listOf(ImportIssue.INVALID_TAQVIM_RECURRENCE, ImportIssue.RECURRENCE_DATES_IGNORED)
        issues(IcsEvent("x", date(2026, 1, 1), exceptionDates = listOf(date(2026, 1, 2)))) shouldBe
            listOf(ImportIssue.EXCEPTION_DATES_IGNORED)
    }

    @Test
    fun `display alarms at or before the start become reminders`() {
        val alarms =
            listOf(
                AlarmTrigger.Relative((-15).minutes),
                AlarmTrigger.Relative((-15).minutes),
                AlarmTrigger.Relative(0.minutes),
                AlarmTrigger.Relative(5.minutes),
                AlarmTrigger.Relative((-5).minutes, relatedToEnd = true),
                AlarmTrigger.Absolute(Instant.parse("2026-01-01T00:00:00Z")),
            ).map { DisplayAlarm(it, "") }
        val imported = import(IcsEvent("r", date(2026, 1, 1), alarms = alarms))

        imported.reminderMinutes shouldBe listOf(15, 0)
        imported.warnings shouldBe listOf(ImportWarning("r", ImportIssue.UNSUPPORTED_ALARM))
    }
}
