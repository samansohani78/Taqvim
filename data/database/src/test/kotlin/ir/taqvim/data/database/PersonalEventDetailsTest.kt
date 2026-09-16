/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Review I02 (R): events are read with their recurrence, exceptions, overrides and reminders in one transaction, with
 * a number of queries that does not grow with the number of events.
 */
@RunWith(AndroidJUnit4::class)
class PersonalEventDetailsTest {
    private val selects = AtomicInteger()
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryCallback(Dispatchers.Unconfined) { sql, _ ->
                if (sql.trimStart().startsWith("SELECT", ignoreCase = true)) selects.incrementAndGet()
            }.build()
    private val events = db.personalEventDao()
    private val reminders = db.reminderDao()

    @After
    fun close() {
        db.close()
    }

    private fun event(start: Long) =
        PersonalEventEntity(
            title = "e$start",
            calendarSystem = CalendarSystem.PERSIAN,
            startJdn = start,
            endJdn = start,
            timeZoneId = "Asia/Tehran",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    /** A weekly event starting on [start] with one exception, one override and two reminders; returns its id. */
    private suspend fun detailedEvent(start: Long): Long {
        val id = events.insert(event(start))
        events.upsertRecurrence(RecurrenceRule(Frequency.WEEKLY).toEntity(id, CalendarSystem.PERSIAN))
        events.insertExceptions(listOf(EventExceptionEntity(id, start + 14), EventExceptionEntity(id, start + 7)))
        val moved = EventOverrideEntity(id, start + 21, "moved", "", start + 22, 600, start + 22, 660)
        events.upsertOverrides(listOf(moved))
        reminders.insertReminder(ReminderEntity(eventId = id, minutesBefore = 60))
        reminders.insertReminder(ReminderEntity(eventId = id, minutesBefore = 10))
        return id
    }

    @Test
    fun eachEventCarriesItsOwnChildren(): Unit =
        runTest {
            val detailed = detailedEvent(START)
            val plain = events.insert(event(START + 1))

            val range = events.observeInRange(START, START + 1).first().associateBy { it.event.id }
            range.getValue(detailed).recurrence?.frequency shouldBe Frequency.WEEKLY
            range
                .getValue(detailed)
                .exceptions
                .map { it.dayJdn }
                .sorted() shouldBe listOf(START + 7, START + 14)
            range.getValue(detailed).overrides.map { it.originalJdn } shouldBe listOf(START + 21)
            range.getValue(plain) shouldBe
                PersonalEventDetails(event(START + 1).copy(id = plain), null, emptyList(), emptyList())

            val all = events.allWithReminders().associateBy { it.event.id }
            all
                .getValue(detailed)
                .reminders
                .map { it.minutesBefore }
                .sorted() shouldBe listOf(10, 60)
            all.getValue(detailed).exceptions shouldHaveSize 2
            all.getValue(plain).reminders shouldBe emptyList()
            all.getValue(plain).recurrence shouldBe null
        }

    @Test
    fun readsOfAThousandEventsUseAFixedNumberOfQueries(): Unit =
        runTest {
            db.withTransaction { repeat(EVENTS) { detailedEvent(START + it) } }

            selects.set(0)
            events.observeInRange(START, START + EVENTS).first() shouldHaveSize EVENTS
            // One query for the events, one per relation for each batch of at most 999 parents, and Room's checks of
            // its invalidation tracker — a few queries whatever the number of events (per-event reads needed 3 001).
            selects.get() shouldBeLessThanOrEqual MAX_RANGE_SELECTS

            selects.set(0)
            val all = events.allWithReminders()
            all shouldHaveSize EVENTS
            all.sumOf { it.reminders.size } shouldBe 2 * EVENTS
            selects.get() shouldBeLessThanOrEqual MAX_REMINDER_SELECTS
        }

    private companion object {
        const val START = 2_460_000L
        const val EVENTS = 1_000
        const val MAX_RANGE_SELECTS = 10
        const val MAX_REMINDER_SELECTS = 12
    }
}
