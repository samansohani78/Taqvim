/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1801 frame timing of the timeline and search screens, opened through their `taqvim://` links. The plan's jank
 * budget (under 1 % of frames over 16 ms) is read from the FrameTimingMetric results; nightly runs fail on a
 * regression of more than 10 % against the committed baseline (tools/benchmark/compare_benchmarks.py).
 */
@RunWith(AndroidJUnit4::class)
class ScreenScrollBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun timelineScroll(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            // WARM, like the other scroll journeys: with COLD the framework kills the app between the setup and the
            // measured block, so a journey that opens its screen in the setup would measure an empty screen.
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                openLink(Links.TIMELINE)
            },
        ) {
            val grid = withSafeGestureMargin(waitForTag(Tags.TIMELINE_GRID))
            repeat(FLINGS) {
                grid.fling(Direction.DOWN)
                device.waitForIdle()
                grid.fling(Direction.UP)
                device.waitForIdle()
            }
        }

    @Test
    fun searchTyping(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            // WARM, like the other scroll journeys: with COLD the framework kills the app between the setup and the
            // measured block, so a journey that opens its screen in the setup would measure an empty screen.
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                openLink(Links.SEARCH)
            },
        ) {
            val field = waitForTag(Tags.SEARCH_FIELD)
            QUERY.indices.forEach { index ->
                field.text = QUERY.substring(0, index + 1)
                device.waitForIdle()
            }
            withSafeGestureMargin(waitForTag(Tags.SEARCH_RESULTS)).fling(Direction.DOWN)
            device.waitForIdle()
        }

    private companion object {
        const val ITERATIONS = 5
        const val FLINGS = 3

        /** Nowruz, typed one letter at a time as a user would. */
        const val QUERY = "نوروز"
    }
}
