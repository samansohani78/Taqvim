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
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.events.PersonalExpansion
import ir.taqvim.data.events.RoomPersonalEventsSource
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-1003 (R): EXDATE and RECURRENCE-ID components imported into Room, expanded, exported and imported again. */
@RunWith(AndroidJUnit4::class)
class IcsExceptionsRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val events = db.personalEventDao()
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-13T12:00:00Z")
        }
    private val zone = { TimeZone.of("Asia/Tehran") }
    private val importer = IcsImporter(events, db.reminderDao(), RoomTransactionRunner(db), clock, zone)
    private val exporter = IcsExporter(events, db.reminderDao(), { CalendarProvider.DEFAULT }, clock, zone)
    private val monday = LocalDate(2026, 7, 6).toJdn().value
    private val nowruz1405 = LocalDate(2026, 3, 21).toJdn().value
    private val nowruz1406 = LocalDate(2027, 3, 21).toJdn().value

    @After
    fun close() {
        db.close()
    }

    /** Occurrences of the event with [uid] from [from] to [until] as (day, title) pairs. */
    private suspend fun occurrences(
        uid: String,
        from: Long,
        until: Long,
    ): List<Pair<Long, String>> {
        val id = events.getByIcsUid(uid).shouldNotBeNull().id
        val days = Jdn(from)..Jdn(until)
        return RoomPersonalEventsSource(events)
            .events(days)
            .first()
            .filter { it.event.id == id }
            .flatMap { PersonalExpansion.expand(it, days, CalendarProvider.DEFAULT) }
            .map { it.days.start.value to it.title }
    }

    @Test
    fun exceptionsAndOverridesAreStoredExpandedAndExportedBack(): Unit =
        runTest {
            val imported = importer.import(GoldenFile.load("golden/ics/t1003-exceptions.ics").body)

            imported.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(imported.created, imported.replaced, imported.skipped) shouldBe listOf(2, 0, 0)
            imported.warnings shouldBe emptyList()
            val lesson = events.getByIcsUid("class@taqvim.test").shouldNotBeNull()
            events.exceptionDays(lesson.id) shouldBe listOf(monday + 7)
            val tuesday = monday + 15
            val moved =
                EventOverrideEntity(lesson.id, monday + 14, "Class (moved)", "Room 9", tuesday, 900, tuesday, 960)
            val cancelled = EventOverrideEntity(lesson.id, monday + 21, "Class", "", monday + 21, 570, monday + 21, 570)
            events.overrides(lesson.id) shouldBe listOf(moved, cancelled.copy(cancelled = true))
            val lessons =
                listOf(0 to "Class", 15 to "Class (moved)", 28 to "Class", 35 to "Class")
                    .map { monday + it.first to it.second }
            occurrences("class@taqvim.test", monday, monday + 40) shouldBe lessons
            val esfand = listOf(nowruz1406 + 1 to "Esfand party (moved)")
            occurrences("esfand@taqvim.test", nowruz1405 - 1, nowruz1406 + 10) shouldBe esfand

            val text = exporter.export()
            val lines = text.lines().map { it.trim() }
            lines shouldContain "EXDATE;TZID=Asia/Tehran:20260713T093000"
            lines shouldContain "EXDATE;TZID=Asia/Tehran:20260727T093000"
            lines shouldContain "RECURRENCE-ID;TZID=Asia/Tehran:20260720T093000"
            lines shouldContain "RECURRENCE-ID;VALUE=DATE:20270321"
            lines shouldContain "EXDATE;VALUE=DATE:20260321"
            lines shouldContain "RDATE;VALUE=DATE:20270321"
            lines shouldNotContain "RDATE;VALUE=DATE:20260321"

            val again = importer.import(text)
            again.shouldBeInstanceOf<ImportResult.Imported>()
            listOf(again.created, again.replaced, again.skipped) shouldBe listOf(0, 2, 0)
            events.exceptionDays(lesson.id) shouldBe listOf(monday + 7, monday + 21)
            events.overrides(lesson.id).map { it.originalJdn } shouldBe listOf(monday + 14)
            occurrences("class@taqvim.test", monday, monday + 40) shouldBe lessons
            occurrences("esfand@taqvim.test", nowruz1405 - 1, nowruz1406 + 10) shouldBe esfand
        }
}
