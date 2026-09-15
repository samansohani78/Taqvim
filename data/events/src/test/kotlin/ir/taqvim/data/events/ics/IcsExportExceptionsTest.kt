/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.collections.shouldBeEmpty
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
import ir.taqvim.core.ics.IcsEvent
import ir.taqvim.core.ics.IcsParseResult
import ir.taqvim.core.ics.IcsReader
import ir.taqvim.core.ics.IcsWriter
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1003 (U): exception days as EXDATE, overrides as RECURRENCE-ID components, and their import round trip. */
class IcsExportExceptionsTest {
    private val mapping = IcsExportMapping(CalendarProvider.DEFAULT)
    private val tehran = "Asia/Tehran"

    private fun event(
        startJdn: Long,
        startMinute: Int? = null,
        calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    ) = PersonalEventEntity(
        id = 7,
        title = "Class",
        notes = "Room 4",
        calendarSystem = calendar,
        startJdn = startJdn,
        startMinute = startMinute,
        endJdn = startJdn,
        endMinute = startMinute?.plus(60),
        timeZoneId = tehran,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
        icsUid = "series@taqvim.test",
    )

    private fun override(
        original: Long,
        start: Long,
        minute: Int?,
        cancelled: Boolean = false,
    ) = EventOverrideEntity(7, original, "Moved", "Room 9", start, minute, start, minute?.plus(30), null, cancelled)

    /** [events] written and read back, then imported as one series with its overrides. */
    private fun reimport(events: List<IcsEvent>): ImportedEvent {
        val text = IcsWriter.write(IcsCalendar("-//Test//EN", events), Instant.fromEpochMilliseconds(0))
        val parsed = IcsReader.read(text)
        parsed.shouldBeInstanceOf<IcsParseResult.Success>()
        val (overrides, series) = parsed.calendar.events.partition { it.recurrenceId != null }
        return IcsEventMapping(TimeZone.of(tehran)).toImported(series.single(), 1_000, overrides)
    }

    private fun zoned(
        day: Long,
        hour: Int,
        minute: Int,
    ) = IcsDateTime.Zoned(LocalDateTime(Jdn(day).toLocalDate(), LocalTime(hour, minute)), tehran)

    @Test
    fun `an RRULE series writes EXDATE values and one VEVENT per kept override`() {
        val monday = LocalDate(2026, 7, 6).toJdn().value
        val moved = override(monday + 14, monday + 15, 900)
        val record =
            ExportRecord(
                event(monday, startMinute = 570),
                RecurrenceRule(Frequency.WEEKLY, count = 6),
                listOf(ReminderEntity(eventId = 7, minutesBefore = 10)),
                exceptionDays = listOf(monday + 7),
                overrides = listOf(override(monday + 21, monday + 21, 570, cancelled = true), moved),
            )

        val series = mapping.toIcs(record, Jdn(monday))
        val overrides = mapping.overrideEvents(record)

        series.exceptionDates shouldBe listOf(zoned(monday + 7, 9, 30), zoned(monday + 21, 9, 30))
        series.recurrenceDates.shouldBeEmpty()
        overrides shouldBe
            listOf(
                IcsEvent(
                    uid = "series@taqvim.test",
                    start = zoned(monday + 15, 15, 0),
                    end = zoned(monday + 15, 15, 30),
                    summary = "Moved",
                    description = "Room 9",
                    alarms = listOf(DisplayAlarm(AlarmTrigger.Relative((-10).minutes), "Moved")),
                    recurrenceId = zoned(monday + 14, 9, 30),
                ),
            )

        val imported = reimport(listOf(series) + overrides)
        imported.exceptionDays shouldBe listOf(monday + 7, monday + 21)
        imported.overrides shouldBe listOf(moved.copy(eventId = 0))
        imported.recurrence shouldBe record.recurrence
        imported.warnings.shouldBeEmpty()
    }

    @Test
    fun `Persian and Islamic RDATE lists skip exceptions and keep the dates overrides name`() {
        val nowruz1405 = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))
        listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC).forEach { system ->
            val calendar = requireNotNull(CalendarProvider.DEFAULT.calendarFor(system))
            val rule = RecurrenceRule(Frequency.MONTHLY, count = 8, invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH)
            val all = RecurrenceEngine(calendar).occurrences(calendar.fromJdn(nowruz1405), rule).toList()
            val moved = override(all[4].value, all[4].value + 1, null)
            val record =
                ExportRecord(
                    event(nowruz1405.value, calendar = system),
                    rule,
                    emptyList(),
                    exceptionDays = listOf(all[2].value),
                    overrides = listOf(moved, override(all[5].value, all[5].value, null, cancelled = true)),
                )

            val series = mapping.toIcs(record, nowruz1405)
            val overrides = mapping.overrideEvents(record)

            series.recurrenceDates shouldBe (all.drop(1) - all[2] - all[5]).map { IcsDateTime.Date(it.toLocalDate()) }
            series.exceptionDates shouldBe listOf(all[2], all[5]).map { IcsDateTime.Date(it.toLocalDate()) }
            overrides.map { it.recurrenceId to it.start } shouldBe
                listOf(IcsDateTime.Date(all[4].toLocalDate()) to IcsDateTime.Date((all[4] + 1).toLocalDate()))

            val imported = reimport(listOf(series) + overrides)
            imported.recurrence shouldBe rule
            imported.event.calendarSystem shouldBe system
            imported.exceptionDays shouldBe listOf(all[2].value, all[5].value)
            imported.overrides shouldBe listOf(moved.copy(eventId = 0))
        }
    }
}
