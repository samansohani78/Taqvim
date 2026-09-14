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
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventId
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.data.database.OfficialReminderEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/** T-1002 wiring: official reminder opt-ins in Room (schema 4), planned at the stored all-day time; change events. */
@RunWith(AndroidJUnit4::class)
class OfficialReminderAdaptersTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = database.officialReminderDao()
    private val store = RoomOfficialReminderStore(dao)

    @After
    fun closeDatabase() {
        database.close()
        // Robolectric starts TaqvimApplication, and so Koin, for every test.
        stopKoin()
    }

    @Test
    fun remindersAreKeptPerEventAndLeadTime(): Unit =
        runBlocking {
            store.setReminder(NOWRUZ, daysBefore = 3, enabled = true)
            store.setReminder(NOWRUZ, daysBefore = 7, enabled = true)
            store.setReminder(YALDA, daysBefore = 0, enabled = true)
            store.setReminder(NOWRUZ, daysBefore = 7, enabled = false)
            store.setReminder(NOWRUZ, daysBefore = 3, enabled = true)

            store.daysBefore(NOWRUZ).first() shouldBe setOf(3)
            store.daysBefore(YALDA).first() shouldBe setOf(0)
            store.daysBefore("ir.holiday.unknown").first() shouldBe emptySet()
            dao.all().size shouldBe 2
            shouldThrow<IllegalArgumentException> { store.setReminder(NOWRUZ, daysBefore = 31, enabled = true) }
            shouldThrow<IllegalArgumentException> { store.setReminder(" ", daysBefore = 1, enabled = true) }
        }

    @Test
    fun theSetupPlansEnabledOptInsAtTheStoredAllDayTime(): Unit =
        runBlocking {
            dao.insert(OfficialReminderEntity(eventId = NOWRUZ, daysBefore = 3))
            dao.insert(OfficialReminderEntity(eventId = YALDA, daysBefore = 0, enabled = false))
            val defaults = UserPreferences.defaultsFor("fa")
            val preferences = repositoryOf(defaults.copy(app = defaults.app.copy(allDayReminderMinute = 480)))

            val setup =
                RoomReminderSetupSource(
                    events = database.personalEventDao(),
                    reminders = database.reminderDao(),
                    officialReminders = dao,
                    preferences = preferences,
                    zone = { TimeZone.of("Asia/Tehran") },
                ).current()

            setup.officials.map { it.eventId to it.daysBefore } shouldBe listOf(EventId(NOWRUZ) to 3)
            setup.allDayTime shouldBe MinuteOfDay(480)
        }

    @Test
    fun changesOfReminderInputsAreAnnounced(): Unit =
        runBlocking {
            val changed = CompletableDeferred<Set<String>>()
            val collector = launch(Dispatchers.IO) { reminderInputChanges(database).collect { changed.complete(it) } }

            withTimeout(10.seconds) {
                var lead = 0
                while (!changed.isCompleted) {
                    dao.insert(OfficialReminderEntity(eventId = NOWRUZ, daysBefore = lead++ % 31))
                    delay(50.milliseconds)
                }
            }

            val tables = changed.await()
            tables.shouldNotBeEmpty()
            REMINDER_INPUT_TABLES.toSet().containsAll(tables) shouldBe true
            collector.cancel()
        }

    private companion object {
        const val NOWRUZ = "ir.holiday.nowruz-1"
        const val YALDA = "ir.ancient.yalda"
    }
}
