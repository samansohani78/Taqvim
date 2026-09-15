/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.benchmark.micro

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wall-clock timings of one operation: [warmup] untimed runs, then [runs] timed ones (ADR-0018 addendum). An interim
 * harness while Jetpack Microbenchmark cannot be downloaded; it has no CPU pinning or thermal checks, so only its
 * median is compared, never a single run.
 */
internal data class Timing(
    val runsNs: List<Long>,
    val warmup: Int,
) {
    val medianNs: Long get() = runsNs.sorted()[runsNs.size / 2]

    companion object {
        const val DEFAULT_WARMUP: Int = 10
        const val DEFAULT_RUNS: Int = 30

        fun measure(
            warmup: Int = DEFAULT_WARMUP,
            runs: Int = DEFAULT_RUNS,
            block: () -> Unit,
        ): Timing {
            require(warmup >= 0 && runs > 0) { "Timing needs at least one run" }
            repeat(warmup) { block() }
            val results =
                List(runs) {
                    val start = System.nanoTime()
                    block()
                    System.nanoTime() - start
                }
            return Timing(results, warmup)
        }
    }
}

/** Writes [Timing]s as AndroidX Benchmark JSON (`*benchmarkData.json`) into the connected test's additional output. */
internal object TimingReport {
    private const val OUTPUT_ARGUMENT = "additionalTestOutputDir"

    /** Records [timing] of [className].[name]; one file per benchmark so parallel classes never overwrite each other. */
    fun record(
        className: String,
        name: String,
        timing: Timing,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory =
            InstrumentationRegistry.getArguments().getString(OUTPUT_ARGUMENT)?.let(::File)
                ?: instrumentation.targetContext.filesDir
        directory.mkdirs()
        File(directory, "$className.$name-benchmarkData.json").writeText(json(className, name, timing).toString())
    }

    private fun json(
        className: String,
        name: String,
        timing: Timing,
    ): JSONObject {
        val runs = timing.runsNs
        val time =
            JSONObject()
                .put("minimum", runs.min())
                .put("maximum", runs.max())
                .put("median", timing.medianNs)
                .put("runs", JSONArray(runs))
        val benchmark =
            JSONObject()
                .put("name", name)
                .put("className", className)
                .put("totalRunTimeNs", runs.sum())
                .put("metrics", JSONObject().put("timeNs", time))
                .put("warmupIterations", timing.warmup)
                .put("repeatIterations", runs.size)
        val build = JSONObject().put("model", Build.MODEL).put("sdk", Build.VERSION.SDK_INT)
        return JSONObject()
            .put("context", JSONObject().put("build", build))
            .put("benchmarks", JSONArray().put(benchmark))
    }
}
