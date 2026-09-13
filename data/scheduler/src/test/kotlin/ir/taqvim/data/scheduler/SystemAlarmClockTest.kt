/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** T-604 (R): `ShadowAlarmManager` set, replace and cancel with and without the exact-alarm permission. */
@RunWith(AndroidJUnit4::class)
class SystemAlarmClockTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms: ShadowAlarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val at = Instant.parse("2026-09-13T12:00:00Z")

    @Test
    fun exactAlarmsAreSetReplacedAndCancelled() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val clock = SystemAlarmClock(context)

        clock.canScheduleExact() shouldBe true
        clock.set(7, at, exact = true) shouldBe true
        clock.set(7, at + 1.hours, exact = true) shouldBe true
        clock.set(8, at + 2.hours, exact = true) shouldBe true

        alarms.scheduledAlarms.map { it.triggerAtMs } shouldContainExactlyInAnyOrder
            listOf((at + 1.hours).toEpochMilliseconds(), (at + 2.hours).toEpochMilliseconds())
        val replaced = alarms.scheduledAlarms.first { it.triggerAtMs == (at + 1.hours).toEpochMilliseconds() }
        replaced.getType() shouldBe AlarmManager.RTC_WAKEUP
        replaced.isAllowWhileIdle shouldBe true
        replaced.windowLengthMs shouldBe ShadowAlarmManager.WINDOW_EXACT

        @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
        val operation = requireNotNull(replaced.operation)
        val intent = shadowOf(operation).savedIntent
        intent.component?.className shouldBe AlarmReceiver::class.java.name
        intent.action shouldBe AlarmReceiver.ACTION_ALARM
        intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) shouldBe 7L

        clock.cancel(7)
        clock.cancel(99)
        alarms.scheduledAlarms.map { it.triggerAtMs } shouldBe listOf((at + 2.hours).toEpochMilliseconds())
    }

    @Test
    fun withoutThePermissionAlarmsAreInexact() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val clock = SystemAlarmClock(context)

        clock.canScheduleExact() shouldBe false
        clock.set(9, at, exact = true) shouldBe false
        clock.set(10, at + 1.hours, exact = false) shouldBe false

        alarms.scheduledAlarms.map { it.windowLengthMs }.toSet() shouldBe setOf(ShadowAlarmManager.WINDOW_HEURISTIC)
        alarms.scheduledAlarms.all { it.isAllowWhileIdle } shouldBe true
    }
}
