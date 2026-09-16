/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.events.ics.RoomTransactionRunner
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.events.PersonalEvent
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/** T-1000 wiring: personal events stored through the Room DAOs (schema 3) with their recurrence and reminders. */
@RunWith(AndroidJUnit4::class)
class RoomPersonalEventStoreTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private var nowMillis = CREATED
    private val clock =
        object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
        }
    private val arithmetic = UserPreferences.defaultsFor("fa").availableArithmetic()
    private val store =
        RoomPersonalEventStore(
            events = database.personalEventDao(),
            reminders = database.reminderDao(),
            transactions = RoomTransactionRunner(database),
            clock = clock,
            arithmetic = { arithmetic },
        )
    private val nowruz = CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1)
    private val birthday =
        PersonalEvent(
            title = "تولد",
            notes = "کیک",
            calendar = CalendarSystem.PERSIAN,
            start = nowruz,
            end = nowruz,
            timeZoneId = "Asia/Tehran",
            colorArgb = COLOR,
            recurrence = RecurrenceRule(Frequency.YEARLY, invalidDates = InvalidDatePolicy.NEXT_DAY),
            reminderMinutes = listOf(10, 60),
            sourceLink = "https://example.org/birthday",
        )

    @After
    fun closeDatabase() {
        database.close()
        // Robolectric starts TaqvimApplication, and so Koin, for every test.
        stopKoin()
    }

    @Test
    fun savedEventsLoadBackWithRecurrenceRemindersAndSourceLink(): Unit =
        runBlocking {
            val id = store.save(birthday)

            store.load(id) shouldBe birthday.copy(id = id)
            val row = database.personalEventDao().get(id).shouldNotBeNull()
            row.startJdn shouldBe PersianCalendarSystem.toJdn(nowruz).value
            row.createdAtEpochMillis shouldBe CREATED
            row.sourceLink shouldBe "https://example.org/birthday"
        }

    @Test
    fun updatesKeepCreationTimeAndIcsUidAndReplaceRecurrenceAndReminders(): Unit =
        runBlocking {
            val id = store.save(birthday)
            val stored = database.personalEventDao().get(id).shouldNotBeNull()
            database.personalEventDao().update(stored.copy(icsUid = "uid-7"))
            database.personalEventDao().insertExceptions(listOf(EventExceptionEntity(id, stored.startJdn + 365)))
            val movedDay = stored.startJdn + 731
            database.personalEventDao().upsertOverrides(
                listOf(EventOverrideEntity(id, stored.startJdn + 730, "moved", "", movedDay, null, movedDay)),
            )
            nowMillis = UPDATED

            val edited =
                birthday.copy(
                    id = id,
                    title = "جشن",
                    recurrence = null,
                    reminderMinutes = listOf(30),
                    sourceLink = null,
                )
            store.save(edited) shouldBe id

            store.load(id) shouldBe edited
            val row = database.personalEventDao().get(id).shouldNotBeNull()
            row.createdAtEpochMillis shouldBe CREATED
            row.updatedAtEpochMillis shouldBe UPDATED
            row.icsUid shouldBe "uid-7"
            database.personalEventDao().getRecurrence(id).shouldBeNull()
            database.personalEventDao().exceptionDays(id) shouldBe emptyList()
            database.personalEventDao().overrides(id) shouldBe emptyList()
            database.reminderDao().reminders(id).map { it.minutesBefore } shouldBe listOf(30)
        }

    @Test
    fun oneOccurrenceLoadsSavesAndCancelsAsItsOwnChange(): Unit =
        runBlocking {
            val id = store.save(birthday)
            val start = PersianCalendarSystem.toJdn(nowruz)
            val nextYear = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1406, 1, 1))

            // Unchanged, the occurrence is the series moved to its day, without repetition.
            val opened = store.loadOccurrence(id, nextYear).shouldNotBeNull()
            opened.recurrence.shouldBeNull()
            opened.start shouldBe CalendarDate(CalendarSystem.PERSIAN, 1406, 1, 1)
            opened.title shouldBe birthday.title
            opened.reminderMinutes shouldBe birthday.reminderMinutes
            store.loadOccurrence(id, nextYear + 1).shouldBeNull()
            store.loadOccurrence(id + 99, nextYear).shouldBeNull()

            // A changed occurrence is stored as its override and loads back; the series is untouched.
            val moved = CalendarDate(CalendarSystem.PERSIAN, 1406, 1, 3)
            val changed = opened.copy(title = "جشن", start = moved, end = moved, startMinute = 600, endMinute = 660)
            store.saveOccurrence(id, nextYear, changed)
            store.loadOccurrence(id, nextYear) shouldBe changed
            store.load(id) shouldBe birthday.copy(id = id)
            val override = database.personalEventDao().overrides(id).single()
            listOf(override.originalJdn, override.startJdn, override.cancelled) shouldBe
                listOf(nextYear.value, nextYear.value + 2, false)
            shouldThrow<IllegalArgumentException> { store.saveOccurrence(id, nextYear + 1, changed) }

            // Cancelling keeps the other occurrences and replaces the change.
            store.cancelOccurrence(id, nextYear)
            database
                .personalEventDao()
                .overrides(id)
                .single()
                .cancelled shouldBe true
            store.loadOccurrence(id, nextYear).shouldNotBeNull().title shouldBe birthday.title
            store.loadOccurrence(id, start).shouldNotBeNull()
        }

    @Test
    fun seriesEditsKeepOnlyChangesOfDaysThatStillOccur(): Unit =
        runBlocking {
            val daily = birthday.copy(recurrence = RecurrenceRule(Frequency.DAILY))
            val id = store.save(daily)
            val start = PersianCalendarSystem.toJdn(nowruz)
            for ((offset, title) in listOf(1 to "A", 2 to "B")) {
                val day = start + offset
                store.saveOccurrence(id, day, store.loadOccurrence(id, day).shouldNotBeNull().copy(title = title))
            }
            store.cancelOccurrence(id, start + 3)
            database.personalEventDao().insertExceptions(listOf(EventExceptionEntity(id, start.value + 4)))

            // Every other day from the same start: days 2 and 4 remain occurrences, 1 and 3 do not.
            store.save(daily.copy(id = id, recurrence = RecurrenceRule(Frequency.DAILY, interval = 2)))

            database.personalEventDao().overrides(id).map { it.originalJdn - start.value to it.title } shouldBe
                listOf(2L to "B")
            database.personalEventDao().exceptionDays(id) shouldBe listOf(start.value + 4)
        }

    @Test
    fun deletingRemovesTheEventWithItsRemindersAndMissingOrUncomputableEventsLoadAsNull(): Unit =
        runBlocking {
            val id = store.save(birthday)
            store.delete(id)

            store.load(id).shouldBeNull()
            database.reminderDao().reminders(id) shouldBe emptyList()

            val nepali =
                database.personalEventDao().insert(
                    PersonalEventEntity(
                        title = "Dashain",
                        calendarSystem = CalendarSystem.NEPALI,
                        startJdn = 2_461_000,
                        endJdn = 2_461_000,
                        timeZoneId = "Asia/Kathmandu",
                        createdAtEpochMillis = CREATED,
                        updatedAtEpochMillis = CREATED,
                    ),
                )
            store.load(nepali).shouldNotBeNull().calendar shouldBe CalendarSystem.NEPALI
            val withoutNepali =
                RoomPersonalEventStore(
                    events = database.personalEventDao(),
                    reminders = database.reminderDao(),
                    transactions = RoomTransactionRunner(database),
                    clock = clock,
                    arithmetic = { arithmetic - CalendarSystem.NEPALI },
                )
            withoutNepali.load(nepali).shouldBeNull()
        }

    private companion object {
        const val CREATED = 1_000L
        const val UPDATED = 2_000L
        const val COLOR = -16_776_961
    }
}
