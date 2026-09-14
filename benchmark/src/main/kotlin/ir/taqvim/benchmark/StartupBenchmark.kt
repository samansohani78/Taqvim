/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1800/T-1801 startup: plan §9 budgets are ≤ 350 ms to the first month frame on a Pixel 6a-class device and
 * ≤ 800 ms on a low-end API 26 device. Emulator runs (benchmark.yml) are only compared with earlier runs, never with
 * the budget. The run without ahead-of-time compilation shows what the baseline profile saves.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCold(): Unit = startup(StartupMode.COLD, CompilationMode.DEFAULT)

    @Test
    fun startupWarm(): Unit = startup(StartupMode.WARM, CompilationMode.DEFAULT)

    @Test
    fun startupColdWithoutProfile(): Unit = startup(StartupMode.COLD, CompilationMode.None())

    private fun startup(
        mode: StartupMode,
        compilation: CompilationMode,
    ) {
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            iterations = ITERATIONS,
            startupMode = mode,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
