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

/** Build service that lets only one `timingTest` task run at a time, so timings are not measured under load. */
abstract class TimingTestSerializer : BuildService<BuildServiceParameters.None>

/** Registers (once per build) and returns the [TimingTestSerializer]. */
internal fun Project.timingTestSerializer(): Provider<TimingTestSerializer> =
    gradle.sharedServices.registerIfAbsent("taqvimTimingTestSerializer", TimingTestSerializer::class.java) {
        maxParallelUsages.set(1)
    }

/**
 * Build service that lets only one instrumented test task use the device at a time.
 *
 * A build runs the `connected*AndroidTest` and managed-device tasks of every module against the **one** device
 * attached to it, and Gradle starts them in parallel. The tests that drive the app's UI then compete for the
 * foreground with another module's test activity: on the API 33 leg of main@ce6d101 the `:app` device tests ran from
 * 07:33:13 to 07:38:29 while some twenty other modules' device tests ran inside that window, and all eight tests that
 * open a screen failed with "the app crashed or the screen did not open" while the four that never launch the UI
 * passed. One user at a time makes the device behave like the single resource it is.
 */
abstract class DeviceTestSerializer : BuildService<BuildServiceParameters.None>

/** Registers (once per build) and returns the [DeviceTestSerializer]. */
internal fun Project.deviceTestSerializer(): Provider<DeviceTestSerializer> =
    gradle.sharedServices.registerIfAbsent("taqvimDeviceTestSerializer", DeviceTestSerializer::class.java) {
        maxParallelUsages.set(1)
    }

/**
 * True when [taskClassName] is one of AGP's instrumentation test tasks, which run against the attached device.
 *
 * Gradle decorates task classes (`…Task_Decorated`), so the name is matched by prefix. Tasks are matched by type and
 * not by name because `assembleDebugAndroidTest` and the other build tasks also end in `AndroidTest` and must keep
 * running in parallel.
 */
internal fun runsOnADevice(taskClassName: String): Boolean = DEVICE_TEST_TASK_CLASSES.any(taskClassName::startsWith)

private val DEVICE_TEST_TASK_CLASSES =
    listOf(
        "com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask",
        "com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestTask",
    )

/** Heap for each forked test JVM (Robolectric needs ~700 MB for Compose screenshot tests). */
internal const val TEST_JVM_HEAP = "1g"

/** Heap for each forked detekt JVM (no type resolution). */
internal const val DETEKT_JVM_HEAP = "768m"

private const val DEFAULT_MAX_FORKED_JVMS = 2
