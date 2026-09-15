/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1800 baseline and startup profile generator (ADR-0018): cold start → calendar → month swipes → day details →
 * times → search. Needs an API 33+ device or emulator (or a rooted one); the profiles it writes are copied to
 * `app/src/main/baseline-prof.txt` by a person after review. Not part of the nightly benchmark
 * run.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate(): Unit =
        rule.collect(packageName = APP_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            skipOnboarding()
            swipeMonths()
            restartOn(Links.NOWRUZ_1405)
            restartOn(Links.TIMES)
            restartOn(Links.SEARCH)
            waitForTag(Tags.SEARCH_FIELD).text = SEARCH_QUERY
            device.waitForIdle()
        }

    private fun MacrobenchmarkScope.swipeMonths() {
        val pager = withSafeGestureMargin(waitForTag(Tags.MONTH_PAGER))
        repeat(MONTH_SWIPES) {
            pager.swipe(Direction.LEFT, 1f)
            device.waitForIdle()
        }
    }

    /** A fresh start on [link], so each screen's own startup path is recorded. */
    private fun MacrobenchmarkScope.restartOn(link: String) {
        killProcess()
        openLink(link)
        device.waitForIdle()
    }

    private companion object {
        const val MONTH_SWIPES = 3
        const val SEARCH_QUERY = "نوروز"
    }
}
