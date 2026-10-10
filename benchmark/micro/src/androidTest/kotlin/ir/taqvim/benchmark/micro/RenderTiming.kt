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

        /**
         * [a] and [b] measured alternately in one run, with the order of each pair swapped every iteration.
         *
         * Two separately measured benchmarks cannot be subtracted from one another and called a cost: they ran at
         * different times, at different thermal states, and on this harness the run-to-run spread is wider than
         * the difference being claimed. Interleaving puts both sides of the comparison in the same conditions, and
         * swapping the order within each pair keeps whichever runs first from carrying the cost of waking
         * something up.
         *
         * Returns the two timings and the per-pair signed difference `b - a`, whose own spread is the thing to
         * read: a difference that is stable across pairs is evidence that the fixed cost really is additive, and
         * one that is not says the subtraction means nothing.
         */
        fun measurePaired(
            warmup: Int = DEFAULT_WARMUP,
            runs: Int = DEFAULT_RUNS,
            a: () -> Unit,
            b: () -> Unit,
        ): PairedTiming {
            require(warmup >= 0 && runs > 0) { "Timing needs at least one run" }
            repeat(warmup) {
                a()
                b()
            }
            val aRuns = ArrayList<Long>(runs)
            val bRuns = ArrayList<Long>(runs)
            repeat(runs) { index ->
                if (index % 2 == 0) {
                    aRuns += time(a)
                    bRuns += time(b)
                } else {
                    bRuns += time(b)
                    aRuns += time(a)
                }
            }
            return PairedTiming(Timing(aRuns, warmup), Timing(bRuns, warmup), warmup)
        }

        private fun time(block: () -> Unit): Long {
            val start = System.nanoTime()
            block()
            return System.nanoTime() - start
        }
    }
}

/** Two operations measured alternately, with the per-pair signed difference `b - a`. */
internal data class PairedTiming(
    val a: Timing,
    val b: Timing,
    val warmup: Int,
) {
    /** `b - a` for each pair, in the order measured; its spread says whether the difference is a real quantity. */
    val difference: Timing
        get() = Timing(a.runsNs.indices.map { b.runsNs[it] - a.runsNs[it] }, warmup)
}

/** Writes [Timing]s as AndroidX Benchmark JSON (`*benchmarkData.json`) into the connected test's additional output. */
internal object TimingReport {
    private const val OUTPUT_ARGUMENT = "additionalTestOutputDir"

    /**
     * Records [timing] of [className].[name]; one file per benchmark so parallel classes never overwrite each
     * other.
     *
     * [extra] carries further series of the same measurement under their own metric names — the two sides of a
     * paired comparison beside the difference, say — so the raw samples survive without inventing benchmark names
     * that no `@Test` declares.
     */
    fun record(
        className: String,
        name: String,
        timing: Timing,
        extra: Map<String, Timing> = emptyMap(),
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory =
            InstrumentationRegistry.getArguments().getString(OUTPUT_ARGUMENT)?.let(::File)
                ?: instrumentation.targetContext.filesDir
        directory.mkdirs()
        File(directory, "$className.$name-benchmarkData.json")
            .writeText(json(className, name, timing, extra).toString())
    }

    private fun json(
        className: String,
        name: String,
        timing: Timing,
        extra: Map<String, Timing>,
    ): JSONObject {
        val runs = timing.runsNs
        val metrics = JSONObject().put("timeNs", metric(timing))
        extra.forEach { (metricName, series) -> metrics.put(metricName, metric(series)) }
        val benchmark =
            JSONObject()
                .put("name", name)
                .put("className", className)
                .put("totalRunTimeNs", runs.sum())
                .put("metrics", metrics)
                .put("warmupIterations", timing.warmup)
                .put("repeatIterations", runs.size)
        val build = JSONObject().put("model", Build.MODEL).put("sdk", Build.VERSION.SDK_INT)
        return JSONObject()
            .put("context", JSONObject().put("build", build))
            .put("benchmarks", JSONArray().put(benchmark))
    }

    private fun metric(timing: Timing): JSONObject {
        val runs = timing.runsNs
        return JSONObject()
            .put("minimum", runs.min())
            .put("maximum", runs.max())
            .put("median", timing.medianNs)
            .put("runs", JSONArray(runs))
    }
}
