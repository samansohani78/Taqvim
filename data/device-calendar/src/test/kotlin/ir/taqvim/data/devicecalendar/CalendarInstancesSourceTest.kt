/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import android.Manifest
import android.app.Application
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** T-602 (R): provider rows (all-day, multi-day, deleted, invisible), permission, failures and change notifications. */
@RunWith(AndroidJUnit4::class)
class CalendarInstancesSourceTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val provider = FakeCalendarProvider.install()
    private val source = CalendarInstancesSource(application)
    private val window = InstantWindow(1_000L, 9_000L)

    private val fixtures =
        listOf(
            InstanceRow(1L, 10L, "All day", 0L, 86_400_000L, allDay = true, displayColor = 0x336699, true, false),
            InstanceRow(2L, 10L, "Three days", 5_000L, 259_205_000L, allDay = false, displayColor = null, true, false),
            InstanceRow(3L, 11L, "Hidden calendar", 2_000L, 3_000L, allDay = false, displayColor = 7, false, false),
            InstanceRow(4L, 10L, null, 4_000L, 4_500L, allDay = false, displayColor = null, true, true),
        )

    private fun permission(granted: Boolean) {
        if (granted) {
            shadowOf(application).grantPermissions(Manifest.permission.READ_CALENDAR)
        } else {
            shadowOf(application).denyPermissions(Manifest.permission.READ_CALENDAR)
        }
    }

    @Test
    fun readsEveryInstanceColumnForTheWindow() {
        permission(granted = true)
        provider.rows += fixtures

        source.query(window) shouldBe InstancesResult.Rows(fixtures)
        provider.queried
            .single()
            .pathSegments
            .takeLast(2) shouldBe listOf("1000", "9000")
        provider.queried.single().authority shouldBe CalendarContract.AUTHORITY
    }

    @Test
    fun withoutPermissionTheProviderIsNotQueried() {
        permission(granted = false)
        provider.rows += fixtures

        source.hasPermission() shouldBe false
        source.query(window) shouldBe InstancesResult.PermissionDenied
        provider.queried.shouldBeEmpty()
    }

    @Test
    fun failuresAreTypedResults() {
        permission(granted = true)
        provider.failure = SecurityException("revoked")
        source.query(window) shouldBe InstancesResult.PermissionDenied

        provider.failure = IllegalStateException("provider crashed")
        source.query(window) shouldBe InstancesResult.Unavailable

        provider.failure = null
        provider.answerNull = true
        source.query(window) shouldBe InstancesResult.Unavailable
    }

    @Test
    fun providerChangesAreObservedUntilCancelled(): Unit =
        runTest {
            val resolver = shadowOf(application.contentResolver)
            source.changes().test {
                application.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
                awaitItem() shouldBe Unit
                resolver.getContentObservers(CalendarContract.CONTENT_URI).size shouldBe 1
                cancelAndIgnoreRemainingEvents()
            }
            resolver.getContentObservers(CalendarContract.CONTENT_URI).shouldBeEmpty()
        }
}
