/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import ir.taqvim.app.MainActivity
import ir.taqvim.app.automation.DayChangeReceiver
import ir.taqvim.feature.widgets.WidgetKind
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Every widget kind must end up showing content a user can read (T-1200).
 *
 * This exists because its predecessor asserted that the host received *some* `RemoteViews* and called that a pass.
 * A widget stuck on `glance_default_loading_layout` forever satisfied that, and shipped: on a physical phone both
 * placed widgets span a spinner indefinitely while this suite was green on four API levels. [RenderedWidget]
 * refuses the placeholder by construction, and every assertion here goes through it.
 */
class DeviceWidgetContentTest {
    private val probe = WidgetHostProbe(appContext, HOST_ID)

    @Before
    fun setUp() {
        keepScreenOn()
        updatePreferences { it.copy(onboardingCompleted = true) }
        shell("appwidget grantbind --package ${appContext.packageName} --user 0")
        probe.start()
    }

    @After
    fun tearDown() {
        probe.stop()
    }

    @Test
    fun everyWidgetKindRendersContentAndNotTheLoadingPlaceholder() {
        val providers = probe.providers()
        check(providers.size >= WidgetKind.entries.size) {
            "Only ${providers.size} providers installed for ${WidgetKind.entries.size} widget kinds"
        }
        val rendered = mutableListOf<RenderedWidget>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val context = checkNotNull(activity)
            providers.forEach { rendered += probe.render(it, context) }
        }

        val stuck = rendered.filter { it.isLoadingPlaceholder }
        check(stuck.isEmpty()) {
            "These widgets never showed content and would sit on a spinner on a launcher:\n" +
                stuck.joinToString(separator = "\n") { "  $it" }
        }
        val failed = rendered.filter { it.showedErrorView }
        check(failed.isEmpty()) { "These widgets showed the error view:\n${failed.joinToString("\n")}" }
    }

    /**
     * T-1216 is the canary: the simplest widget, no configuration, no database, three calendars of today. If this
     * one cannot fill itself the shared widget framework is broken, whatever the others do.
     */
    @Test
    fun theThreeDatesCanaryShowsThreeDates() {
        val provider =
            probe.providers().first { it.provider.shortClassName.contains("ThreeDates") }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            probe.render(provider, checkNotNull(activity)).assertShowsAtLeastTexts(THREE_DATES_LINES)
        }
    }

    /** A second instance of the same kind must render as fully as the first: no shared or stale per-id state. */
    @Test
    fun twoInstancesOfTheSameWidgetBothRender() {
        val provider = probe.providers().first { it.provider.shortClassName.contains("DateWidget1x1") }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val context = checkNotNull(activity)
            val first = probe.render(provider, context)
            val second = probe.render(provider, context)
            first.assertRenderedFinalContent()
            second.assertRenderedFinalContent()
        }
    }

    /**
     * The v1.3.0 bug, end to end: an upgrade or a reboot must not stop a widget drawing.
     *
     * Android re-sends `APPWIDGET_UPDATE` to every placed widget after `MY_PACKAGE_REPLACED` and
     * `BOOT_COMPLETED`, and Glance answers by enqueueing `AsyncRequestWorker` through WorkManager, which the
     * platform runs as a job. The app used to call `JobScheduler.cancelAll()` on exactly those two broadcasts, so
     * the update was cancelled before it could draw and nothing retried. Every automated test placed its widget on
     * a fresh install, where neither broadcast fires, which is why four API levels stayed green while real phones
     * showed a spinner.
     */
    @Test
    fun aWidgetStillRendersAfterAnUpgradeOrRebootBroadcast() {
        val provider = probe.providers().first { it.provider.shortClassName.contains("ThreeDates") }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val context = checkNotNull(activity)
            UPGRADE_BROADCASTS.forEach { action ->
                // MY_PACKAGE_REPLACED and BOOT_COMPLETED are protected broadcasts an app may not send, so the
                // receiver is invoked the way the system invokes it, while an update is already in flight.
                val rendered =
                    probe.render(provider, context) {
                        DayChangeReceiver().onReceive(appContext, Intent(action))
                    }
                rendered.assertShowsAtLeastTexts(THREE_DATES_LINES)
            }
        }
    }

    /**
     * The device-level mirror of `UpgradeKeepsScheduledWorkTest`: on a real runtime, an upgrade or a reboot must
     * leave scheduled work alone.
     *
     * [aWidgetStillRendersAfterAnUpgradeOrRebootBroadcast] above does **not** catch the v1.3.0 bug — verified by
     * restoring the bug and watching it stay green. On an emulator Glance usually finishes inside the broadcast's
     * own window, so no `AsyncRequestWorker` job is pending to be cancelled, and the race never happens. This test
     * does catch it, because it does not depend on the race: it schedules a job standing in for Glance's and
     * checks the receiver did not take it away.
     */
    @Test
    fun anUpgradeDoesNotCancelScheduledWorkOnADevice() {
        val jobs = checkNotNull(appContext.getSystemService(JobScheduler::class.java))
        val component = ComponentName(appContext, SENTINEL_SERVICE)
        jobs.schedule(
            JobInfo
                .Builder(SENTINEL_JOB_ID, component)
                .setMinimumLatency(SENTINEL_LATENCY_MILLIS)
                .build(),
        )
        check(jobs.allPendingJobs.any { it.id == SENTINEL_JOB_ID }) { "the sentinel job was not scheduled" }

        DayChangeReceiver().onReceive(appContext, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        try {
            check(jobs.allPendingJobs.any { it.id == SENTINEL_JOB_ID }) {
                "an upgrade cancelled scheduled work: Glance draws widgets through WorkManager jobs, so this is " +
                    "the launcher spinner of v1.3.0"
            }
        } finally {
            jobs.cancel(SENTINEL_JOB_ID)
        }
    }

    private companion object {
        /** A real, declared service so JobScheduler accepts the sentinel; it is never actually run. */
        const val SENTINEL_SERVICE = "androidx.work.impl.background.systemjob.SystemJobService"
        const val SENTINEL_JOB_ID = 918_274
        const val SENTINEL_LATENCY_MILLIS = 600_000L

        /** The two broadcasts after which Android asks every placed widget to redraw. */
        val UPGRADE_BROADCASTS = listOf(Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_BOOT_COMPLETED)

        /** Not [DeviceSurfacesTest]'s or [DeviceEveryApiTest]'s host id: all three may run in one process. */
        const val HOST_ID = 0x7a53

        /** Persian, Gregorian and Islamic: the three lines T-1216 exists to show. */
        const val THREE_DATES_LINES = 3
    }
}
