/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1803 heap budget: plan §9 allows the month screen less than 80 MB RSS. The run opens Nowruz 1405 and pages a year
 * of months; `tools/benchmark/compare_benchmarks.py --budgets benchmark/budgets.json` fails when the peak anonymous
 * plus file-backed RSS reaches the budget. Needs a device or emulator (benchmark.yml).
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class MonthScreenMemoryBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun monthScreenMemory(): Unit =
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(MemoryUsageMetric(MemoryUsageMetric.Mode.Max, SUB_METRICS)),
            compilationMode = CompilationMode.DEFAULT,
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
        ) {
            pressHome()
            openLink(Links.NOWRUZ_1405)
            val pager = withSafeGestureMargin(waitForTag(Tags.MONTH_PAGER))
            repeat(MONTHS) { pager.swipe(Direction.LEFT, SWIPE_PERCENT) }
            device.waitForIdle()
        }

    private companion object {
        const val ITERATIONS = 5
        const val MONTHS = 12
        const val SWIPE_PERCENT = 0.8f
        val SUB_METRICS =
            listOf(
                MemoryUsageMetric.SubMetric.RssAnon,
                MemoryUsageMetric.SubMetric.RssFile,
                MemoryUsageMetric.SubMetric.HeapSize,
            )
    }
}
