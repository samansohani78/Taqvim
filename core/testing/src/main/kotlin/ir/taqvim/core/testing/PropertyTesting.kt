/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

/**
 * Iteration budget for property tests (docs/PLAN.md §8.1): 1 000 by default, raised in the nightly job with
 * `-Ptaqvim.propertyIterations=10000` (forwarded to test JVMs as a system property).
 */
public object PropertyTesting {
    /** System property (and Gradle property) holding the iteration count. */
    public const val ITERATIONS_PROPERTY: String = "taqvim.propertyIterations"

    /** Iterations used when the property is absent or invalid. */
    public const val DEFAULT_ITERATIONS: Int = 1_000

    /** Iterations for the current run. */
    public val iterations: Int
        get() = parse(System.getProperty(ITERATIONS_PROPERTY))

    /** Parses [value]; blank, non-numeric or non-positive values fall back to [DEFAULT_ITERATIONS]. */
    public fun parse(value: String?): Int = value?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_ITERATIONS
}
