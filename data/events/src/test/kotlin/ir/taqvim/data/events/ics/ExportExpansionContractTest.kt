/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.devicecalendar.InstantWindow
import ir.taqvim.data.events.DeviceEventsSource
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.IcsEventsSource
import ir.taqvim.data.events.PersonalEventRecord
import ir.taqvim.data.events.PersonalEventsSource
import ir.taqvim.data.events.PersonalOccurrence
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

/**
 * I08: a personal event exported to iCalendar and expanded again as a subscription starts at the same instants as the
 * calendar shows it (T-305, T-1003), in zones ahead of and behind the display zone, across a moved override and a
 * cancelled instance, for UTC series and for all-day events.
 */
class ExportExpansionContractTest {
    private val day = LocalDate(2026, 9, 15).toJdn()
    private val range: JdnRange = day - 1..day + 9
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T00:00:00Z")
        }
    private val mapping = IcsExportMapping(CalendarProvider.DEFAULT)

    @Test
    fun `a Tokyo meeting after midnight starts at the same instant in the calendar and in its export`(): Unit =
        runTest {
            assertSameStarts(record(zoneId = "Asia/Tokyo", start = 30), TimeZone.UTC)
        }

    @Test
    fun `a weekly Los Angeles evening meeting starts at the same instants for a Tehran reader`(): Unit =
        runTest {
            val weekly = record("America/Los_Angeles", start = 23 * 60, rule = RecurrenceRule(Frequency.WEEKLY))
            assertSameStarts(weekly, TimeZone.of("Asia/Tehran"))
        }

    @Test
    fun `a daily series with an override moved past midnight and a cancelled day agrees`(): Unit =
        runTest {
            val moved = override(original = day + 2, start = day + 3, minute = 30)
            val cancelled = override(original = day + 4, start = day + 4, minute = 22 * 60, cancelled = true)
            val daily =
                record(
                    "Asia/Tehran",
                    start = 22 * 60,
                    rule = RecurrenceRule(Frequency.DAILY),
                    overrides = listOf(moved, cancelled),
                )
            val starts = assertSameStarts(daily, TimeZone.of("Asia/Tehran"))

            starts.contains(instant(day + 3, 30, "Asia/Tehran")) shouldBe true
            starts.contains(instant(day + 2, 22 * 60, "Asia/Tehran")) shouldBe false
            starts.contains(instant(day + 4, 22 * 60, "Asia/Tehran")) shouldBe false
        }

    @Test
    fun `a UTC series keeps its UTC starts through export and expansion`(): Unit =
        runTest {
            val utc = record("UTC", start = 21 * 60 + 30, rule = RecurrenceRule(Frequency.WEEKLY))
            assertSameStarts(utc, TimeZone.of("Asia/Tehran"))
        }

    @Test
    fun `an all-day event keeps its date in the calendar and in its export`(): Unit =
        runTest {
            val allDay = record("Asia/Tokyo", start = null)
            val zone = TimeZone.of("America/Los_Angeles")
            val shown = occurrences(allDay, zone).map { it.days }.toSet()
            val exported =
                expand(allDay, zone).map { row ->
                    Instant.fromEpochMilliseconds(row.startEpochMillis).toJdn(TimeZone.UTC)
                }

            shown shouldBe setOf(day..day)
            exported shouldBe listOf(day)
        }

    /** Asserts the calendar's and the export's start instants of [record] within [range] agree, and returns them. */
    private suspend fun assertSameStarts(
        record: PersonalEventRecord,
        zone: TimeZone,
    ): Set<Instant> {
        val shown = occurrences(record, zone).map { it.startInstant() }.toSet()
        val exported = expand(record, zone).map { Instant.fromEpochMilliseconds(it.startEpochMillis) }.toSet()
        exported shouldBe shown
        shown.isNotEmpty() shouldBe true
        return shown
    }

    private suspend fun occurrences(
        record: PersonalEventRecord,
        zone: TimeZone,
    ): List<PersonalOccurrence> {
        val repository =
            EventsRepository(
                settings = flowOf(EventsSettings(EventPreferences(emptySet(), zone), emptySet(), hijriOffset = null)),
                inputs =
                    EventInputs(
                        personal = PersonalEventsSource { flowOf(listOf(record)) },
                        device = DeviceEventsSource { flowOf(emptyList()) },
                        ics = IcsEventsSource { flowOf(emptyList()) },
                    ),
                clock = clock,
                zone = { zone },
                computeDispatcher = Dispatchers.Unconfined,
            )
        return repository
            .days(range)
            .first()
            .flatMap { it.personal }
            .distinct()
    }

    private fun expand(
        record: PersonalEventRecord,
        zone: TimeZone,
    ) = IcsOccurrenceExpander(zone).expand(
        subscriptionId = 1,
        event = mapping.toIcs(export(record), today = day),
        window = window(zone),
        overrides = mapping.overrideEvents(export(record)),
    )

    /** The instants whose display-zone day lies in [range]. */
    private fun window(zone: TimeZone) =
        InstantWindow(
            range.start
                .toLocalDate()
                .atStartOfDayIn(zone)
                .toEpochMilliseconds(),
            (range.endInclusive + 1).toLocalDate().atStartOfDayIn(zone).toEpochMilliseconds(),
        )

    /** The start of this occurrence: its first own-zone date at its own-zone minute. */
    private fun PersonalOccurrence.startInstant(): Instant = instant(days.start, startMinute ?: 0, timeZoneId)

    private fun instant(
        jdn: Jdn,
        minute: Int,
        zoneId: String,
    ): Instant = jdn.toLocalDate().atTime(minute / 60, minute % 60).toInstant(TimeZone.of(zoneId))

    private fun export(record: PersonalEventRecord) =
        ExportRecord(
            event = record.event,
            recurrence = record.recurrence,
            reminders = emptyList(),
            exceptionDays = record.exceptions.map { it.value },
            overrides = record.overrides,
        )

    private fun record(
        zoneId: String,
        start: Int?,
        rule: RecurrenceRule? = null,
        overrides: List<EventOverrideEntity> = emptyList(),
    ): PersonalEventRecord {
        val end = start?.let { it + 60 }
        return PersonalEventRecord(
            event =
                PersonalEventEntity(
                    id = 1,
                    title = "Standup",
                    calendarSystem = CalendarSystem.GREGORIAN,
                    startJdn = day.value,
                    startMinute = start,
                    endJdn = if (end != null && end >= MINUTES_PER_DAY) day.value + 1 else day.value,
                    endMinute = end?.rem(MINUTES_PER_DAY),
                    timeZoneId = zoneId,
                    createdAtEpochMillis = 0,
                    updatedAtEpochMillis = 0,
                ),
            recurrence = rule,
            overrides = overrides,
        )
    }

    private fun override(
        original: Jdn,
        start: Jdn,
        minute: Int,
        cancelled: Boolean = false,
    ) = EventOverrideEntity(
        eventId = 1,
        originalJdn = original.value,
        title = "Standup",
        startJdn = start.value,
        startMinute = minute,
        endJdn = if (minute + 60 >= MINUTES_PER_DAY) start.value + 1 else start.value,
        endMinute = (minute + 60) % MINUTES_PER_DAY,
        cancelled = cancelled,
    )

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
