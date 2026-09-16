/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.events.DeviceEventsSource
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.IcsEventsSource
import ir.taqvim.data.events.PersonalEventRecord
import ir.taqvim.data.events.PersonalEventsSource
import ir.taqvim.feature.notification.OfficialEventSchedule
import ir.taqvim.feature.notification.ReminderPlanner
import ir.taqvim.feature.notification.ReminderSetup
import ir.taqvim.feature.search.SearchEventKind
import ir.taqvim.feature.timeline.TimelineEventKind
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * I08: the same personal or subscribed event reaches the timeline (through the repository), the reminder planner and
 * the search at the same instants and days, for zones ahead of and behind the display zone, a moved override, a
 * cancelled instance, a UTC series and an all-day event (T-305, T-804, T-900, T-1001).
 */
class CrossSurfaceContractTest {
    private val day = LocalDate(2026, 9, 15).toJdn()
    private val range: JdnRange = day - 1..day + 15
    private val tehran = TimeZone.of("Asia/Tehran")
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-14T00:00:00Z")
        }

    @Test
    fun `a Tokyo meeting after midnight is reminded when the timeline shows it`(): Unit =
        runTest {
            assertTimelineMatchesReminders(record("Asia/Tokyo", start = 30), TimeZone.UTC)
        }

    @Test
    fun `a weekly Los Angeles evening meeting is reminded when a Tehran reader sees it`(): Unit =
        runTest {
            assertTimelineMatchesReminders(
                record("America/Los_Angeles", start = 23 * 60, rule = RecurrenceRule(Frequency.WEEKLY)),
                tehran,
            )
        }

    @Test
    fun `a weekly series with a moved override and a cancelled week is shown and reminded alike`(): Unit =
        runTest {
            val moved = override(original = day + 7, start = day + 8, minute = 30)
            val cancelled = override(original = day + 14, start = day + 14, minute = 22 * 60, cancelled = true)
            val weekly =
                record(
                    "Asia/Tehran",
                    start = 22 * 60,
                    rule = RecurrenceRule(Frequency.WEEKLY),
                    overrides = listOf(moved, cancelled),
                )
            val starts = assertTimelineMatchesReminders(weekly, tehran)

            starts shouldBe setOf(instant(day, 22 * 60, tehran), instant(day + 8, 30, tehran))
        }

    @Test
    fun `a daily series keeps its own occurrence on the day an override moves to`(): Unit =
        runTest {
            val moved = override(original = day + 2, start = day + 3, minute = 30)
            val daily =
                record(
                    "Asia/Tehran",
                    start = 22 * 60,
                    rule = RecurrenceRule(Frequency.DAILY),
                    overrides = listOf(moved),
                )
            val starts = assertTimelineMatchesReminders(daily, tehran)

            starts.contains(instant(day + 3, 30, tehran)) shouldBe true
            starts.contains(instant(day + 3, 22 * 60, tehran)) shouldBe true
            starts.contains(instant(day + 2, 22 * 60, tehran)) shouldBe false
        }

    @Test
    fun `a UTC series is reminded when the timeline shows it`(): Unit =
        runTest {
            assertTimelineMatchesReminders(
                record("UTC", start = 21 * 60 + 30, rule = RecurrenceRule(Frequency.WEEKLY)),
                tehran,
            )
        }

    @Test
    fun `an all-day event is shown, reminded and found on its own date`(): Unit =
        runTest {
            val allDay = record("Asia/Tokyo", start = null)
            val zone = TimeZone.of("America/Los_Angeles")

            timeline(allDay, zone).map { it.first }.distinct() shouldBe listOf(day)
            reminders(allDay, zone).map { it.toJdn(zone) } shouldBe listOf(day)
            searchDay(allDay, zone, today = day) shouldBe day
        }

    @Test
    fun `a subscription event is found on the day the timeline shows it`(): Unit =
        runTest {
            // 21:00 UTC on 15 September is 00:30 on 16 September in Tehran.
            val begin = instant(day, 21 * 60, TimeZone.UTC).toEpochMilliseconds()
            val row = IcsEventCacheEntity(4, "u1", begin, begin + HOUR, false, "Standup")
            val shown =
                timeline(record = null, zone = tehran, feed = listOf(row))
                    .filter { it.second == TimelineEventKind.SUBSCRIPTION }
                    .map { it.first }

            shown shouldBe listOf(day + 1)
            search(tehran, day, subscriptions = listOf(row)).single().nextDay shouldBe day + 1
        }

    @Disabled(
        "I08 finding: search dates personal events by their stored start date in the event's own zone " +
            "(CompositeSearchEventSource PersonalEventEntity.toSearchEvent), not by the display-zone day the " +
            "calendar shows (ADR-0031); a 00:30 Tokyo meeting is shown on 14 September in UTC but found on " +
            "15 September.",
    )
    @Test
    fun `search finds a Tokyo meeting on the day the timeline shows it`(): Unit =
        runTest {
            val tokyo = record("Asia/Tokyo", start = 30)
            val shown = timeline(tokyo, TimeZone.UTC, days = day - 1..day).map { it.first }.distinct()

            shown shouldBe listOf(day - 1)
            searchDay(tokyo, TimeZone.UTC, today = day - 1) shouldBe day - 1
        }

    @Disabled(
        "I08 finding: search gives a recurring personal event no next day once its first occurrence is past " +
            "(nextDay = startJdn only when it is not before today), while the timeline shows its later occurrences.",
    )
    @Test
    fun `search finds the next occurrence of a recurring series`(): Unit =
        runTest {
            val weekly = record("Asia/Tehran", start = 10 * 60, rule = RecurrenceRule(Frequency.WEEKLY))

            searchDay(weekly, tehran, today = day + 1) shouldBe day + 7
        }

    /** Asserts every personal start the timeline shows in [range] is exactly a planned reminder, and returns them. */
    private suspend fun assertTimelineMatchesReminders(
        record: PersonalEventRecord,
        zone: TimeZone,
    ): Set<Instant> {
        val shown =
            timeline(record, zone)
                .filter { it.second == TimelineEventKind.PERSONAL && it.third != null }
                .mapNotNull { (jdn, _, minute) -> minute?.let { instant(jdn, it, zone) } }
                .toSet()
        val reminded = reminders(record, zone).toSet()
        reminded shouldBe shown
        shown.isNotEmpty() shouldBe true
        return shown
    }

    /** (day, kind, start minute of a segment that starts that day or `null` for all-day) for every timeline event. */
    private suspend fun timeline(
        record: PersonalEventRecord?,
        zone: TimeZone,
        days: JdnRange = range,
        feed: List<IcsEventCacheEntity> = emptyList(),
    ): List<Triple<Jdn, TimelineEventKind, Int?>> {
        val repository =
            EventsRepository(
                settings = flowOf(EventsSettings(EventPreferences(emptySet(), zone), emptySet(), hijriOffset = null)),
                inputs =
                    EventInputs(
                        personal = PersonalEventsSource { flowOf(listOfNotNull(record)) },
                        device = DeviceEventsSource { flowOf(emptyList()) },
                        ics = IcsEventsSource { flowOf(feed) },
                    ),
                clock = clock,
                zone = { zone },
                computeDispatcher = Dispatchers.Unconfined,
            )
        val source = RepositoryTimelineDaysSource(repository::days, flowOf("en"), zone = { zone })
        return source.days(days).first().flatMap { shownDay ->
            shownDay.events
                .filter { it.isAllDay || it.startMinute > 0 }
                .map { Triple(shownDay.jdn, it.kind, if (it.isAllDay) null else it.startMinute) }
        }
    }

    /** Planned reminder instants (0 minutes before) whose display-zone day lies in [range]. */
    private fun reminders(
        record: PersonalEventRecord,
        zone: TimeZone,
    ): List<Instant> {
        val event =
            reminderEvent(
                entity = record.event,
                arithmetic = mapOf(CalendarSystem.GREGORIAN to GregorianCalendarSystem),
                recurrence = record.recurrence,
                reminders = listOf(ReminderEntity(id = 1, eventId = record.event.id, minutesBefore = 0)),
                exceptionDays = record.exceptions.map { it.value },
                overrides = record.overrides,
            ) ?: return emptyList()
        val setup = ReminderSetup(listOf(event), emptyList(), NoOfficialEvents, zone)
        return ReminderPlanner
            .upcoming(clock.now(), setup)
            .map { it.at }
            .filter { it.toJdn(zone) in range }
    }

    private suspend fun searchDay(
        record: PersonalEventRecord,
        zone: TimeZone,
        today: Jdn,
    ): Jdn? =
        search(zone, today, personal = listOf(record.event))
            .single { it.kind == SearchEventKind.PERSONAL }
            .nextDay

    private suspend fun search(
        zone: TimeZone,
        today: Jdn,
        personal: List<PersonalEventEntity> = emptyList(),
        subscriptions: List<IcsEventCacheEntity> = emptyList(),
    ) = CompositeSearchEventSource(
        official = OfficialEventSearchSource(language = { "en" }, today = { today }),
        stores = SearchEventStores({ personal }, { _, _ -> emptyList() }, { _, _ -> subscriptions }),
        today = { today },
        zone = { zone },
        window = 30,
    ).events("Standup", "en", limit = 5).filter { it.kind != SearchEventKind.OFFICIAL }

    private fun instant(
        jdn: Jdn,
        minute: Int,
        zone: TimeZone,
    ): Instant = jdn.toLocalDate().atTime(minute / 60, minute % 60).toInstant(zone)

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

    private object NoOfficialEvents : OfficialEventSchedule {
        override fun title(eventId: EventId): String? = null

        override fun days(
            eventId: EventId,
            from: Jdn,
            until: Jdn,
        ): List<Jdn> = emptyList()
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val HOUR = 3_600_000L
    }
}
