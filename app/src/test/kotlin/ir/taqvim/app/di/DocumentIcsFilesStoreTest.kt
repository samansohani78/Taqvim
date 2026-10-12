/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.toEntity
import ir.taqvim.data.events.ics.IcsDocuments
import ir.taqvim.data.events.ics.IcsExporter
import ir.taqvim.data.events.ics.IcsImporter
import ir.taqvim.data.events.ics.RoomTransactionRunner
import ir.taqvim.feature.settings.IcsDuplicates
import ir.taqvim.feature.settings.IcsExportOutcome
import ir.taqvim.feature.settings.IcsImportOutcome
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/**
 * T-1003 wiring: the calendar files page's store, over the real reader, writer, Room tables and the content
 * resolver. This is the path the screen actually uses — `IcsImporter` has its own unit tests, and having both is
 * the point: it was possible for those to pass while nothing in the app could reach them (ADR-0054).
 */
@RunWith(AndroidJUnit4::class)
class DocumentIcsFilesStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-03-21T06:00:00Z")
        }
    private val tehran = { TimeZone.of("Asia/Tehran") }
    private val store =
        DocumentIcsFilesStore(
            documents = IcsDocuments(context.contentResolver),
            importer =
                IcsImporter(
                    database.personalEventDao(),
                    database.reminderDao(),
                    RoomTransactionRunner(database),
                    clock,
                    tehran,
                ),
            exporter =
                IcsExporter(
                    database.personalEventDao(),
                    database.reminderDao(),
                    { CalendarProvider.DEFAULT },
                    clock,
                    tehran,
                ),
            database = database,
        )

    @After
    fun close() {
        database.close()
        stopKoin()
    }

    @Test
    fun exportingAndImportingBackKeepsTheEventWithItsRuleRemindersExceptionsAndOverrides(): Unit =
        runBlocking {
            val events = database.personalEventDao()
            val id = events.insert(weekly)
            events.upsertRecurrence(RecurrenceRule(Frequency.WEEKLY).toEntity(id, CalendarSystem.PERSIAN))
            database.reminderDao().insertReminder(ReminderEntity(eventId = id, minutesBefore = 30))
            events.insertExceptions(listOf(EventExceptionEntity(id, SECOND_WEEK)))
            events.upsertOverrides(
                listOf(
                    EventOverrideEntity(
                        eventId = id,
                        originalJdn = THIRD_WEEK,
                        title = "Moved standup",
                        startJdn = THIRD_WEEK + 1,
                        endJdn = THIRD_WEEK + 1,
                    ),
                ),
            )
            store.eventCount().first() shouldBe 1

            val file = temporaryFile("round-trip.ics")
            val exported = store.export(file.toString())

            // The series, plus one component for the overridden occurrence (RFC 5545 RECURRENCE-ID).
            exported shouldBe IcsExportOutcome.Exported(2)
            val text = File(file.path!!).readText()
            text shouldContain "BEGIN:VCALENDAR"
            // ADR-0013: a rule counted in a non-Gregorian calendar is written as explicit dates for other apps,
            // and the rule itself travels in Taqvim's own property so a round trip back here is lossless.
            text shouldContain "RDATE;TZID=Asia/Tehran"
            text shouldContain """X-TAQVIM-RECURRENCE:CALENDAR=PERSIAN\;FREQ=WEEKLY"""

            // Importing the same file back must match the stored event by UID rather than duplicate it, which only
            // works because the export minted a UID and stored it.
            store.import(file.toString(), IcsDuplicates.REPLACE) shouldBe
                IcsImportOutcome.Imported(created = 0, replaced = 1, skipped = 0, problems = 0)
            store.eventCount().first() shouldBe 1

            val stored = events.all().single()
            stored.title shouldBe "Standup"
            events.getRecurrence(stored.id).shouldNotBeNull().let {
                it.frequency shouldBe Frequency.WEEKLY
                it.calendarSystem shouldBe CalendarSystem.PERSIAN
            }
            database.reminderDao().reminders(stored.id).map { it.minutesBefore } shouldContainExactly listOf(30)
            events.exceptionDays(stored.id) shouldContainExactly listOf(SECOND_WEEK)
            events.overrides(stored.id).map { it.title } shouldContainExactly listOf("Moved standup")
        }

    @Test
    fun skippingLeavesAStoredEventAsItIs(): Unit =
        runBlocking {
            val events = database.personalEventDao()
            val id = events.insert(weekly)
            val file = temporaryFile("skip.ics")
            store.export(file.toString())
            events.update(events.all().single().copy(title = "Renamed here"))

            store.import(file.toString(), IcsDuplicates.SKIP) shouldBe
                IcsImportOutcome.Imported(created = 0, replaced = 0, skipped = 1, problems = 0)

            events.all().single().let {
                it.id shouldBe id
                it.title shouldBe "Renamed here"
            }
        }

    @Test
    fun aFileThatIsNotACalendarChangesNothing(): Unit =
        runBlocking {
            val file = temporaryFile("notes.txt")
            File(file.path!!).writeText("just some notes")

            store.import(file.toString(), IcsDuplicates.REPLACE) shouldBe IcsImportOutcome.NotCalendar
            database.personalEventDao().all().shouldBeEmptyList()
        }

    @Test
    fun aMissingFileIsReportedRatherThanCrashing(): Unit =
        runBlocking {
            val missing = Uri.fromFile(File(context.cacheDir, "nowhere.ics")).toString()

            store.import(missing, IcsDuplicates.REPLACE) shouldBe IcsImportOutcome.Unreadable
            database.personalEventDao().all().shouldBeEmptyList()
        }

    private fun List<PersonalEventEntity>.shouldBeEmptyList() = size shouldBe 0

    /** A `file:` document the content resolver can open, standing in for one the picker returned. */
    private fun temporaryFile(name: String): Uri =
        Uri.fromFile(File(context.cacheDir, name).apply { parentFile?.mkdirs() })

    private val weekly =
        PersonalEventEntity(
            title = "Standup",
            calendarSystem = CalendarSystem.PERSIAN,
            startJdn = FIRST_WEEK,
            endJdn = FIRST_WEEK,
            startMinute = 9 * 60,
            endMinute = 9 * 60 + 15,
            timeZoneId = "Asia/Tehran",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )

    private companion object {
        const val FIRST_WEEK = 2_461_121L
        const val SECOND_WEEK = FIRST_WEEK + 7
        const val THIRD_WEEK = FIRST_WEEK + 14
    }
}
