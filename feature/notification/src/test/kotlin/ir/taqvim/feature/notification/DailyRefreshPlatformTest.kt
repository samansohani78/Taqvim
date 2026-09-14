/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.feature.notification.TodayFixtures.MIDNIGHT
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf

/** T-1213/T-1214 (R): one replaceable alarm, and a receiver that refreshes only on its own and system broadcasts. */
@RunWith(AndroidJUnit4::class)
class DailyRefreshPlatformTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms = shadowOf(requireNotNull(context.getSystemService(AlarmManager::class.java)))

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theSchedulerKeepsOneAlarmForTheReceiverAndCancelsIt() {
        val scheduler = AlarmDailyRefreshScheduler(context)

        scheduler.schedule(MIDNIGHT - DailyRefreshCoordinator.RETRY)
        scheduler.schedule(MIDNIGHT)

        val alarm = alarms.scheduledAlarms.single()
        alarm.triggerAtMs shouldBe MIDNIGHT.toEpochMilliseconds()
        alarm.getType() shouldBe AlarmManager.RTC
        val intent =
            Intent(context, DailyRefreshReceiver::class.java).setAction(DailyRefreshReceiver.ACTION_REFRESH)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        (PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags) != null) shouldBe true

        scheduler.cancel()
        alarms.scheduledAlarms.shouldBeEmpty()
    }

    @Test
    fun theReceiverRunsTheRefreshesOnlyForHandledBroadcasts() {
        val runs = AtomicInteger()
        startKoin {
            modules(
                module {
                    single<DailyRefreshScheduler> { AlarmDailyRefreshScheduler(context) }
                    single {
                        DailyRefreshCoordinator(
                            listOf(
                                DailyRefresh {
                                    runs.incrementAndGet()
                                    MIDNIGHT
                                },
                            ),
                            get(),
                        )
                    }
                },
            )
        }
        val receiver = DailyRefreshReceiver()

        receiver.onReceive(context, Intent("ir.taqvim.unknown"))
        Thread.sleep(SETTLE_MILLIS)
        runs.get() shouldBe 0

        receiver.onReceive(context, Intent(DailyRefreshReceiver.ACTION_REFRESH))
        receiver.onReceive(context, Intent(Intent.ACTION_LOCALE_CHANGED))
        waitFor { runs.get() == 2 }
    }

    @Test
    fun withoutTheAppGraphTheReceiverDoesNothing() {
        DailyRefreshReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        alarms.scheduledAlarms.shouldBeEmpty()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(ATTEMPTS) {
            if (condition()) return
            Thread.sleep(POLL_MILLIS)
        }
        condition() shouldBe true
    }

    private companion object {
        const val ALARM_REQUEST_CODE = 1_213
        const val ATTEMPTS = 300
        const val POLL_MILLIS = 10L
        const val SETTLE_MILLIS = 50L
    }
}
