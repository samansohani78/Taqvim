/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.toEntity
import ir.taqvim.data.preferences.DeviceTimeZone
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** T-305 (R): the Room inputs and the Koin module over an in-memory database and a real preferences store. */
@RunWith(AndroidJUnit4::class)
class RoomEventInputsTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val db =
        Room
            .inMemoryDatabaseBuilder(application, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))

    @After
    fun close() {
        storeScope.cancel()
        db.close()
    }

    private fun event(start: Jdn) =
        PersonalEventEntity(
            title = "event",
            calendarSystem = CalendarSystem.PERSIAN,
            startJdn = start.value,
            endJdn = start.value,
            timeZoneId = "Asia/Tehran",
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )

    @Test
    fun personalRecordsCarryTheirRecurrenceRule(): Unit =
        runTest {
            val dao = db.personalEventDao()
            val weekly = RecurrenceRule(Frequency.WEEKLY, interval = 2)
            val recurringId = dao.insert(event(nowruz - 30))
            dao.upsertRecurrence(weekly.toEntity(recurringId, CalendarSystem.PERSIAN))
            val singleId = dao.insert(event(nowruz))
            dao.insert(event(nowruz + 30))

            val records = RoomPersonalEventsSource(dao).events(nowruz..nowruz + 1).first()

            records.map { it.event.id to it.recurrence } shouldBe listOf(recurringId to weekly, singleId to null)
        }

    @Test
    fun personalRecordsCarryTheirExceptionsAndOverrides(): Unit =
        runTest {
            val dao = db.personalEventDao()
            val id = dao.insert(event(nowruz - 30))
            dao.upsertRecurrence(RecurrenceRule(Frequency.DAILY).toEntity(id, CalendarSystem.PERSIAN))
            val moved = EventOverrideEntity(id, nowruz.value, "moved", "", nowruz.value + 1, 600, nowruz.value + 1, 660)
            dao.insertExceptions(listOf(EventExceptionEntity(id, nowruz.value - 1)))
            dao.upsertOverrides(listOf(moved))

            val record = RoomPersonalEventsSource(dao).events(nowruz..nowruz + 1).first().single()

            record.exceptions shouldBe setOf(nowruz - 1)
            record.overrides shouldBe listOf(moved)
        }

    @Test
    fun theKoinModuleCombinesTheDatasetWithStoredEvents(): Unit =
        runTest {
            db.personalEventDao().insert(event(nowruz))
            val clock =
                object : Clock {
                    override fun now(): Instant = Instant.parse("2026-03-01T00:00:00Z")
                }
            val koin =
                koinApplication {
                    modules(
                        eventsDataModule,
                        module {
                            single { db.personalEventDao() }
                            single {
                                UserPreferencesRepository(
                                    UserPreferencesRepository.createDataStore(application, storeScope) { "fa" },
                                )
                            }
                            single<Clock> { clock }
                            single<Flow<TimeZone>>(named(DeviceTimeZone.QUALIFIER)) { DeviceTimeZone.current }
                        },
                    )
                }.koin

            val day = koin.get<EventsRepository>().day(nowruz).first()

            day.isHoliday shouldBe true
            day.personal.map { it.title } shouldBe listOf("event")
        }
}
