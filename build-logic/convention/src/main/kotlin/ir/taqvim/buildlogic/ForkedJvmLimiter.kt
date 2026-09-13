/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Shared build service whose only job is to cap how many memory-hungry forked JVMs (test workers,
 * detekt) run at the same time across all modules. Without it, `org.gradle.parallel` happily starts
 * one JVM per module and exhausts RAM on 14 GB machines (ADR-0004 §9).
 */
abstract class ForkedJvmLimiter : BuildService<BuildServiceParameters.None>

/** Registers (once per build) and returns the [ForkedJvmLimiter]; limit from `taqvim.maxForkedJvms`. */
internal fun Project.forkedJvmLimiter(): Provider<ForkedJvmLimiter> {
    val limit = providers.gradleProperty("taqvim.maxForkedJvms").map(String::toInt).orElse(DEFAULT_MAX_FORKED_JVMS)
    return gradle.sharedServices.registerIfAbsent("taqvimForkedJvmLimiter", ForkedJvmLimiter::class.java) {
        maxParallelUsages.set(limit)
    }
}

/** Heap for each forked test JVM (Robolectric needs ~700 MB for Compose screenshot tests). */
internal const val TEST_JVM_HEAP = "1g"

/** Heap for each forked detekt JVM (no type resolution). */
internal const val DETEKT_JVM_HEAP = "768m"

private const val DEFAULT_MAX_FORKED_JVMS = 2
