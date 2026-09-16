/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/**
 * T-604 (R): the Koin module, Room table, platform alarms and both receivers together — a time-zone broadcast
 * schedules prayer alarms, a boot broadcast restores them, and firing one delivers it and keeps the next.
 */
@RunWith(AndroidJUnit4::class)
class SchedulerIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java).build()
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val alarms: ShadowAlarmManager = shadowOf(alarmManager)
    private val start = Instant.parse("2026-09-13T12:00:00Z")
    private val clock = FakeClock(start)
    private val prayers = FakeSource(AlarmKind.PRAYER, listOf(start + 1.hours, start + 2.hours))
    private val delivered = Channel<ScheduledAlarmEntity>(Channel.UNLIMITED)
    private val delivery =
        object : AlarmDelivery {
            override val kind = AlarmKind.PRAYER

            override suspend fun deliver(alarm: ScheduledAlarmEntity): DeliveryOutcome {
                delivered.send(alarm)
                return DeliveryOutcome.DELIVERED
            }
        }

    @Before
    fun startGraph() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        startKoin {
            androidContext(context)
            modules(
                schedulerModule,
                module {
                    single { db.reminderDao() }
                    single<Clock> { clock }
                    single<AlarmSource> { prayers }
                    single<AlarmDelivery> { delivery }
                },
            )
        }
    }

    @After
    fun stopGraph() {
        stopKoin()
        db.close()
    }

    private fun millis(instant: Instant) = instant.toEpochMilliseconds()

    /** Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field. */
    @Suppress("DEPRECATION")
    private fun ShadowAlarmManager.ScheduledAlarm.pendingIntent(): PendingIntent = requireNotNull(operation)

    private suspend fun awaitUntil(condition: suspend () -> Boolean) {
        withTimeout(10.seconds) {
            while (!condition()) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(10)
            }
        }
    }

    @Test
    fun broadcastsScheduleRestoreAndDeliverAlarms(): Unit =
        runBlocking {
            context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
            awaitUntil { alarms.scheduledAlarms.size == 2 }
            alarms.scheduledAlarms.map { it.triggerAtMs }.sorted() shouldBe
                listOf(millis(start + 1.hours), millis(start + 2.hours))

            alarms.scheduledAlarms.toList().forEach { alarmManager.cancel(it.pendingIntent()) }
            context.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))
            awaitUntil { alarms.scheduledAlarms.size == 2 }

            clock.advanceBy(1.hours)
            alarms.scheduledAlarms
                .minBy { it.triggerAtMs }
                .pendingIntent()
                .send()
            shadowOf(Looper.getMainLooper()).idle()
            withTimeout(10.seconds) { delivered.receive() }.triggerAtEpochMillis shouldBe millis(start + 1.hours)
            awaitUntil { db.reminderDao().alarms().size == 1 }
            db
                .reminderDao()
                .alarms()
                .single()
                .triggerAtEpochMillis shouldBe millis(start + 2.hours)
        }

    @Test
    fun receiversIgnoreUnrelatedIntents() {
        AlarmReceiver().onReceive(context, Intent(AlarmReceiver.ACTION_ALARM))
        AlarmReceiver().onReceive(context, Intent("other").putExtra(AlarmReceiver.EXTRA_ALARM_ID, 1L))
        RescheduleReceiver().onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        prayers.queries shouldBe 0
    }
}
