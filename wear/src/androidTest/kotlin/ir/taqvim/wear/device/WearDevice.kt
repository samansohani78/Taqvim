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
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
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
import java.util.regex.Pattern
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
 * The selector of the node whose test tag is exactly [tag].
 *
 * `By.res(String)` matches a resource name anywhere in the id, and the watch tags nest (`wear:settings` is the start
 * of `wear:settings:language`), so an unanchored selector reports the settings screen as shown while a choice list
 * is in front — and the test then drives the wrong screen. The pattern is anchored to prevent that.
 */
internal fun byTag(tag: String): BySelector = By.res(Pattern.compile("^" + Pattern.quote(tag) + "$"))

/** The node tagged [tag], scrolling the screen while looking for it and failing when it never appears. */
internal fun awaitTag(tag: String): UiObject2 = awaitVisible(byTag(tag), tag)

/**
 * Taps the node tagged [tag] after bringing it to the middle of the screen.
 *
 * The watch lists are a `TransformingLazyColumn`: items near the top and bottom edge are scaled down and moved, so a
 * tap aimed at the middle of a node's reported bounds can land outside the button that is actually drawn. Scrolling
 * the node towards the centre, where the list applies no transformation, and tapping its visible centre makes the tap
 * land on the control (T-1600).
 */
internal fun tapTag(tag: String) = tap(byTag(tag), tag)

/** Taps the node whose content description is [description], the same way as [tapTag]. */
internal fun tapDescription(description: String) = tap(By.desc(description), description)

/**
 * Taps the node matching [selector], after scrolling it into view.
 *
 * The tap is the accessibility click of the node itself, not a gesture at its coordinates: the watch lists morph the
 * items near the top and bottom edge, so the button drawn there does not cover the middle of its reported bounds and
 * a tap at those coordinates is lost. A coordinate tap on the centred node remains as a fallback.
 */
private fun tap(
    selector: BySelector,
    name: String,
) {
    awaitVisible(selector, name)
    if (!clickNode(selector, name)) {
        val centred = centre(selector, name)
        device.click(centred.centerX(), centred.centerY())
    }
    device.waitForIdle(IDLE_MILLIS)
}

/** Performs the accessibility click of the node matching [selector], or of its nearest clickable parent. */
private fun clickNode(
    selector: BySelector,
    name: String,
): Boolean {
    val byDescription = selector.toString().contains("DESC")
    val matches: (AccessibilityNodeInfo) -> Boolean = { node ->
        if (byDescription) node.contentDescription?.toString() == name else node.viewIdResourceName == name
    }
    val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
    val node = root?.let { findNode(it, matches) }?.let(::clickable)
    return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
}

/**
 * [node] itself when it takes clicks, otherwise its nearest parent that does.
 *
 * A Wear Material button whose content description sits on the modifier reports `clickable=false` while still
 * offering the click action, so the action list is consulted as well as the flag.
 */
private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var candidate: AccessibilityNodeInfo? = node
    while (candidate != null && !candidate.takesClicks()) candidate = candidate.parent
    return candidate
}

private fun AccessibilityNodeInfo.takesClicks(): Boolean =
    isClickable || actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

