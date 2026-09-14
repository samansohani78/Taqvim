/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-601 (R): CRUD of personal events, recurrences, reminders and scheduled alarms on an in-memory database. */
@RunWith(AndroidJUnit4::class)
class PersonalDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val events = db.personalEventDao()
    private val reminders = db.reminderDao()

    @After
    fun close() {
        db.close()
    }

    private fun event(
        title: String,
        startJdn: Long,
        endJdn: Long = startJdn,
    ) = PersonalEventEntity(
        title = title,
        calendarSystem = CalendarSystem.PERSIAN,
        startJdn = startJdn,
        endJdn = endJdn,
        timeZoneId = "Asia/Tehran",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    @Test
    fun eventCrud(): Unit =
        runTest {
            val id = events.insert(event("a", START))
            events.get(id) shouldBe event("a", START).copy(id = id)

            val changed = event("b", START + 1).copy(id = id, startMinute = 600, endMinute = 660, colorArgb = -1)
            events.update(changed)
            events.get(id) shouldBe changed

            events.delete(id)
            events.get(id) shouldBe null
        }

    @Test
    fun rangeHoldsOverlappingAndRecurringEvents(): Unit =
        runTest {
            events.insert(event("before", START, START + 5))
            val overlapping = events.insert(event("overlapping", START + 8, START + 12))
            val recurring = events.insert(event("recurring", START - 50))
            events.insert(event("after", START + 100))
            events.upsertRecurrence(RecurrenceRule(Frequency.WEEKLY).toEntity(recurring, CalendarSystem.PERSIAN))

            events.observeInRange(START + 10, START + 20).test {
                awaitItem().map { it.id } shouldBe listOf(recurring, overlapping)
                events.delete(recurring)
                awaitItem().map { it.id } shouldBe listOf(overlapping)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun recurrenceRoundTripsAndCascades(): Unit =
        runTest {
            val id = events.insert(event("x", START))
            val rule =
                RecurrenceRule(
                    frequency = Frequency.MONTHLY,
                    interval = 2,
                    until = Jdn(START + 400),
                    byDay = listOf(WeekdayNum(Weekday.FRIDAY), WeekdayNum(Weekday.MONDAY, -1)),
                    byMonthDay = listOf(30, -1),
                    invalidDates = InvalidDatePolicy.LAST_DAY_OF_MONTH,
                    weekStart = Weekday.SATURDAY,
                )
            events.upsertRecurrence(rule.toEntity(id, CalendarSystem.PERSIAN))
            events.getRecurrence(id)?.toRule() shouldBe rule

            val counted = rule.copy(count = 3, until = null, byDay = emptyList(), byMonthDay = emptyList())
            events.upsertRecurrence(counted.toEntity(id, CalendarSystem.ISLAMIC))
            events.getRecurrence(id)?.toRule() shouldBe counted
            events.getRecurrence(id)?.calendarSystem shouldBe CalendarSystem.ISLAMIC

            events.deleteRecurrence(id)
            events.getRecurrence(id) shouldBe null

            events.upsertRecurrence(rule.toEntity(id, CalendarSystem.PERSIAN))
            events.delete(id)
            events.getRecurrence(id) shouldBe null
        }

    @Test
    fun remindersCrudAndCascade(): Unit =
        runTest {
            val eventId = events.insert(event("x", START))
            reminders.observeReminders(eventId).test {
                awaitItem() shouldBe emptyList()
                val late = ReminderEntity(eventId = eventId, minutesBefore = 60)
                val lateId = reminders.insertReminder(late)
                awaitItem() shouldBe listOf(late.copy(id = lateId))
                val early = ReminderEntity(eventId = eventId, minutesBefore = 10, enabled = false)
                val earlyId = reminders.insertReminder(early)
                awaitItem() shouldBe listOf(early.copy(id = earlyId), late.copy(id = lateId))
                reminders.updateReminder(late.copy(id = lateId, minutesBefore = 5))
                awaitItem().map { it.id } shouldBe listOf(lateId, earlyId)
                reminders.deleteReminder(earlyId)
                awaitItem().map { it.id } shouldBe listOf(lateId)
                events.delete(eventId)
                awaitItem() shouldBe emptyList()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scheduledAlarmsCrud(): Unit =
        runTest {
            val prayer = ScheduledAlarmEntity(kind = AlarmKind.PRAYER, sourceId = null, triggerAtEpochMillis = 300L)
            val reminder = ScheduledAlarmEntity(kind = AlarmKind.REMINDER, sourceId = 7L, triggerAtEpochMillis = 100L)
            val shift = ScheduledAlarmEntity(kind = AlarmKind.SHIFT, sourceId = 2L, triggerAtEpochMillis = 200L)
            val prayerId = reminders.insertAlarm(prayer)
            val reminderId = reminders.insertAlarm(reminder)
            val shiftId = reminders.insertAlarm(shift)

            reminders.alarms() shouldBe
                listOf(reminder.copy(id = reminderId), shift.copy(id = shiftId), prayer.copy(id = prayerId))

            reminders.deleteAlarm(shiftId)
            reminders.deleteAlarms(AlarmKind.PRAYER)
            reminders.alarms() shouldBe listOf(reminder.copy(id = reminderId))
        }

    @Test
    fun officialRemindersAreOnePerEventAndLeadTime(): Unit =
        runTest {
            val official = db.officialReminderDao()
            official.insert(OfficialReminderEntity(eventId = NOWRUZ, daysBefore = 3))
            official.insert(OfficialReminderEntity(eventId = NOWRUZ, daysBefore = 7))
            official.insert(OfficialReminderEntity(eventId = NOWRUZ, daysBefore = 3, enabled = false))

            official.all().map { it.daysBefore to it.enabled } shouldBe listOf(3 to false, 7 to true)

            official.observeAll().test {
                awaitItem().map { it.daysBefore } shouldBe listOf(3, 7)
                official.delete(NOWRUZ, 7)
                awaitItem().map { it.daysBefore } shouldBe listOf(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val START = 2_460_000L
        const val NOWRUZ = "ir.holiday.nowruz-1"
    }
}
