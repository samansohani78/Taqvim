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
import ir.taqvim.feature.widgets.WidgetStateLoader
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

    /**
     * S17: what the user waits for, measured where they wait for it.
     *
     * The micro benchmark times `provideGlance` inside the test process and states in its own KDoc that
     * end-to-end time to a first useful render "cannot be measured from inside this process". This can: the probe
     * binds a real [android.appwidget.AppWidgetHost], and the figure runs from the update broadcast to readable
     * content on screen, host inflation included.
     *
     * The ceiling is derived from [WidgetStateLoader.LOAD_TIMEOUT] rather than fitted to what the hardware
     * happens to do. A widget may wait that long for one store and then draws regardless, so anything past it
     * plus [DELIVERY_ALLOWANCE_MILLIS] for composition, `RemoteViews` delivery and host inflation is not a slow
     * device, it is a widget that is not bounded at all — the v1.3.0 failure. It is deliberately not a tight
     * performance budget: the numbers are printed so a regression is visible in the log, and the assertion
     * catches only the class of bug this pass exists for.
     */
    @Test
    fun everyWidgetReachesContentWithinItsBoundedBudget() {
        val measured = mutableListOf<RenderedWidget>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val context = checkNotNull(activity)
            probe.providers().forEach { measured += probe.render(it, context) }
        }

        val report =
            measured
                .sortedBy { it.millisToContent ?: Long.MAX_VALUE }
                .joinToString(separator = "\n") { "  ${it.millisToContent} ms  ${it.provider}" }
        println("Host-visible time to content (±${WidgetHostProbe.POLL_RESOLUTION_MILLIS} ms):\n$report")

        val late = measured.filter { (it.millisToContent ?: Long.MAX_VALUE) > BUDGET_MILLIS }
        check(late.isEmpty()) {
            "These widgets took longer than the ${BUDGET_MILLIS} ms a bounded load can justify:\n" +
                late.joinToString(separator = "\n") { "  $it" }
        }
    }

    /**
     * S2/S11 at the device layer: a configuration change must reach a widget the host has already bound.
     *
     * The local suite proves the *content* follows the language — `ThreeDatesWidgetTest` asserts an English user
     * reads Latin digits. What it cannot prove is that a preference written by the app arrives at a widget drawn
     * by a separate host: that crosses a DataStore, a process boundary and Glance's session. So this changes the
     * language between two real renders and reads the digits back off the host's own view tree.
     *
     * The language is set through the stored preferences rather than `useLanguage`, because the widgets format
     * from [ir.taqvim.data.preferences.UserPreferences], not from the Android locale, and `LocaleManager` does not
     * exist on the API 26 leg.
     */
    @Test
    fun aLanguageChangeReachesAPlacedWidget() {
        val provider = probe.providers().first { it.provider.shortClassName.contains("ThreeDates") }
        val original = updatePreferences { it }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                var activity: Activity? = null
                scenario.onActivity { activity = it }
                val context = checkNotNull(activity)

                updatePreferences { it.withLanguage("en") }
                val english = probe.render(provider, context)
                english.assertRenderedFinalContent()
                check(english.texts.any { line -> line.any { it in LATIN_DIGITS } }) {
                    "an English user must read Latin digits on the widget. $english"
                }

                updatePreferences { it.withLanguage("fa") }
                val persian = probe.render(provider, context)
                persian.assertRenderedFinalContent()
                check(persian.texts.any { line -> line.any { it in PERSIAN_DIGITS } }) {
                    "the language change never reached the placed widget. $persian"
                }
            }
        } finally {
            updatePreferences { original }
        }
    }

    private companion object {
        /** Proof the widget formatted in the chosen language, read off the host's own view tree. */
        val LATIN_DIGITS = '0'..'9'
        val PERSIAN_DIGITS = '\u06F0'..'\u06F9'

        /**
         * Composition, `RemoteViews` delivery, host inflation and the poll granularity on top of the loader's own
         * bound. Generous on purpose: see [everyWidgetReachesContentWithinItsBoundedBudget].
         */
        const val DELIVERY_ALLOWANCE_MILLIS = 5_000L
        val BUDGET_MILLIS = WidgetStateLoader.LOAD_TIMEOUT.inWholeMilliseconds + DELIVERY_ALLOWANCE_MILLIS

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
