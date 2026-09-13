/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.toEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-1003 (R): import of a fixture into Room, duplicates by UID, and export → import without duplicates. */
@RunWith(AndroidJUnit4::class)
class IcsRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val events = db.personalEventDao()
    private val reminders = db.reminderDao()
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-13T12:00:00Z")
        }
    private val zone = { TimeZone.of("Asia/Tehran") }
    private val importer = IcsImporter(events, reminders, RoomTransactionRunner(db), clock, zone)
    private val exporter = IcsExporter(events, reminders, { CalendarProvider.DEFAULT }, clock, zone)
    private val fixture = GoldenFile.load("golden/ics/t1003-import-sample.ics").body

    @After
    fun close() {
        db.close()
    }

    @Test
    fun importsTheFixtureIntoTheDatabase(): Unit =
        runTest {
            val result = importer.import(fixture)

            result.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(result.created, result.replaced, result.skipped) shouldBe listOf(3, 0, 1)
            result.problems.size shouldBe 1
            result.warnings shouldBe listOf(ImportWarning("standup@taqvim.test", ImportIssue.EXCEPTION_DATES_IGNORED))

            val trip = events.getByIcsUid("trip@taqvim.test").shouldNotBeNull()
            listOf(trip.title, trip.startJdn, trip.endJdn) shouldBe
                listOf("Nowruz trip", LocalDate(2026, 3, 21).toJdn().value, LocalDate(2026, 3, 23).toJdn().value)
            reminders.reminders(trip.id).map { it.minutesBefore } shouldBe listOf(15)

            val standup = events.getByIcsUid("standup@taqvim.test").shouldNotBeNull()
            listOf(standup.startMinute, standup.endMinute, standup.timeZoneId) shouldBe
                listOf(540, 555, "Europe/Berlin")
            events.getRecurrence(standup.id)?.frequency shouldBe Frequency.WEEKLY
            events.getRecurrence(standup.id)?.untilJdn shouldBe LocalDate(2026, 12, 30).toJdn().value

            val esfand = events.getByIcsUid("esfand@taqvim.test").shouldNotBeNull()
            esfand.calendarSystem shouldBe CalendarSystem.PERSIAN
            events.getRecurrence(esfand.id)?.invalidDates shouldBe InvalidDatePolicy.NEXT_DAY
            events.all().size shouldBe 3
        }

    @Test
    fun duplicatesAreSkippedOrReplacedByUid(): Unit =
        runTest {
            importer.import(fixture)
            val trip = events.getByIcsUid("trip@taqvim.test").shouldNotBeNull()
            events.update(trip.copy(colorArgb = 0x112233, title = "Edited"))

            val skipped = importer.import(fixture, DuplicatePolicy.SKIP)
            skipped.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(skipped.created, skipped.replaced, skipped.skipped) shouldBe listOf(0, 0, 4)
            events.get(trip.id)?.title shouldBe "Edited"

            val replaced = importer.import(fixture, DuplicatePolicy.REPLACE)
            replaced.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(replaced.created, replaced.replaced, replaced.skipped) shouldBe listOf(0, 3, 1)
            events.all().size shouldBe 3
            events.get(trip.id).shouldNotBeNull().let {
                listOf(it.title, it.colorArgb, it.createdAtEpochMillis) shouldBe
                    listOf("Nowruz trip", 0x112233, trip.createdAtEpochMillis)
            }
            reminders.reminders(trip.id).size shouldBe 1
        }

    @Test
    fun unreadableTextStoresNothing(): Unit =
        runTest {
            importer.import("not a calendar").shouldBeInstanceOf<ImportResult.Unreadable>()
            events.all().size shouldBe 0
        }

    @Test
    fun exportedEventsImportBackWithoutDuplicates(): Unit =
        runTest {
            val start = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1403, 12, 30))
            val persianId = events.insert(personal("Esfand", start, calendar = CalendarSystem.PERSIAN))
            val rule =
                RecurrenceRule(
                    Frequency.YEARLY,
                    invalidDates = InvalidDatePolicy.NEXT_DAY,
                    weekStart = Weekday.SATURDAY,
                )
            events.upsertRecurrence(rule.toEntity(persianId, CalendarSystem.PERSIAN))
            reminders.insertReminder(ReminderEntity(eventId = persianId, minutesBefore = 60))
            val weeklyId = events.insert(personal("Class", LocalDate(2026, 9, 14).toJdn(), minute = 600))
            val weekly = RecurrenceRule(Frequency.WEEKLY, count = 10, byDay = listOf(WeekdayNum(Weekday.MONDAY)))
            events.upsertRecurrence(weekly.toEntity(weeklyId, CalendarSystem.GREGORIAN))

            val text = exporter.export()
            events.get(persianId)?.icsUid shouldBe "taqvim-$persianId-1000"
            text.lines().map { it.trim() } shouldContain "RRULE:FREQ=WEEKLY;COUNT=10;BYDAY=MO"
            exporter.export(setOf(weeklyId)).contains("Esfand") shouldBe false

            val result = importer.import(text)
            result.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(result.created, result.replaced) shouldBe listOf(0, 2)
            events.all().size shouldBe 2
            events.getRecurrence(persianId)?.let { it.frequency to it.invalidDates } shouldBe
                (Frequency.YEARLY to InvalidDatePolicy.NEXT_DAY)
            events.getRecurrence(weeklyId)?.count shouldBe 10
            reminders.reminders(persianId).map { it.minutesBefore } shouldBe listOf(60)
        }

    private fun personal(
        title: String,
        day: Jdn,
        minute: Int? = null,
        calendar: CalendarSystem = CalendarSystem.GREGORIAN,
    ) = PersonalEventEntity(
        title = title,
        calendarSystem = calendar,
        startJdn = day.value,
        startMinute = minute,
        endJdn = day.value,
        endMinute = minute,
        timeZoneId = "Asia/Tehran",
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
    )
}
