/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.ics.AlarmTrigger
import ir.taqvim.core.ics.DisplayAlarm
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    fun `timed events keep their zone, UTC stays UTC and floating times go to the device zone`() {
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
        listOf(utc.startJdn, utc.startMinute, utc.endJdn, utc.endMinute, utc.timeZoneId) shouldBe
            listOf(day(2026, 7, 10), 360, day(2026, 7, 10), 1_260, "UTC")

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
    fun `a date-time UNTIL keeps its time of day, so the last day drops when the cutoff is earlier`() {
        val tehranSeries =
            import(
                IcsEvent(
                    "weekly",
                    IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 1, 0), "Asia/Tehran"),
                    recurrence =
                        Recurrence(Frequency.WEEKLY, until = IcsDateTime.Utc(Instant.parse("2026-07-23T21:30:00Z"))),
                ),
            )

        // 21:30 UTC is 01:00 in Tehran the next day: exactly the start time, and UNTIL includes it.
        tehranSeries.recurrence shouldBe RecurrenceRule(Frequency.WEEKLY, until = LocalDate(2026, 7, 24).toJdn())
        untilOf("2026-09-15T09:00:00Z") shouldBe LocalDate(2026, 9, 14).toJdn()
        untilOf("2026-09-15T10:00:00Z") shouldBe LocalDate(2026, 9, 15).toJdn()
        untilOf("2026-09-15T11:00:00Z") shouldBe LocalDate(2026, 9, 15).toJdn()
        import(
            IcsEvent("yearly", date(2026, 3, 21), recurrence = Recurrence(Frequency.YEARLY, until = date(2030, 3, 21))),
        ).recurrence shouldBe RecurrenceRule(Frequency.YEARLY, until = LocalDate(2030, 3, 21).toJdn())
    }

    /** The rule's last day for a daily 10:00 UTC series beginning 14 September 2026 and ending at [until]. */
    private fun untilOf(until: String) =
        import(
            IcsEvent(
                "daily",
                IcsDateTime.Utc(Instant.parse("2026-09-14T10:00:00Z")),
                recurrence = Recurrence(Frequency.DAILY, until = IcsDateTime.Utc(Instant.parse(until))),
            ),
        ).recurrence
            ?.until

    @Test
    fun `a UTC series keeps its UTC instants across a daylight-saving change`() {
        val berlinDevice = IcsEventMapping(TimeZone.of("Europe/Berlin"))
        val utcSeries =
            berlinDevice
                .toImported(
                    IcsEvent(
                        "utc-weekly",
                        IcsDateTime.Utc(Instant.parse("2026-03-23T09:00:00Z")),
                        recurrence = Recurrence(Frequency.WEEKLY),
                    ),
                    nowEpochMillis = 1_000,
                ).event

        listOf(utcSeries.startJdn, utcSeries.startMinute, utcSeries.timeZoneId) shouldBe
            listOf(day(2026, 3, 23), 540, "UTC")
        // Berlin changes to summer time on 29 March 2026; the next occurrence still starts at 09:00 UTC.
        instantOf(utcSeries.startJdn + 7, utcSeries.startMinute, utcSeries.timeZoneId) shouldBe
            Instant.parse("2026-03-30T09:00:00Z")

        val berlinSeries =
            berlinDevice
                .toImported(
                    IcsEvent(
                        "zoned-weekly",
                        IcsDateTime.Zoned(LocalDateTime(2026, 3, 23, 10, 0), "Europe/Berlin"),
                        recurrence = Recurrence(Frequency.WEEKLY),
                    ),
                    nowEpochMillis = 1_000,
                ).event

        listOf(berlinSeries.startMinute, berlinSeries.timeZoneId) shouldBe listOf(600, "Europe/Berlin")
        instantOf(berlinSeries.startJdn + 7, berlinSeries.startMinute, berlinSeries.timeZoneId) shouldBe
            Instant.parse("2026-03-30T08:00:00Z")
    }

    private fun instantOf(
        dayJdn: Long,
        minute: Int?,
        timeZoneId: String,
    ): Instant {
        val date = Jdn(dayJdn).toLocalDate()
        val minutes = requireNotNull(minute)
        return LocalDateTime(date, LocalTime(minutes / 60, minutes % 60)).toInstant(TimeZone.of(timeZoneId))
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
    }

    @Test
    fun `exception dates and overrides are kept as days and instances in the series' time zone`() {
        val berlin = "Europe/Berlin"
        val series =
            IcsEvent(
                "s",
                IcsDateTime.Zoned(LocalDateTime(2026, 7, 6, 23, 30), berlin),
                IcsDateTime.Zoned(LocalDateTime(2026, 7, 7, 0, 30), berlin),
                summary = "Series",
                description = "Notes",
                recurrence = Recurrence(Frequency.DAILY),
                exceptionDates =
                    listOf(
                        IcsDateTime.Utc(Instant.parse("2026-07-08T21:30:00Z")),
                        date(2026, 7, 9),
                        IcsDateTime.Zoned(LocalDateTime(2026, 7, 9, 23, 30), berlin),
                    ),
            )
        val moved =
            IcsEvent(
                "s",
                IcsDateTime.Utc(Instant.parse("2026-07-10T08:00:00Z")),
                IcsDateTime.Utc(Instant.parse("2026-07-10T09:15:00Z")),
                recurrenceId = IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 23, 30), berlin),
            )
        val allDay = IcsEvent("s", date(2026, 7, 12), summary = "Day off", recurrenceId = date(2026, 7, 11))
        val cancelledId = IcsDateTime.Utc(Instant.parse("2026-07-12T21:30:00Z"))
        val cancelled = moved.copy(recurrenceId = cancelledId, cancelled = true)

        val imported = mapping.toImported(series, 1_000, listOf(moved, allDay, cancelled, moved))

        imported.warnings.shouldBeEmpty()
        imported.exceptionDays shouldBe listOf(day(2026, 7, 8), day(2026, 7, 9))
        val spans =
            imported.overrides.map { listOf(it.originalJdn, it.startJdn, it.startMinute, it.endJdn, it.endMinute) }
        spans shouldBe
            listOf(
                listOf(day(2026, 7, 10), day(2026, 7, 10), 600, day(2026, 7, 10), 675),
                listOf(day(2026, 7, 11), day(2026, 7, 12), null, day(2026, 7, 12), null),
                listOf(day(2026, 7, 12), day(2026, 7, 10), 600, day(2026, 7, 10), 675),
            )
        imported.overrides.map { Triple(it.title, it.notes, it.cancelled) } shouldBe
            listOf(Triple("Series", "Notes", false), Triple("Day off", "Notes", false), Triple("Series", "Notes", true))
        imported.overrides.map { it.eventId }.toSet() shouldBe setOf(0L)
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