/** The first node of [node]'s tree for which [matches] holds. */
private fun findNode(
    node: AccessibilityNodeInfo,
    matches: (AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo? {
    if (matches(node)) return node
    for (index in 0 until node.childCount) {
        val child = node.getChild(index) ?: continue
        findNode(child, matches)?.let { return it }
    }
    return null
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
    while (attempts < CENTRE_ATTEMPTS && abs(bounds.centerY() - middle) > band) {
        attempts++
        scrollScreen(down = bounds.centerY() > middle)
        bounds = device.findObject(selector)?.visibleBounds ?: bounds
    }
    return bounds
}

/**
 * The node matching [selector], scrolled into view if needed, failing with a hierarchy dump when it never appears.
 *
 * A watch list composes only the items on the screen, and `TransformingLazyColumn` publishes no scrollable node to
 * the accessibility tree (its dumps report `scrollable=false` for every node), so the list cannot be scrolled through
 * `UiObject2.scroll`. The screen is swiped instead, first down to the end of the list and then back up, so a node
 * above the starting position is found as well.
 */
private fun awaitVisible(
    selector: BySelector,
    name: String,
): UiObject2 {
    device.wait(Until.findObject(selector), DEVICE_TIMEOUT_MILLIS)?.let { return it }
    repeat(SCROLL_ATTEMPTS) {
        scrollScreen(down = true)
        device.findObject(selector)?.let { return it }
    }
    repeat(SCROLL_ATTEMPTS * 2) {
        scrollScreen(down = false)
        device.findObject(selector)?.let { return it }
    }
    dumpHierarchy(name)
    error("$name is not shown; the watch app crashed or the screen did not open")
}

/**
 * Swipes the watch screen vertically, which scrolls the list on it.
 *
 * The swipe stays inside the middle band of the screen: a vertical swipe that starts near the top or the bottom edge
 * of a watch pulls down the system's quick settings or its notification stream instead, which covers the app. The
 * watch app is never sent a key event to recover from that — `pressBack` on the first screen leaves the app for the
 * watch face, which is worse than a covered screen that the next swipe uncovers.
 */
internal fun scrollScreen(down: Boolean) {
    val x = device.displayWidth / 2
    val near = device.displayHeight * SCROLL_NEAR / SCROLL_SCALE
    val far = device.displayHeight * SCROLL_FAR / SCROLL_SCALE
    if (down) device.swipe(x, near, x, far, SWIPE_STEPS) else device.swipe(x, far, x, near, SWIPE_STEPS)
    device.waitForIdle(IDLE_MILLIS)
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
        if (device.wait(Until.hasObject(byTag(screenTag(route))), OPEN_TIMEOUT_MILLIS)) {
            awaitScreen(route)
            return
        }
        if (attempt == OPEN_ATTEMPTS - 1) dumpHierarchy("open-$route")
    }
    error("${screenTag(route)} did not open after $OPEN_ATTEMPTS taps on $tag")
}

/**
 * Swipes right from the left edge (Wear's swipe to dismiss) and waits until [from] is gone and [to] is shown.
 *
 * `SwipeDismissableNavHost` keeps both destinations in the tree while the dismiss animates, so waiting only for the
 * target would return while the dismissed screen is still on top — and the next gesture would then cancel the
 * dismiss and leave the test on the wrong screen.
 */
internal fun swipeBack(
    from: String,
    to: String,
) {
    repeat(DISMISS_ATTEMPTS) { attempt ->
        device.swipe(1, device.displayHeight / 2, device.displayWidth - 1, device.displayHeight / 2, SWIPE_STEPS)
        device.wait(Until.gone(byTag(screenTag(from))), DEVICE_TIMEOUT_MILLIS)
        device.waitForIdle(IDLE_MILLIS)
        // The pop counts only once the dismissed screen stays gone: a swipe that does not pass the dismiss threshold
        // animates back, and the screen underneath is composed throughout, so a single check can pass while the
        // dismissed screen is on its way back and the test would then drive it.
        val cameBack = device.wait(Until.hasObject(byTag(screenTag(from))), SETTLE_MILLIS)
        if (cameBack != true && device.hasObject(byTag(screenTag(to)))) {
            awaitScreen(to)
            return
        }
        if (attempt == DISMISS_ATTEMPTS - 1) dumpHierarchy("dismiss-$from")
    }
    error("${screenTag(from)} was not dismissed to ${screenTag(to)} after $DISMISS_ATTEMPTS swipes")
}

/**
 * Goes back from [from] to [to] with the system's back action, which on a watch dismisses the screen.
 *
 * Unlike the swipe, the action is atomic: a swipe that does not pass the dismiss threshold animates back, and a test
 * that checked while it animated would go on driving the screen it thought it had left. Swipe to dismiss itself is
 * covered by `todayOpensTheOtherScreensAndSwipingGoesBack`.
 */
internal fun goBack(
    from: String,
    to: String,
) {
    repeat(DISMISS_ATTEMPTS) {
        device.pressBack()
        device.wait(Until.gone(byTag(screenTag(from))), DEVICE_TIMEOUT_MILLIS)
        device.waitForIdle(IDLE_MILLIS)
        val cameBack = device.wait(Until.hasObject(byTag(screenTag(from))), SETTLE_MILLIS)
        if (cameBack != true && device.hasObject(byTag(screenTag(to)))) {
            awaitScreen(to)
            return
        }
    }
    dumpHierarchy("back-$from")
    error("${screenTag(from)} did not go back to ${screenTag(to)}")
}

/** Fails when the watch app is no longer in front (e.g. it crashed). */
internal fun assertInFront() {
    check(device.currentPackageName == appContext.packageName) {
        "The watch app is not in front (${device.currentPackageName}); it probably crashed"
    }
}

private const val IDLE_MILLIS = 1_000L
private const val SWIPE_STEPS = 20

/** How often the swipe to dismiss is repeated when the screen underneath does not stay in front. */
private const val DISMISS_ATTEMPTS = 3

/** How long the dismissed screen must stay gone before the pop counts as committed. */
private const val SETTLE_MILLIS = 1_500L

/** How often the screen is swiped while looking for a node, and the swipe's start and end as parts of the height. */
private const val SCROLL_ATTEMPTS = 6
private const val SCROLL_NEAR = 62
private const val SCROLL_FAR = 38
private const val SCROLL_SCALE = 100

/** How often a tap is repeated when the screen does not open, and how long each attempt waits for it. */
private const val OPEN_ATTEMPTS = 3
private const val OPEN_TIMEOUT_MILLIS = 5_000L

/** Centring a node: how many swipes it may take, and the band around the middle that counts as centred. */
private const val CENTRE_ATTEMPTS = 4
private const val CENTRE_BAND = 6
