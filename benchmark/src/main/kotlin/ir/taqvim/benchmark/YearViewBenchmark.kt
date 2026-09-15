/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-805 frame timing of the year view. There is no `taqvim://` link for it (ADR-0016), so the journeys open it from the
 * More tab as a user does. Measured: opening it until the mini months show, paging year by year with the next and
 * previous buttons, and switching calendars by swipe and scrolling the grid. The plan's jank budget (under 1 % of
 * frames over 16 ms) is read from the FrameTimingMetric results; nightly runs fail on a regression of more than 10 %
 * against the committed baseline (tools/benchmark/compare_benchmarks.py).
 */
@RunWith(AndroidJUnit4::class)
class YearViewBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun openYearView(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                skipOnboarding()
            },
        ) {
            openYearView()
        }

    @Test
    fun pageThroughYears(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                skipOnboarding()
                openYearView()
            },
        ) {
            repeat(YEARS) {
                waitForTag(Tags.YEAR_NEXT).click()
                device.waitForIdle()
            }
            repeat(YEARS) {
                waitForTag(Tags.YEAR_PREVIOUS).click()
                device.waitForIdle()
            }
        }

    @Test
    fun swipeCalendarsAndScrollMonths(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                skipOnboarding()
                openYearView()
            },
        ) {
            val pager = withSafeGestureMargin(waitForTag(Tags.YEAR_PAGER))
            repeat(SWIPES) {
                withSafeGestureMargin(waitForTag(Tags.YEAR_GRID)).fling(Direction.DOWN)
                device.waitForIdle()
                // Swipes towards the next calendar in either layout direction, then back.
                pager.swipe(Direction.LEFT, 1f)
                device.waitForIdle()
                pager.swipe(Direction.RIGHT, 1f)
                device.waitForIdle()
            }
        }

    /** Opens the year view from the More tab and waits for its grid of mini months. */
    private fun MacrobenchmarkScope.openYearView() {
        waitForTag(Tags.MORE_TAB).click()
        waitForTag(Tags.MORE_YEAR).click()
        waitForTag(Tags.YEAR_GRID)
        device.waitForIdle()
    }

    private companion object {
        const val ITERATIONS = 5
        const val YEARS = 5
        const val SWIPES = 3
    }
}
