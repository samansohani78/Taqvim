/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.events.DeviceEventsSource
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.IcsEventsSource
import ir.taqvim.data.events.PersonalEventRecord
import ir.taqvim.data.events.PersonalEventsSource
import ir.taqvim.feature.timeline.TimelineEvent
import ir.taqvim.feature.timeline.TimelineEventKind
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** B03: a personal event in another zone reaches the timeline on its display-zone day, through the repository. */
class TimelineZoneDatingTest {
    private val day = LocalDate(2026, 9, 15).toJdn()
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T00:00:00Z")
        }

    @Test
    fun `a Tokyo meeting after midnight shows on the previous UTC day with its UTC minutes`(): Unit =
        runTest {
            // 00:30–01:30 in Tokyo is 15:30–16:30 UTC on the day before; it used to vanish from both days.
            val days = timeline(meeting(zoneId = "Asia/Tokyo", start = 30), TimeZone.UTC)

            days.getValue(day - 1) shouldBe
                listOf(TimelineEvent("1", TimelineEventKind.PERSONAL, "Standup", false, false, 930, 990))
            days.getValue(day).shouldBeEmpty()
        }

    @Test
    fun `a weekly Los Angeles evening meeting lands on the next Tehran day every week`(): Unit =
        runTest {
            // 23:00–00:00 in Los Angeles (UTC−7 in September) is 09:30–10:30 the next day in Tehran.
            val weekly =
                meeting(zoneId = "America/Los_Angeles", start = 23 * 60, recurrence = RecurrenceRule(Frequency.WEEKLY))
            val days = timeline(weekly, TimeZone.of("Asia/Tehran"), range = day..day + 8)

            // Each occurrence is identified by its event and original (Los Angeles) day, ADR-0034.
            fun expected(original: Jdn) =
                TimelineEvent("1@${original.value}", TimelineEventKind.PERSONAL, "Standup", false, false, 570, 630)
            days.getValue(day + 1) shouldBe listOf(expected(day))
            days.getValue(day + 8) shouldBe listOf(expected(day + 7))
            (days.keys - setOf(day + 1, day + 8)).forEach { days.getValue(it).shouldBeEmpty() }
        }

    private suspend fun timeline(
        record: PersonalEventRecord,
        zone: TimeZone,
        range: JdnRange = day - 1..day,
    ): Map<Jdn, List<TimelineEvent>> {
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
                zones = flowOf(zone),
                computeDispatcher = Dispatchers.Unconfined,
            )
        val source = RepositoryTimelineDaysSource(repository::days, flowOf("en"), zones = flowOf(zone))
        return source.days(range).first().associate { it.jdn to it.events }
    }

    private fun meeting(
        zoneId: String,
        start: Int,
        recurrence: RecurrenceRule? = null,
    ) = PersonalEventRecord(
        event =
            PersonalEventEntity(
                id = 1,
                title = "Standup",
                calendarSystem = CalendarSystem.GREGORIAN,
                startJdn = day.value,
                startMinute = start,
                endJdn = if (start + 60 >= 24 * 60) day.value + 1 else day.value,
                endMinute = (start + 60) % (24 * 60),
                timeZoneId = zoneId,
                createdAtEpochMillis = 0,
                updatedAtEpochMillis = 0,
            ),
        recurrence = recurrence,
    )
}
