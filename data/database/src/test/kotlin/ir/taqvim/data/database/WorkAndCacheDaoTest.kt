/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.core.workdays.WorkdayProfile
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-601 (R): CRUD of shift rotations, workday profiles, calendar caches and the diagnostics ring. */
@RunWith(AndroidJUnit4::class)
class WorkAndCacheDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @After
    fun close() {
        db.close()
    }

    @Test
    fun shiftRotationsAndRecords(): Unit =
        runTest {
            val dao = db.shiftRotationDao()
            val rotation =
                ShiftRotationEntity(name = "B", anchorJdn = 2_460_000L, pattern = listOf("D", "D", "N", "", ""))
            val id = dao.insertRotation(rotation)
            dao.insertRotation(ShiftRotationEntity(name = "A", anchorJdn = 1L, pattern = emptyList(), isActive = false))

            dao.observeRotations().test {
                awaitItem().map { it.name } shouldBe listOf("A", "B")
                dao.updateRotation(rotation.copy(id = id, name = "C"))
                awaitItem().last() shouldBe rotation.copy(id = id, name = "C")
                cancelAndIgnoreRemainingEvents()
            }

            // Outside the observed range; inserted before subscribing because every table write re-emits.
            dao.upsertRecord(ShiftRotationRecordEntity(id, 25L, "N"))
            dao.observeRecords(id, 10L, 20L).test {
                awaitItem() shouldBe emptyList()
                dao.upsertRecord(ShiftRotationRecordEntity(id, 12L, "N"))
                awaitItem() shouldBe listOf(ShiftRotationRecordEntity(id, 12L, "N"))
                dao.upsertRecord(ShiftRotationRecordEntity(id, 12L, "D", note = "swap"))
                awaitItem() shouldBe listOf(ShiftRotationRecordEntity(id, 12L, "D", note = "swap"))
                dao.deleteRecord(id, 12L)
                awaitItem() shouldBe emptyList()
                dao.upsertRecord(ShiftRotationRecordEntity(id, 11L, ""))
                awaitItem().size shouldBe 1
                dao.deleteRotation(id)
                awaitItem() shouldBe emptyList()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun workdayProfiles(): Unit =
        runTest {
            val dao = db.workdayProfileDao()
            val iran =
                WorkdayProfileEntity(
                    name = "Iran",
                    weekend = setOf(Weekday.FRIDAY, Weekday.THURSDAY),
                    holidaySources = setOf(EventSource.IRAN_OFFICIAL),
                    halfDays = HalfDayPolicy.FULL_WORKDAY,
                    personalLeave = listOf(LeaveRange(Jdn(-3L), Jdn(-1L)), LeaveRange(Jdn(10L), Jdn(10L))),
                )
            val empty =
                iran.copy(
                    name = "Empty",
                    weekend = emptySet(),
                    holidaySources = emptySet(),
                    personalLeave = emptyList(),
                )
            val iranId = dao.insert(iran)
            val emptyId = dao.insert(empty)

            dao.observeAll().test {
                awaitItem() shouldBe listOf(empty.copy(id = emptyId), iran.copy(id = iranId))
                dao.makeDefault(iranId)
                awaitItem().map { it.id to it.isDefault } shouldBe listOf(iranId to true, emptyId to false)
                dao.update(iran.copy(id = iranId, isDefault = true, halfDays = HalfDayPolicy.HALF))
                awaitItem().first().toProfile() shouldBe
                    WorkdayProfile(iran.weekend, iran.holidaySources, HalfDayPolicy.HALF, iran.personalLeave)
                dao.delete(iranId)
                awaitItem().map { it.id } shouldBe listOf(emptyId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun icsSubscriptionsAndCache(): Unit =
        runTest {
            val dao = db.icsSubscriptionDao()
            val feed =
                IcsSubscriptionEntity(url = "https://example.org/a.ics", displayName = "A", refreshIntervalMinutes = 60)
            val id = dao.insertSubscription(feed)
            dao.getSubscription(id) shouldBe feed.copy(id = id)
            val refreshed = feed.copy(id = id, lastFetchedAtEpochMillis = 5L, etag = "\"v1\"", colorArgb = 7)
            dao.updateSubscription(refreshed)
            dao.observeSubscriptions().test {
                awaitItem() shouldBe listOf(refreshed)
                cancelAndIgnoreRemainingEvents()
            }

            val first = IcsEventCacheEntity(id, "u1", 100L, 200L, allDay = false, summary = "s")
            val second = IcsEventCacheEntity(id, "u2", 300L, 400L, allDay = true, summary = "t", location = "l")
            dao.observeEvents(150L, 350L).test {
                awaitItem() shouldBe emptyList()
                dao.replaceEvents(id, listOf(first, second))
                awaitItem() shouldBe listOf(first, second)
                dao.replaceEvents(id, listOf(second))
                awaitItem() shouldBe listOf(second)
                dao.updateSubscription(refreshed.copy(enabled = false))
                awaitItem() shouldBe emptyList()
                cancelAndIgnoreRemainingEvents()
            }
            shouldThrow<IllegalArgumentException> { dao.replaceEvents(id + 1, listOf(first)) }

            dao.deleteSubscription(id)
            dao.getSubscription(id) shouldBe null
        }

    @Test
    fun deviceEventsWindowIsReplaced(): Unit =
        runTest {
            val dao = db.deviceEventDao()
            val a = DeviceEventCacheEntity(1L, 9L, 100L, 150L, allDay = false, title = "a")
            val b = DeviceEventCacheEntity(2L, 9L, 500L, 600L, allDay = false, title = "b", colorArgb = 3)
            dao.insertAll(listOf(a, b))

            dao.observeInRange(0L, 1_000L).test {
                awaitItem() shouldBe listOf(a, b)
                val c = DeviceEventCacheEntity(3L, 9L, 120L, 130L, allDay = true, title = "c")
                dao.replaceWindow(0L, 400L, listOf(c))
                awaitItem() shouldBe listOf(c, b)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deviceEventsOverlappingAWindowAreReplaced(): Unit =
        runTest {
            val dao = db.deviceEventDao()
            val spanning = DeviceEventCacheEntity(1L, 9L, 50L, 250L, allDay = false, title = "spanning")
            val zeroLength = DeviceEventCacheEntity(2L, 9L, 100L, 100L, allDay = false, title = "zero")
            val endsAtStart = DeviceEventCacheEntity(3L, 9L, 10L, 100L, allDay = false, title = "before")
            val startsAtEnd = DeviceEventCacheEntity(4L, 9L, 200L, 300L, allDay = false, title = "after")
            dao.insertAll(listOf(spanning, zeroLength, endsAtStart, startsAtEnd))

            val fresh = DeviceEventCacheEntity(5L, 9L, 150L, 160L, allDay = true, title = "fresh")
            dao.replaceOverlapping(100L, 200L, listOf(fresh))

            dao.observeInRange(0L, 1_000L).test {
                awaitItem() shouldBe listOf(endsAtStart, fresh, startsAtEnd)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun diagnosticsRingKeepsNewestRows(): Unit =
        runTest {
            val dao = db.diagnosticsDao()
            val total = DiagnosticsDao.MAX_ROWS + 3
            repeat(total) {
                val entry =
                    DiagnosticsLogEntity(
                        atEpochMillis = it.toLong(),
                        level = DiagnosticsLevel.INFO,
                        tag = "t",
                        message = "$it",
                    )
                dao.append(entry)
            }

            dao.count() shouldBe DiagnosticsDao.MAX_ROWS
            dao.observeRecent(2).test {
                awaitItem().map { it.message } shouldBe listOf("${total - 1}", "${total - 2}")
                cancelAndIgnoreRemainingEvents()
            }
            dao.trimTo(1)
            dao.count() shouldBe 1
            dao.clear()
            dao.count() shouldBe 0
        }
}
