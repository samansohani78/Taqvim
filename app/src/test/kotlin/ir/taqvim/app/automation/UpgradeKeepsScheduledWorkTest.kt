/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/**
 * An upgrade or a reboot must not cancel background work that belongs to a library (T-1200).
 *
 * This is the regression test for the widget bug shipped in v1.3.0. `StaleWork.cancelAll` called
 * `JobScheduler.cancelAll()` on `MY_PACKAGE_REPLACED` and `BOOT_COMPLETED`, on the premise — written into its own
 * documentation — that "Taqvim now schedules no job of any kind, so every pending job belongs to the removed
 * feature". That premise was false. Glance draws every widget through `androidx.glance.appwidget.AsyncRequestWorker`,
 * a WorkManager worker that WorkManager schedules through the platform JobScheduler, and Android re-sends
 * `APPWIDGET_UPDATE` to every placed widget at exactly those two moments. So the cancel destroyed the jobs that
 * were about to draw the widgets, nothing retried, and every widget sat on `glance_default_loading_layout` — the
 * launcher's spinner — until the user removed and re-added it.
 *
 * It escaped every existing test because all of them place a widget on a *fresh install*, where neither broadcast
 * fires near the placement. The invariant below does not mention widgets on purpose: what has to hold is that the
 * app never cancels work it did not schedule.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeKeepsScheduledWorkTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val jobs = application.getSystemService(JobScheduler::class.java)

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun tearDown() {
        stopKoin()
    }

    /** A job standing in for Glance's `AsyncRequestWorker`: scheduled by a library, owned by this app. */
    private fun scheduleLibraryJob(id: Int) {
        val component = ComponentName(application, "androidx.work.impl.background.systemjob.SystemJobService")
        jobs.schedule(
            JobInfo
                .Builder(id, component)
                .setMinimumLatency(MINIMUM_LATENCY_MILLIS)
                .build(),
        )
    }

    @Test
    fun anUpgradeDoesNotCancelWorkTheAppDidNotSchedule() {
        scheduleLibraryJob(GLANCE_JOB_ID)
        jobs.allPendingJobs.map { it.id } shouldContain GLANCE_JOB_ID

        DayChangeReceiver().onReceive(application, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        jobs.allPendingJobs.map { it.id } shouldContain GLANCE_JOB_ID
    }

    @Test
    fun aRebootDoesNotCancelWorkTheAppDidNotSchedule() {
        scheduleLibraryJob(GLANCE_JOB_ID)

        DayChangeReceiver().onReceive(application, Intent(Intent.ACTION_BOOT_COMPLETED))

        jobs.allPendingJobs.map { it.id } shouldContain GLANCE_JOB_ID
    }

    /** The same holds for a day change, which arrives far more often than either of the other two. */
    @Test
    fun theMidnightAlarmDoesNotCancelScheduledWorkEither() {
        scheduleLibraryJob(GLANCE_JOB_ID)

        DayChangeReceiver().onReceive(application, Intent(DayChangeAlarm.ACTION_MIDNIGHT).putExtra("jdn", 1L))

        jobs.allPendingJobs.any { it.id == GLANCE_JOB_ID } shouldBe true
    }

    private companion object {
        const val GLANCE_JOB_ID = 918_273
        const val MINIMUM_LATENCY_MILLIS = 60_000L
    }
}
