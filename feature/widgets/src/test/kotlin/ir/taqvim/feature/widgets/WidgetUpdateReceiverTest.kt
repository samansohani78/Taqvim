/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.FakeClock
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

/** T-1200 fix: without Koin the update receiver ignores broadcasts instead of failing on a worker thread. */
@RunWith(AndroidJUnit4::class)
class WidgetUpdateReceiverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val frameworkBroadcasts =
        listOf(
            Intent(WidgetBroadcasts.ACTION_WAKE_UP).putExtra(WidgetBroadcasts.EXTRA_TRIGGERS, arrayOf("MinuteTick")),
            Intent("android.intent.action.TIMEZONE_CHANGED"),
            WidgetUpdateReceiver.rescheduleIntent(context),
            WidgetUpdateReceiver.deletedIntent(context, intArrayOf(4)),
        )

    @Before
    fun withoutKoin() {
        stopKoin()
    }

    @Test
    fun withoutKoinNoFailureLeaksOutOfTheReceiver() {
        val uncaught = LinkedBlockingQueue<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> uncaught.put(error) }
        runCatching {
            GlobalContext.getOrNull().shouldBeNull()
            frameworkBroadcasts.forEach { WidgetUpdateReceiver().onReceive(context, it) }
            // The refresher used to be injected lazily on a worker thread, which failed after onReceive returned and
            // surfaced in whichever test ran next.
            uncaught.poll(LEAK_WAIT_MILLIS, TimeUnit.MILLISECONDS).shouldBeNull()
        }.also { Thread.setDefaultUncaughtExceptionHandler(previous) }.getOrThrow()
    }

    @Test
    fun workStartsOnlyForFrameworkBroadcastsWithARefresher() {
        val launched = mutableListOf<Int>()

        fun receiver(refresher: WidgetRefresher?) = WidgetUpdateReceiver({ refresher }, { launched += 1 })

        frameworkBroadcasts.forEach { receiver(null).onReceive(context, it) }
        launched.shouldBeEmpty()

        val refresher = WidgetSamples.refresher(FakeInstalledWidgets(), clock = FakeClock())
        frameworkBroadcasts.forEach { receiver(refresher).onReceive(context, it) }
        receiver(refresher).onReceive(context, Intent("android.intent.action.BOOT_COMPLETED"))
        launched.size shouldBe frameworkBroadcasts.size
    }

    private companion object {
        const val LEAK_WAIT_MILLIS = 500L
    }
}
