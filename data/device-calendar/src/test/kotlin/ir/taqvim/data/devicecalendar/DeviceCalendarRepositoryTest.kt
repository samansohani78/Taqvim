/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Looper
import android.provider.CalendarContract
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** T-602 (R): provider → cache → dated events in UTC+3:30, refresh on change, permission loss, provider failure. */
@RunWith(AndroidJUnit4::class)
class DeviceCalendarRepositoryTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val provider = FakeCalendarProvider.install()
    private val db =
        Room
            .inMemoryDatabaseBuilder(application, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val zone = TimeZone.of("UTC+03:30")
    private val repository =
        DeviceCalendarRepository(
            source = CalendarInstancesSource(application),
            dao = db.deviceEventDao(),
            zones = flowOf(zone),
            ioDispatcher = Dispatchers.Unconfined,
        )
    private val day10 = day(10)..day(10)

    @After
    fun close() {
        db.close()
    }

    private fun day(dayOfMonth: Int): Jdn = LocalDate(2026, 1, dayOfMonth).toJdn()

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilliseconds()

    private fun row(
        id: Long,
        begin: String,
        end: String,
        allDay: Boolean = false,
        visible: Boolean = true,
        deleted: Boolean = false,
    ) = InstanceRow(id, 1L, "event $id", millis(begin), millis(end), allDay, 0x112233, visible, deleted)

    private val fixtures =
        listOf(
            row(1L, "2026-01-10T00:00:00Z", "2026-01-11T00:00:00Z", allDay = true),
            row(2L, "2026-01-09T00:00:00Z", "2026-01-10T00:00:00Z", allDay = true),
            row(3L, "2026-01-08T12:00:00Z", "2026-01-12T12:00:00Z"),
            row(4L, "2026-01-10T08:00:00Z", "2026-01-10T09:00:00Z", visible = false),
            row(5L, "2026-01-10T10:00:00Z", "2026-01-10T11:00:00Z", deleted = true),
            row(6L, "2026-01-10T21:00:00Z", "2026-01-10T22:00:00Z"),
        )

    private fun permission(granted: Boolean) {
        if (granted) {
            shadowOf(application).grantPermissions(Manifest.permission.READ_CALENDAR)
        } else {
            shadowOf(application).denyPermissions(Manifest.permission.READ_CALENDAR)
        }
    }

    private suspend fun ReceiveTurbine<List<DeviceEvent>>.awaitIds(ids: List<Long>) {
        var events = awaitItem()
        while (events.map { it.eventId } != ids) events = awaitItem()
    }

    private suspend fun cachedIds(window: InstantWindow): List<Long> =
        db
            .deviceEventDao()
            .observeInRange(window.fromEpochMillis, window.toEpochMillis)
            .first()
            .map { it.eventId }

    private fun notifyProviderChange() {
        application.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
    }

    @Test
    fun instancesAreFilteredAndDatedInTheDeviceZone(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures

            repository.events(day10).test {
                awaitIds(listOf(3L, 1L))
                cancelAndIgnoreRemainingEvents()
            }
            val allDay = repository.events(day10).first { it.isNotEmpty() }.last()
            allDay.days shouldBe day10
            allDay.colorArgb shouldBe 0xFF112233.toInt()
        }

    @Test
    fun aDeviceZoneChangeRedatesTheInstances(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures
            val zones = MutableStateFlow(zone)
            val moving =
                DeviceCalendarRepository(
                    source = CalendarInstancesSource(application),
                    dao = db.deviceEventDao(),
                    zones = zones,
                    ioDispatcher = Dispatchers.Unconfined,
                )

            moving.events(day10).test {
                // 21:00–22:00 UTC is already the next day at UTC+03:30 …
                awaitIds(listOf(3L, 1L))
                zones.value = TimeZone.UTC
                // … and on the 10th in UTC.
                awaitIds(listOf(3L, 1L, 6L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theDeviceZoneStreamFollowsTimeZoneBroadcasts(): Unit =
        runTest {
            var current = zone
            DeviceTimeZone.changes(application) { current }.test {
                awaitItem() shouldBe zone
                current = TimeZone.of("Asia/Tokyo")
                application.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
                shadowOf(Looper.getMainLooper()).idle()
                awaitItem() shouldBe TimeZone.of("Asia/Tokyo")
                // Another broadcast without a change emits nothing; unrelated broadcasts are not received.
                application.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
                application.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
                shadowOf(Looper.getMainLooper()).idle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            // The receiver is unregistered when collection ends.
            shadowOf(application).registeredReceivers.none { receiver ->
                receiver.intentFilter.hasAction(Intent.ACTION_TIMEZONE_CHANGED)
            } shouldBe true
        }

    @Test
    fun providerChangesRefreshTheCache(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures

            repository.events(day10).test {
                awaitIds(listOf(3L, 1L))
                provider.rows.removeAll { it.eventId == 3L }
                notifyProviderChange()
                awaitIds(listOf(1L))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun withoutPermissionEventsAreEmptyAndTheCacheIsCleared(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures
            val window = DeviceEventMapping.window(day10, zone)

            repository.events(day10).test {
                awaitIds(listOf(3L, 1L))
                permission(granted = false)
                notifyProviderChange()
                awaitIds(emptyList())
                // Asserted here, not after the block: the empty list is the reader being told there is nothing to
                // show, and by then the rows must already be gone. Asserting after `cancel` instead let a CI run
                // pass while the deletion was still owed to a refresh the cancellation had ended (run 36046477919).
                cachedIds(window).shouldBeEmpty()
                cancelAndIgnoreRemainingEvents()
            }
            cachedIds(window).shouldBeEmpty()
        }

    @Test
    fun anUnavailableProviderKeepsTheCache(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures
            val window = DeviceEventMapping.window(day10, zone)
            repository.refresh(window) shouldBe InstancesResult.Rows(fixtures)

            provider.failure = IllegalStateException("provider crashed")
            repository.refresh(window) shouldBe InstancesResult.Unavailable
            cachedIds(window) shouldBe listOf(3L, 2L, 1L, 6L)
        }

    /**
     * A source that has lost the permission while the provider itself answers nothing, so [refresh] leaves the cache
     * alone (`Unavailable` means "any cached instances are still valid") and never emits a change.
     *
     * It isolates the guarantee the reader relies on from the refresh that usually also delivers it:
     * `CalendarInstancesSource` would answer `PermissionDenied` here and clear the window on its way through
     * [DeviceCalendarRepository.refresh], which is why removing that clearing still left every other test passing.
     */
    private object SilentDeniedSource : InstancesSource {
        override fun hasPermission(): Boolean = false

        override fun query(window: InstantWindow): InstancesResult = InstancesResult.Unavailable

        override fun changes(): Flow<Unit> = emptyFlow()
    }

    @Test
    fun theReaderIsNeverToldEventsAreGoneWhileTheirRowsRemain(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures
            val window = DeviceEventMapping.window(day10, zone)
            repository.refresh(window) shouldBe InstancesResult.Rows(fixtures)
            cachedIds(window) shouldBe listOf(3L, 2L, 1L, 6L)

            val denied =
                DeviceCalendarRepository(
                    source = SilentDeniedSource,
                    dao = db.deviceEventDao(),
                    zones = flowOf(zone),
                    ioDispatcher = Dispatchers.Unconfined,
                )
            denied.events(day10).test {
                awaitIds(emptyList())
                cachedIds(window).shouldBeEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aCollectionStartedWithoutPermissionClearsWhatTheLastOneCached(): Unit =
        runTest {
            permission(granted = true)
            provider.rows += fixtures
            val window = DeviceEventMapping.window(day10, zone)
            repository.refresh(window) shouldBe InstancesResult.Rows(fixtures)
            cachedIds(window) shouldBe listOf(3L, 2L, 1L, 6L)

            // The permission is revoked while nothing is collecting, which is what Android does when the user
            // withdraws it: the next reader must not be handed rows the app can no longer read.
            permission(granted = false)
            repository.events(day10).test {
                awaitIds(emptyList())
                cachedIds(window).shouldBeEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
