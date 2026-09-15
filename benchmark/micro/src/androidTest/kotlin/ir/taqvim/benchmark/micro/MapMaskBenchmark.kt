/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark.micro

import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.feature.map.LayerGrids
import ir.taqvim.feature.map.SubPoints
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T-1801 world map mask timings (plan §9: map mask < 150 ms off-main, budgets in `benchmark/budgets.json`). The
 * instrumentation thread is not the main thread, like the map ViewModel's background dispatcher. The crescent grid runs
 * Yallop's criterion per cell and has no plan budget, so it is recorded for regressions only.
 */
@RunWith(AndroidJUnit4::class)
class MapMaskBenchmark {
    private val sun = SubPoints.sun(BenchmarkModels.EQUINOX)
    private val moon = SubPoints.moon(BenchmarkModels.EQUINOX)

    /** Day, twilight and night at the map screen's 2° resolution. */
    @Test
    fun dayNightMask() {
        TimingReport.record(CLASS_NAME, "dayNightMask", Timing.measure { LayerGrids.illumination(sun) })
    }

    @Test
    fun moonVisibilityMask() {
        TimingReport.record(CLASS_NAME, "moonVisibilityMask", Timing.measure { LayerGrids.moonVisibility(moon) })
    }

    @Test
    fun crescentMask() {
        val timing = Timing.measure(CRESCENT_WARMUP, CRESCENT_RUNS) { LayerGrids.crescent(BenchmarkModels.EQUINOX) }
        TimingReport.record(CLASS_NAME, "crescentMask", timing)
    }

    private companion object {
        val CLASS_NAME: String = MapMaskBenchmark::class.java.name
        const val CRESCENT_WARMUP = 2
        const val CRESCENT_RUNS = 10
    }
}
