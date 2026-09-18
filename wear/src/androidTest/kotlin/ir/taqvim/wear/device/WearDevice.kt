/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.wear.WearGraph
import ir.taqvim.wear.WearGraphOwner
import ir.taqvim.wear.WearRoutes
import ir.taqvim.wear.WearSettingsModel
import ir.taqvim.wear.screenTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue

/** How long a watch test waits for a screen or a preference write. */
internal const val DEVICE_TIMEOUT_MILLIS: Long = 20_000

/** The watch app under test. */
internal val appContext: Context
    get() = ApplicationProvider.getApplicationContext()

/** The watch, driven through UiAutomator: it never waits for Compose to be idle, so animated screens do not stall. */
internal val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

/** The running watch app's object graph (`TaqvimWearApplication`). */
internal val graph: WearGraph
    get() = requireNotNull(appContext as? WearGraphOwner) { "the application must provide a WearGraph" }.graph

/**
 * Skips the test unless this device is a watch. The repository's connected-test sweep (`instrumented.yml`) runs every
 * module's device tests on a phone emulator, where a watch app's screens, tiles and complications mean nothing.
 */
internal fun assumeWatch() {
    assumeTrue(
        "not a watch; run the watch tests with ./gradlew :wear:wearApi34DebugAndroidTest",
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH),
    )
}

/** Applies [change] to the stored watch preferences and returns the result. */
internal fun updatePreferences(change: (UserPreferences) -> UserPreferences): UserPreferences =
    runBlocking {
        graph.preferences.update(change)
        graph.preferences.preferences.first()
    }

/**
 * Makes [language] the watch language with the most populous catalog city as the place, so the date lines and the
 * next prayer both have content (the watch is standalone: it has no phone to ask, ADR-0019).
 */
internal fun useLanguage(language: String) {
    keepScreenOn()
    val city =
        requireNotNull(
            graph.catalog.cities
                .filter { it.timeZoneId != null }
                .maxByOrNull { it.population ?: 0L },
        ) { "the bundled city catalog has no city with a time zone" }
    updatePreferences { WearSettingsModel.withCity(UserPreferences.defaultsFor(language), city) }
}

/** Keeps the watch awake and unlocked, so the app stays in front while the tests drive it. */
internal fun keepScreenOn() {
    shell("svc power stayon true")
    shell("input keyevent KEYCODE_WAKEUP")
    shell("wm dismiss-keyguard")
}

/** Runs [command] as the shell user and returns its output. */
internal fun shell(command: String): String {
    val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
}

/** Starts the watch app from its launcher entry in a new task and waits for today. */
internal fun launchWatchApp() {
    val intent =
        requireNotNull(appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)) {
            "the watch app has no launcher activity"
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    appContext.startActivity(intent)
    awaitScreen(WearRoutes.TODAY)
}

/** The screen of [route] (`screenTag`), failing when it does not appear. */
internal fun awaitScreen(route: String): UiObject2 = awaitTag(screenTag(route))

/**
 * The node tagged [tag] (test tags are resource ids in `WearActivity`), failing when it does not appear. Watch lists
 * are taller than the screen, so a list is scrolled down while looking for it.
 */
internal fun awaitTag(tag: String): UiObject2 {
    device.wait(Until.findObject(By.res(tag)), DEVICE_TIMEOUT_MILLIS)?.let { return it }
    repeat(SCROLL_ATTEMPTS) {
        val list = device.findObject(By.scrollable(true)) ?: return@repeat
        list.scroll(Direction.DOWN, SCROLL_FRACTION)
        device.waitForIdle(IDLE_MILLIS)
        device.findObject(By.res(tag))?.let { return it }
    }
    error("$tag is not shown; the watch app crashed or the screen did not open")
}

/** Taps the button tagged [tag] and waits for the screen of [route]. */
internal fun openScreen(
    tag: String,
    route: String,
) {
    awaitTag(tag).click()
    awaitScreen(route)
}

/** Swipes right from the left edge (Wear's swipe to dismiss) and waits for the screen of [route]. */
internal fun swipeBack(route: String) {
    device.swipe(1, device.displayHeight / 2, device.displayWidth - 1, device.displayHeight / 2, SWIPE_STEPS)
    device.waitForIdle(IDLE_MILLIS)
    awaitScreen(route)
}

/** Fails when the watch app is no longer in front (e.g. it crashed). */
internal fun assertInFront() {
    check(device.currentPackageName == appContext.packageName) {
        "The watch app is not in front (${device.currentPackageName}); it probably crashed"
    }
}

private const val IDLE_MILLIS = 1_000L
private const val SWIPE_STEPS = 20

/** How often a list is scrolled while looking for a tag, and how much of its height each scroll moves. */
private const val SCROLL_ATTEMPTS = 4
private const val SCROLL_FRACTION = 0.8f
