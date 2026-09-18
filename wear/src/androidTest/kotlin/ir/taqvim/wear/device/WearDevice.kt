/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
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
import java.io.File
import kotlin.math.abs
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
 * The node tagged [tag], failing when it does not appear. Watch lists are taller than the screen, so the list is
 * scrolled while looking for it.
 */
internal fun awaitTag(tag: String): UiObject2 {
    device.wait(Until.findObject(By.res(tag)), DEVICE_TIMEOUT_MILLIS)?.let { return it }
    repeat(SCROLL_ATTEMPTS) {
        val list = device.findObject(By.scrollable(true)) ?: return@repeat
        list.scroll(Direction.DOWN, SCROLL_FRACTION)
        device.waitForIdle(IDLE_MILLIS)
        device.findObject(By.res(tag))?.let { return it }
    }
    dumpHierarchy(tag)
    error("$tag is not shown; the watch app crashed or the screen did not open")
}

/**
 * Taps the node tagged [tag] after bringing it to the middle of the screen.
 *
 * The watch lists are a `TransformingLazyColumn`: items near the top and bottom edge are scaled down and moved, so a
 * tap aimed at the middle of a node's reported bounds can land outside the button that is actually drawn. Scrolling
 * the node towards the centre, where the list applies no transformation, and tapping its visible centre makes the tap
 * land on the control (T-1600).
 */
internal fun tapTag(tag: String) = tap(By.res(tag), tag)

/** Taps the node whose content description is [description], the same way as [tapTag]. */
internal fun tapDescription(description: String) = tap(By.desc(description), description)

/** Brings the node matching [selector] to the middle of the screen and taps its visible centre. */
private fun tap(
    selector: BySelector,
    name: String,
) {
    val centred = centre(selector, name)
    device.click(centred.centerX(), centred.centerY())
    device.waitForIdle(IDLE_MILLIS)
}

/** Scrolls the node matching [selector] into the middle band of the screen and returns its visible bounds there. */
private fun centre(
    selector: BySelector,
    name: String,
): Rect {
    var bounds = awaitVisible(selector, name).visibleBounds
    val middle = device.displayHeight / 2
    val band = device.displayHeight / CENTRE_BAND
    var attempts = 0
    var list = device.findObject(By.scrollable(true))
    while (attempts < CENTRE_ATTEMPTS && list != null && abs(bounds.centerY() - middle) > band) {
        attempts++
        list.scroll(if (bounds.centerY() > middle) Direction.DOWN else Direction.UP, CENTRE_FRACTION)
        device.waitForIdle(IDLE_MILLIS)
        bounds = device.findObject(selector)?.visibleBounds ?: bounds
        list = device.findObject(By.scrollable(true))
    }
    return bounds
}

/** The node matching [selector], failing with a hierarchy dump when it never appears. */
private fun awaitVisible(
    selector: BySelector,
    name: String,
): UiObject2 =
    device.wait(Until.findObject(selector), DEVICE_TIMEOUT_MILLIS) ?: run {
        dumpHierarchy(name)
        error("$name is not shown; the watch app crashed or the screen did not open")
    }

/** Writes the window hierarchy next to the test output, so a failure on CI names the nodes that were on screen. */
internal fun dumpHierarchy(tag: String) {
    runCatching {
        val directory =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: appContext.getExternalFilesDir(null)
        directory?.mkdirs()
        device.dumpWindowHierarchy(File(directory, "hierarchy-${tag.replace(':', '-')}.xml"))
    }
}

/**
 * Taps the button tagged [tag] until the screen of [route] is shown. A tap on a watch list can be swallowed while the
 * list is still settling, so the tap is repeated before the test gives up.
 */
internal fun openScreen(
    tag: String,
    route: String,
) {
    repeat(OPEN_ATTEMPTS) { attempt ->
        tapTag(tag)
        if (device.wait(Until.hasObject(By.res(screenTag(route))), OPEN_TIMEOUT_MILLIS)) {
            awaitScreen(route)
            return
        }
        if (attempt == OPEN_ATTEMPTS - 1) dumpHierarchy("open-$route")
    }
    error("${screenTag(route)} did not open after $OPEN_ATTEMPTS taps on $tag")
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

/** How often a tap is repeated when the screen does not open, and how long each attempt waits for it. */
private const val OPEN_ATTEMPTS = 3
private const val OPEN_TIMEOUT_MILLIS = 5_000L

/** Centring a node: how many scrolls, how much of the list each one moves, and the band counted as the middle. */
private const val CENTRE_ATTEMPTS = 4
private const val CENTRE_FRACTION = 0.2f
private const val CENTRE_BAND = 6
