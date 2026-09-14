/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.feature.notification.AthanPrayer
import ir.taqvim.feature.notification.PlannedAthan
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/** T-1103: athans and new days are broadcast with their documented extras, and the midnight alarm is kept. */
@RunWith(AndroidJUnit4::class)
class AutomationBroadcastsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    private fun broadcasts(action: String): List<Intent> =
        shadowOf(application).broadcastIntents.filter { it.action == action }

    @Test
    fun anAthanThatStartsIsBroadcastWithItsPrayerTimeAndDay() {
        val at = Instant.parse("2026-09-14T15:04:00Z")
        BroadcastAthanEventHook(application).onAthanStarted(PlannedAthan(AthanPrayer.MAGHRIB, Jdn(2_461_298), at))

        val sent = broadcasts(AutomationBroadcasts.ACTION_ATHAN_STARTED).single()
        assertEquals("MAGHRIB", sent.getStringExtra(AutomationBroadcasts.EXTRA_PRAYER))
        assertEquals(at.toEpochMilliseconds(), sent.getLongExtra(AutomationBroadcasts.EXTRA_TIME, 0))
        assertEquals(2_461_298L, sent.getLongExtra(AutomationBroadcasts.EXTRA_JDN, 0))
    }

    @Test
    fun midnightAnnouncesTheNewDayAndSetsTheNextAlarm() {
        val midnight = Intent(application, DayChangeReceiver::class.java).setAction(DayChangeAlarm.ACTION_MIDNIGHT)
        DayChangeReceiver().onReceive(application, midnight)

        val today = Clock.System.now().toJdn(TimeZone.currentSystemDefault())
        val sent = broadcasts(AutomationBroadcasts.ACTION_DAY_CHANGED).single()
        assertEquals(today.value, sent.getLongExtra(AutomationBroadcasts.EXTRA_JDN, 0))
        assertEquals(today.toLocalDate().toString(), sent.getStringExtra(AutomationBroadcasts.EXTRA_DATE))
        assertMidnightAlarmSet()
    }

    @Test
    fun aRebootOrClockChangeOnlySetsTheAlarmAgain() {
        DayChangeReceiver().onReceive(application, Intent(Intent.ACTION_BOOT_COMPLETED))
        DayChangeReceiver().onReceive(application, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertTrue(broadcasts(AutomationBroadcasts.ACTION_DAY_CHANGED).isEmpty())
        assertMidnightAlarmSet()
    }

    private fun assertMidnightAlarmSet() {
        val alarms = shadowOf(application.getSystemService(AlarmManager::class.java))
        assertEquals(1, alarms.scheduledAlarms.size)
        val alarm = alarms.scheduledAlarms.single()
        val expected = nextLocalMidnight(Clock.System.now(), TimeZone.currentSystemDefault())
        assertTrue(alarm.triggerAtMs in expected.toEpochMilliseconds() - SLACK_MILLIS..expected.toEpochMilliseconds())

        @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
        val operation = requireNotNull(alarm.operation)
        assertEquals(DayChangeAlarm.ACTION_MIDNIGHT, shadowOf(operation).savedIntent.action)
    }

    private companion object {
        /** The alarm and the expectation are computed a moment apart. */
        const val SLACK_MILLIS = 60_000L
    }
}
