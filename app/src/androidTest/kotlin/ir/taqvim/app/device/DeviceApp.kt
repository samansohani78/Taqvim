/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/** How long device tests wait for a screen, a preference write or a system surface. */
internal const val DEVICE_TIMEOUT_MILLIS: Long = 20_000

/** The app under test. */
internal val appContext: Context
    get() = ApplicationProvider.getApplicationContext()

/** The device, driven through UiAutomator: it never waits for Compose to be idle, so animated screens do not stall. */
internal val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

/** The running app's preferences store (Koin is started by `TaqvimApplication`). */
internal val preferences: UserPreferencesRepository
    get() = GlobalContext.get().get()

/** Applies [change] to the stored preferences and returns the result. */
internal fun updatePreferences(change: (UserPreferences) -> UserPreferences): UserPreferences =
    runBlocking {
        preferences.update(change)
        preferences.preferences.first()
    }

/**
 * Makes [language] the app language with the first-run onboarding done, as a returning user's device has it, and waits
 * until Android 13+ shows Taqvim in that language (ADR-0023).
 */
internal fun useLanguage(language: String) {
    keepScreenOn()
    updatePreferences { it.withLanguage(language).copy(onboardingCompleted = true) }
    val manager = appContext.getSystemService(LocaleManager::class.java)
    waitFor("app language $language") {
        manager.applicationLocales.toLanguageTags().startsWith(language)
    }
}

/** Keeps the device awake, unlocked and in portrait, so the app stays in front while the tests drive it. */
internal fun keepScreenOn() {
    shell("svc power stayon true")
    shell("input keyevent KEYCODE_WAKEUP")
    shell("wm dismiss-keyguard")
    device.setOrientationNatural()
}

/** Runs [command] as the shell user and returns its output. */
internal fun shell(command: String): String {
    val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
}

/** Polls [condition] until it holds, failing with [what] after [timeoutMillis]. */
internal fun waitFor(
    what: String,
    timeoutMillis: Long = DEVICE_TIMEOUT_MILLIS,
    condition: () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "Timed out waiting for $what" }
        Thread.sleep(POLL_MILLIS)
    }
}

/** Opens Taqvim on the `taqvim://` [link] (ADR-0016) in a new task and waits for the screen tagged [tag]. */
internal fun openLink(
    link: String,
    tag: String,
) {
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(link))
            .setPackage(appContext.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    appContext.startActivity(intent)
    awaitTag(tag)
}

/**
 * The node tagged [tag] (test tags are resource ids in `MainActivity`), failing when it does not appear. Lists longer
 * than the screen, such as More on a short device, are scrolled down while looking for it.
 */
internal fun awaitTag(tag: String): UiObject2 {
    device.wait(Until.findObject(By.res(tag)), DEVICE_TIMEOUT_MILLIS)?.let { return it }
    repeat(SCROLL_ATTEMPTS) {
        val list = device.findObject(By.scrollable(true)) ?: return@repeat
        list.scroll(Direction.DOWN, SCROLL_FRACTION)
        device.waitForIdle(IDLE_MILLIS)
        device.findObject(By.res(tag))?.let { return it }
    }
    error("$tag is not shown; the app crashed or the screen did not open")
}

/** Selects every tab of the current screen once (not the app's navigation tabs), composing each tab's content. */
internal fun visitTabs() {
    val count = screenTabs().size
    repeat(count) { index ->
        clickRetryingStale { screenTabs().getOrNull(index) }
        device.waitForIdle(IDLE_MILLIS)
    }
}

/**
 * Clicks the object [find] returns, retrying with a freshly found object when the accessibility tree changed under it
 * (`StaleObjectException`) between being listed and being clicked, e.g. mid recomposition (seen once in CI run
 * 35854629904, `DeviceSmokeTest#everyTabAndMoreEntryOpens[fa]` on API 33 only — API 36 in the same run was clean, so
 * this is a timing race rather than a deterministic failure). Re-querying [find] rather than retrying the same click
 * matters: the stale object's node is gone for good, but the tab it represents is still there to find again.
 */
private fun clickRetryingStale(find: () -> UiObject2?) {
    repeat(STALE_CLICK_ATTEMPTS) { attempt ->
        val target = find() ?: return
        try {
            target.click()
            return
        } catch (stale: StaleObjectException) {
            if (attempt == STALE_CLICK_ATTEMPTS - 1) throw stale
            device.waitForIdle(IDLE_MILLIS)
        }
    }
}

private fun screenTabs(): List<UiObject2> = device.findObjects(By.res(SEGMENT_TAG))

/** Turns the device to landscape and back, checking after each turn that the screen tagged [tag] is shown again. */
internal fun rotate(tag: String) {
    device.setOrientationLandscape()
    awaitTag(tag)
    device.setOrientationNatural()
    awaitTag(tag)
}

/** Presses back and waits for the screen tagged [tag]. */
internal fun back(tag: String) {
    device.pressBack()
    awaitTag(tag)
}

/** Fails when Taqvim is no longer the app in front (e.g. it crashed). */
internal fun assertInFront() {
    check(device.currentPackageName == appContext.packageName) {
        "Taqvim is not in front (${device.currentPackageName}); it probably crashed"
    }
}

private const val POLL_MILLIS = 200L
private const val IDLE_MILLIS = 1_000L

/** How often a list is scrolled while looking for a tag, and how much of its height each scroll moves. */
private const val SCROLL_ATTEMPTS = 4
private const val SCROLL_FRACTION = 0.8f

/** How many times [clickRetryingStale] re-finds its target after a `StaleObjectException`. */
private const val STALE_CLICK_ATTEMPTS = 3

/** `segmentTag` of `:core:ui`: the tabs of `SegmentedTabs`, used by the screens with tabs. */
private val SEGMENT_TAG = Regex("""segment:\d+""").toPattern()
