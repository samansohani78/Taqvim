/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.uitesting

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts how often the body of one composable function runs, i.e. how often it was composed or recomposed rather
 * than skipped (T-1802). It listens to the Compose runtime's trace events, which the Compose compiler emits for every
 * restartable function, so the counted composable needs no test hooks.
 *
 * ```
 * RecompositionCounter("ir.taqvim.core.ui.component.DayCell").use { counter -> …; counter.count shouldBe 1 }
 * ```
 *
 * The tracer is process-wide: install one counter at a time and close it when done.
 */
@OptIn(InternalComposeTracingApi::class)
class RecompositionCounter(
    /** Fully qualified name of the composable function, e.g. `ir.taqvim.core.ui.component.DayCell`. */
    private val functionName: String,
) : AutoCloseable {
    private val runs = AtomicInteger()

    /** Number of times the function body ran since this counter was created. */
    val count: Int
        get() = runs.get()

    private val tracer =
        object : CompositionTracer {
            override fun traceEventStart(
                key: Int,
                dirty1: Int,
                dirty2: Int,
                info: String,
            ) {
                if (info.substringBefore(" (") == functionName) runs.incrementAndGet()
            }

            override fun traceEventEnd() = Unit

            override fun isTraceInProgress(): Boolean = true
        }

    init {
        Composer.setTracer(tracer)
    }

    override fun close() {
        Composer.setTracer(null)
    }
}
