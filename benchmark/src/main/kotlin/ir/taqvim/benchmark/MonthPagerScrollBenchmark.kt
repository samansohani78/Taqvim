/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-801: frame timing while the month pager is swiped through 24 months. The plan's budget is under 1 % janky frames,
 * read from the FrameTimingMetric results of the nightly benchmark job. Needs the calendar screen as the app's start
 * destination with test tags exposed as resource ids.
 */
@RunWith(AndroidJUnit4::class)
class MonthPagerScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollTwentyFourMonths(): Unit =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                skipOnboarding()
            },
        ) {
            val pager =
                checkNotNull(device.wait(Until.findObject(By.res(PAGER_RESOURCE_ID)), TIMEOUT_MILLIS)) {
                    "The month pager is not shown"
                }
            pager.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
            repeat(MONTHS) {
                pager.swipe(Direction.LEFT, 1f)
                device.waitForIdle()
            }
        }

    private companion object {
        const val TARGET_PACKAGE = "ir.taqvim.app"

        /** `MONTH_PAGER_TAG` of `:feature:calendar`. */
        const val PAGER_RESOURCE_ID = "month_pager"
        const val ITERATIONS = 5
        const val MONTHS = 24
        const val TIMEOUT_MILLIS = 5_000L
        const val GESTURE_MARGIN_DIVISOR = 5
    }
}
