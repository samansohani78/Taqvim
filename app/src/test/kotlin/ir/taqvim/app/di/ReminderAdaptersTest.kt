/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventId
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.OfficialReminderEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.AlarmKey
import ir.taqvim.feature.notification.OfficialReminder
import ir.taqvim.feature.notification.ReminderAlarm
import ir.taqvim.feature.notification.ReminderOverride
import ir.taqvim.feature.notification.ReminderRule
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1001 wiring: stored events become planner events with enabled reminders, and reminders reach the scheduler. */
class ReminderAdaptersTest {
    private val arithmetic = UserPreferences.defaultsFor("fa").availableArithmetic()
    private val nowruz = CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)
    private val event =
        PersonalEventEntity(
            id = 7,
            title = "Visit",
            calendarSystem = CalendarSystem.PERSIAN,
            startJdn = requireNotNull(arithmetic[CalendarSystem.PERSIAN]).toJdn(nowruz).value,
            startMinute = 600,
            endJdn = requireNotNull(arithmetic[CalendarSystem.PERSIAN]).toJdn(nowruz).value,
            timeZoneId = "Asia/Tehran",
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )

    @Test
    fun `stored events keep their enabled reminders, start and zone`() {
        val reminders =
            listOf(
                ReminderEntity(id = 1, eventId = 7, minutesBefore = 30),
                ReminderEntity(id = 2, eventId = 7, minutesBefore = 60, enabled = false),
                ReminderEntity(id = 3, eventId = 7, minutesBefore = ReminderRule.MAX_MINUTES_BEFORE + 1),
            )

        val planned = reminderEvent(event, arithmetic, recurrence = null, reminders = reminders).shouldNotBeNull()

        planned.id shouldBe 7
        planned.title shouldBe "Visit"
        planned.start shouldBe nowruz
        planned.startMinute shouldBe MinuteOfDay(600)
        planned.timeZoneId shouldBe "Asia/Tehran"
        planned.reminders shouldBe listOf(ReminderRule(1, 30))
    }

    @Test
    fun `events without reminders or in a calendar that cannot be computed are left out`() {
        reminderEvent(event, arithmetic, null, emptyList()).shouldBeNull()
        val nepali = event.copy(calendarSystem = CalendarSystem.NEPALI)
        reminderEvent(
            nepali,
            arithmetic,
            null,
            listOf(ReminderEntity(id = 1, eventId = 7, minutesBefore = 5)),
        ).shouldBeNull()
        val allDay = event.copy(startMinute = null)
        reminderEvent(allDay, arithmetic, null, listOf(ReminderEntity(id = 1, eventId = 7, minutesBefore = 5)))
            .shouldNotBeNull()
            .startMinute
            .shouldBeNull()
    }

    @Test
    fun `exception days and overridden occurrences reach the planner (T-1003)`() {
        val start = event.startJdn
        val moved = EventOverrideEntity(7, start + 7, "Moved", "", start + 8, 720, start + 8, 780)
        val cancelled = EventOverrideEntity(7, start + 14, "Visit", "", start + 14, null, start + 14, cancelled = true)

        val planned =
            reminderEvent(
                event,
                arithmetic,
                recurrence = null,
                reminders = listOf(ReminderEntity(id = 1, eventId = 7, minutesBefore = 5)),
                exceptionDays = listOf(start + 21),
                overrides = listOf(moved, cancelled),
            ).shouldNotBeNull()

        planned.exceptions shouldBe setOf(Jdn(start + 21))
        planned.overrides shouldBe
            listOf(
                ReminderOverride(Jdn(start + 7), Jdn(start + 8), MinuteOfDay(720), "Moved"),
                ReminderOverride(Jdn(start + 14), Jdn(start + 14), null, "Visit", cancelled = true),
            )
        setOf("event_exceptions", "event_overrides").all { it in REMINDER_INPUT_TABLES } shouldBe true
    }

    @Test
    fun `only enabled official opt-ins with an event and a lead time of up to 30 days are planned`() {
        officialReminder(OfficialReminderEntity(id = 4, eventId = NOWRUZ, daysBefore = 3)) shouldBe
            OfficialReminder(4, EventId(NOWRUZ), 3)
        officialReminder(OfficialReminderEntity(id = 5, eventId = NOWRUZ, daysBefore = 0)) shouldBe
            OfficialReminder(5, EventId(NOWRUZ), 0)
        val off = OfficialReminderEntity(id = 6, eventId = NOWRUZ, daysBefore = 7, enabled = false)
        officialReminder(off).shouldBeNull()
        officialReminder(OfficialReminderEntity(id = 7, eventId = " ", daysBefore = 1)).shouldBeNull()
        officialReminder(OfficialReminderEntity(id = 8, eventId = NOWRUZ, daysBefore = 31)).shouldBeNull()
        officialReminder(OfficialReminderEntity(id = 0, eventId = NOWRUZ, daysBefore = 1)).shouldBeNull()
    }

    @Test
    fun `reminders become keyed scheduler alarms and fired alarms show their reminder`(): Unit =
        runTest {
            val now = Instant.parse("2026-03-20T00:00:00Z")
            val at = Instant.parse("2026-03-21T06:00:00Z")
            val source = ReminderAlarmSource { listOf(ReminderAlarm(sourceId = 11, at = at)) }

            source.kind shouldBe AlarmKind.REMINDER
            source.upcomingAlarms(now) shouldBe listOf(AlarmKey(AlarmKind.REMINDER, 11, at))

            val shown = mutableListOf<Pair<Long, Instant>>()
            val delivery = ReminderAlarmDelivery { id, trigger -> shown += id to trigger }
            delivery.kind shouldBe AlarmKind.REMINDER
            delivery.deliver(alarm(sourceId = 11, at = at))
            delivery.deliver(alarm(sourceId = null, at = at))
            shown shouldBe listOf(11L to at)
        }

    private companion object {
        const val NOWRUZ = "ir.holiday.nowruz-1"
    }

    private fun alarm(
        sourceId: Long?,
        at: Instant,
    ): ScheduledAlarmEntity =
        ScheduledAlarmEntity(
            kind = AlarmKind.REMINDER,
            sourceId = sourceId,
            triggerAtEpochMillis = at.toEpochMilliseconds(),
        )
}
