/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContain
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.EventRecurrenceEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.TaqvimDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/**
 * Changing one occurrence of a recurring event redraws the widgets that show it (T-1003, T-1200).
 *
 * Renaming or moving a single occurrence writes to `event_overrides`, and cancelling one writes to
 * `event_exceptions`; neither touches `personal_events`. The widget trigger watched four tables and not those two,
 * so a widget kept showing yesterday's answer until something unrelated happened to redraw it. These run against a
 * real Room database so the invalidation is the database's own, not a stub's.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WidgetOccurrenceRefreshTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = db.personalEventDao()

    // Robolectric starts TaqvimApplication, and so Koin, for every test in the class.
    @After
    fun close() {
        db.close()
        stopKoin()
    }

    private suspend fun weeklyStandup(): Long {
        val id =
            dao.insert(
                PersonalEventEntity(
                    id = 0,
                    title = "Stand-up",
                    calendarSystem = CalendarSystem.PERSIAN,
                    startJdn = START,
                    startMinute = 9 * 60,
                    endMinute = 9 * 60 + 15,
                    endJdn = START,
                    timeZoneId = "Asia/Tehran",
                    createdAtEpochMillis = 0,
                    updatedAtEpochMillis = 0,
                ),
            )
        dao.upsertRecurrence(
            EventRecurrenceEntity(
                eventId = id,
                frequency = Frequency.WEEKLY,
                interval = 1,
                count = null,
                untilJdn = null,
                byDay = emptyList(),
                byMonthDay = emptyList(),
                calendarSystem = CalendarSystem.PERSIAN,
                invalidDates = InvalidDatePolicy.SKIP,
                weekStart = Weekday.SATURDAY,
            ),
        )
        return id
    }

    @Test
    fun `renaming one occurrence refreshes the widgets`(): Unit =
        awaitTable("event_overrides") { id ->
            dao.upsertOverrides(
                listOf(
                    EventOverrideEntity(
                        eventId = id,
                        originalJdn = START + 7,
                        startJdn = START + 7,
                        endJdn = START + 7,
                        startMinute = 10 * 60,
                        endMinute = 10 * 60 + 15,
                        title = "Stand-up (moved)",
                        notes = "",
                        colorArgb = null,
                        cancelled = false,
                    ),
                ),
            )
        }

    @Test
    fun `cancelling one occurrence refreshes the widgets`(): Unit =
        awaitTable("event_exceptions") { id ->
            dao.insertExceptions(listOf(EventExceptionEntity(eventId = id, dayJdn = START + 14)))
        }

    /**
     * Runs [change] on a weekly event and fails unless the widget trigger names [table].
     *
     * Room delivers invalidation on its own executor, so this waits on real time rather than a test scheduler's
     * virtual clock, which never advances it.
     */
    private fun awaitTable(
        table: String,
        change: suspend (Long) -> Unit,
    ): Unit =
        runBlocking {
            val id = weeklyStandup()
            val seen = CompletableDeferred<Set<String>>()
            val collector = launch(Dispatchers.IO) { seen.complete(widgetEventChanges(db).first()) }
            // Give the collector time to subscribe before the write it is waiting for.
            delay(SUBSCRIBE_MILLIS)

            change(id)

            val tables = withTimeout(TIMEOUT_MILLIS) { seen.await() }
            collector.cancel()
            tables shouldContain table
        }

    private companion object {
        const val START = 2461000L
        const val SUBSCRIBE_MILLIS = 300L
        const val TIMEOUT_MILLIS = 10_000L
    }
}
