/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Accessibility Test Framework checks (T-1700) on every top-level screen and More entry of the running app, in a
 * right-to-left and a left-to-right language. Any ATF error fails the test with the screen and the message.
 */
@RunWith(Parameterized::class)
class DeviceAccessibilityTest(
    private val language: String,
) {
    private val validator = AccessibilityValidator().setRunChecksFromRootView(true)

    @Test
    fun screensHaveNoAccessibilityErrors() {
        useLanguage(language)
        val errors = mutableListOf<String>()
        openLink("taqvim://calendar", "destination:Calendar")
        errors += errors("Calendar")
        listOf("TIMES", "TOOLS").forEach { tab ->
            awaitTag("tab:$tab").click()
            device.waitForIdle()
            errors += errors(tab)
        }
        SCREENS.forEach { entry ->
            awaitTag("tab:MORE").click()
            awaitTag("more:$entry")
            errors += errors("More")
            awaitTag("more:$entry").click()
            device.waitForIdle()
            errors += errors(entry)
            back("destination:More")
        }
        check(errors.isEmpty()) { errors.distinct().joinToString(separator = "\n") }
    }

    /** ATF errors of the resumed activity's window, labelled with [screen]. */
    private fun errors(screen: String): List<String> {
        var found = emptyList<String>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity: Activity? =
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()
            found =
                activity
                    ?.let { validator.checkAndReturnResults(it.window.decorView) }
                    .orEmpty()
                    .filter { it.type == AccessibilityCheckResultType.ERROR }
                    .map { "$language $screen: ${it.getMessage(Locale.ENGLISH)}" }
        }
        return found
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages(): List<String> = listOf("fa", "en")

        private val SCREENS =
            listOf(
                "YEAR",
                "AGENDA",
                "SEARCH",
                "ASTRONOMY",
                "MAP",
                "COMPASS",
                "LEVEL",
                "SETTINGS",
                "BACKUP",
                "PRIVACY",
                "ABOUT",
            )
    }
}
