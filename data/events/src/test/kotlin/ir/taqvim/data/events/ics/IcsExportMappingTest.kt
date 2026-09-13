/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.AlarmTrigger
import ir.taqvim.core.ics.DisplayAlarm
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.IcsCalendar
import ir.taqvim.core.ics.IcsDateTime
import ir.taqvim.core.ics.IcsParseResult
import ir.taqvim.core.ics.IcsReader
import ir.taqvim.core.ics.IcsWriter
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.Recurrence
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1003 (U): personal events → iCalendar per ADR-0013, and the export → import round trip. */
class IcsExportMappingTest {
    private val mapping = IcsExportMapping(CalendarProvider.DEFAULT)
    private val tehran = TimeZone.of("Asia/Tehran")
    private val nowruz1405 = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))

    private fun jdn(
        year: Int,
        month: Int,
        day: Int,
    ): Long = LocalDate(year, month, day).toJdn().value

    private fun event(
        startJdn: Long,
        endJdn: Long = startJdn,
        startMinute: Int? = null,
        endMinute: Int? = null,
        calendar: CalendarSystem = CalendarSystem.GREGORIAN,
        timeZoneId: String = "Asia/Tehran",
    ) = PersonalEventEntity(
        id = 7,
        title = "Title",
        calendarSystem = calendar,
        startJdn = startJdn,
        startMinute = startMinute,
        endJdn = endJdn,
        endMinute = endMinute,
        timeZoneId = timeZoneId,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
    )

    private fun reminder(
        minutes: Int,
        enabled: Boolean = true,
    ) = ReminderEntity(eventId = 7, minutesBefore = minutes, enabled = enabled)

    @Test
    fun `one-off events become DATE or zoned DATE-TIME values with display alarms`() {
        val allDay =
            mapping.toIcs(
                ExportRecord(
                    event(jdn(2026, 3, 21), jdn(2026, 3, 23)),
                    null,
                    listOf(reminder(10), reminder(30, false)),
                ),
                Jdn(jdn(2026, 1, 1)),
            )

        allDay.uid shouldBe "taqvim-7-1000"
        allDay.start shouldBe IcsDateTime.Date(LocalDate(2026, 3, 21))
        allDay.end shouldBe IcsDateTime.Date(LocalDate(2026, 3, 24))
        allDay.summary shouldBe "Title"
        allDay.description.shouldBeNull()
        allDay.alarms shouldBe listOf(DisplayAlarm(AlarmTrigger.Relative((-10).minutes), "Title"))
        allDay.recurrence.shouldBeNull()

        val timed = event(jdn(2026, 7, 10), startMinute = 570, endMinute = 630)
        mapping.toIcs(ExportRecord(timed.copy(icsUid = "kept"), null, emptyList()), Jdn(0)).let {
            it.uid shouldBe "kept"
            it.start shouldBe IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 9, 30), "Asia/Tehran")
            it.end shouldBe IcsDateTime.Zoned(LocalDateTime(2026, 7, 10, 10, 30), "Asia/Tehran")
        }
        mapping.toIcs(ExportRecord(timed.copy(timeZoneId = "Mars/Olympus"), null, emptyList()), Jdn(0)).start shouldBe
            IcsDateTime.Floating(LocalDateTime(2026, 7, 10, 9, 30))
    }

    @Test
    fun `Gregorian rules that RFC 5545 expands identically are written as RRULE`() {
        val rule =
            RecurrenceRule(Frequency.WEEKLY, byDay = listOf(WeekdayNum(Weekday.MONDAY)), until = Jdn(jdn(2026, 12, 31)))
        val timed = mapping.toIcs(ExportRecord(event(jdn(2026, 7, 6), startMinute = 570), rule, emptyList()), Jdn(0))

        timed.recurrence shouldBe
            Recurrence(
                Frequency.WEEKLY,
                until = IcsDateTime.Utc(Instant.parse("2026-12-31T06:00:00Z")),
                byDay = listOf(WeekdayNum(Weekday.MONDAY)),
            )
        timed.recurrenceDates.shouldBeEmpty()
        timed.extensions shouldBe emptyMap()
        mapping.toIcs(ExportRecord(event(jdn(2026, 7, 6)), rule, emptyList()), Jdn(0)).recurrence?.until shouldBe
            IcsDateTime.Date(LocalDate(2026, 12, 31))
    }

    @Test
    fun `a Persian rule is written as its occurrences plus the Taqvim rule`() {
        val start = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1403, 12, 30))
        val rule = RecurrenceRule(Frequency.YEARLY, count = 5, invalidDates = InvalidDatePolicy.NEXT_DAY)
        val record = ExportRecord(event(start.value, calendar = CalendarSystem.PERSIAN), rule, emptyList())
        val exported = mapping.toIcs(record, Jdn(0))

        exported.recurrence.shouldBeNull()
        exported.extensions shouldBe
            mapOf(
                TaqvimRecurrence.PROPERTY to TaqvimRecurrence.format(CalendarRecurrence(CalendarSystem.PERSIAN, rule)),
            )
        val expected =
            RecurrenceEngine(PersianCalendarSystem)
                .occurrences(PersianCalendarSystem.fromJdn(start), rule)
                .drop(1)
                .toList()
        exported.recurrenceDates shouldBe expected.map { IcsDateTime.Date(it.toLocalDate()) }
        exported.recurrenceDates shouldHaveSize 4
        expected.map { PersianCalendarSystem.fromJdn(it) }.forEach { date ->
            val lastDay = date.month == 12 && date.day == 30 && PersianCalendarSystem.isLeapYear(date.year)
            (lastDay || (date.month == 1 && date.day == 1)) shouldBe true
        }
    }

    @Test
    fun `open-ended explicit rules stop at the horizon and the date limit`() {
        val rule = RecurrenceRule(Frequency.MONTHLY)
        val record = ExportRecord(event(nowruz1405.value, calendar = CalendarSystem.ISLAMIC), rule, emptyList())
        val horizon =
            IcsExportMapping(
                CalendarProvider.DEFAULT,
                ExportLimits(horizonDays = 400),
            ).toIcs(record, nowruz1405)

        horizon.recurrenceDates.size shouldBeGreaterThanOrEqual 12
        horizon.recurrenceDates.forEach {
            it.shouldBeInstanceOf<IcsDateTime.Date>()
            it.date.toJdn().value shouldBeLessThanOrEqual nowruz1405.value + 400
        }
        IcsExportMapping(CalendarProvider.DEFAULT, ExportLimits(maxRecurrenceDates = 5))
            .toIcs(record, nowruz1405)
            .recurrenceDates shouldHaveSize 5

        val gregorianNextDay =
            RecurrenceRule(Frequency.MONTHLY, byMonthDay = listOf(31), invalidDates = InvalidDatePolicy.NEXT_DAY)
        mapping
            .toIcs(
                ExportRecord(event(jdn(2026, 1, 31)), gregorianNextDay.copy(count = 3), emptyList()),
                Jdn(0),
            ).let {
                it.recurrence.shouldBeNull()
                it.recurrenceDates shouldBe
                    listOf(IcsDateTime.Date(LocalDate(2026, 3, 1)), IcsDateTime.Date(LocalDate(2026, 3, 31)))
            }
        val nepali = ExportRecord(event(nowruz1405.value, calendar = CalendarSystem.NEPALI), rule, emptyList())
        mapping.toIcs(nepali, nowruz1405).let {
            it.recurrenceDates.shouldBeEmpty()
            it.extensions.keys shouldBe setOf(TaqvimRecurrence.PROPERTY)
        }
    }

    @Test
    fun `exported events import back to the same personal events`() {
        val persianStart = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1403, 12, 30)).value
        val records =
            listOf(
                ExportRecord(event(jdn(2026, 3, 21), jdn(2026, 3, 23)).copy(icsUid = "a"), null, listOf(reminder(10))),
                ExportRecord(
                    event(jdn(2026, 7, 6), startMinute = 570, endMinute = 600).copy(icsUid = "b"),
                    RecurrenceRule(Frequency.WEEKLY, until = Jdn(jdn(2026, 12, 28))),
                    emptyList(),
                ),
                ExportRecord(
                    event(persianStart, calendar = CalendarSystem.PERSIAN).copy(icsUid = "c"),
                    RecurrenceRule(
                        Frequency.YEARLY,
                        invalidDates = InvalidDatePolicy.NEXT_DAY,
                        weekStart = Weekday.SATURDAY,
                    ),
                    emptyList(),
                ),
            )
        val text =
            IcsWriter.write(
                IcsCalendar(
                    "-//Test//EN",
                    records.map {
                        mapping.toIcs(it, Jdn(persianStart))
                    },
                ),
                Instant.fromEpochMilliseconds(0),
            )
        val parsed = IcsReader.read(text)
        parsed.shouldBeInstanceOf<IcsParseResult.Success>()
        val imported = parsed.calendar.events.map { IcsEventMapping(tehran).toImported(it, nowEpochMillis = 1_000) }

        imported.map { it.event.copy(id = 7, title = "Title") } shouldBe records.map { it.event }
        imported.map { it.recurrence } shouldBe records.map { it.recurrence }
        imported.map { it.reminderMinutes } shouldBe records.map { record -> record.reminders.map { it.minutesBefore } }
        imported.flatMap { it.warnings }.shouldBeEmpty()
    }
}
