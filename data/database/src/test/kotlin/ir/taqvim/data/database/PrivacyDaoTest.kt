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
import ir.taqvim.core.model.CalendarSystem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-1503 (R): the privacy dashboard's row counts follow the tables, and the device calendar copy can be cleared. */
@RunWith(AndroidJUnit4::class)
class PrivacyDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val privacy = db.privacyDao()

    @After
    fun close() {
        db.close()
    }

    @Test
    fun countsFollowTheTablesAndTheDeviceCopyIsCleared(): Unit =
        runTest {
            privacy.observeCounts().test {
                awaitItem() shouldBe StoredRowCounts(0, 0, 0, 0, 0)

                val eventId =
                    db.personalEventDao().insert(
                        PersonalEventEntity(
                            title = "a",
                            calendarSystem = CalendarSystem.PERSIAN,
                            startJdn = START,
                            endJdn = START,
                            timeZoneId = "Asia/Tehran",
                            createdAtEpochMillis = 1L,
                            updatedAtEpochMillis = 1L,
                        ),
                    )
                awaitItem() shouldBe StoredRowCounts(1, 0, 0, 0, 0)
                db.reminderDao().insertReminder(ReminderEntity(eventId = eventId, minutesBefore = 10))
                awaitItem().reminders shouldBe 1
                db.officialReminderDao().insert(OfficialReminderEntity(eventId = "ir.holiday.nowruz-1", daysBefore = 3))
                awaitItem().reminders shouldBe 2
                db.icsSubscriptionDao().insertSubscription(
                    IcsSubscriptionEntity(
                        url = "https://example.org/a.ics",
                        displayName = "A",
                        refreshIntervalMinutes = 60,
                    ),
                )
                awaitItem().subscriptions shouldBe 1
                db.deviceEventDao().insertAll(
                    listOf(DeviceEventCacheEntity(1, 1, 0L, 3_600_000L, allDay = false, title = "b")),
                )
                awaitItem().deviceEvents shouldBe 1
                db.diagnosticsDao().insert(
                    DiagnosticsLogEntity(atEpochMillis = 1L, level = DiagnosticsLevel.INFO, tag = "t", message = "m"),
                )
                awaitItem() shouldBe StoredRowCounts(1, 2, 1, 1, 1)

                privacy.clearDeviceEvents()
                awaitItem() shouldBe StoredRowCounts(1, 2, 1, 0, 1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val START = 2_460_000L
    }
}
