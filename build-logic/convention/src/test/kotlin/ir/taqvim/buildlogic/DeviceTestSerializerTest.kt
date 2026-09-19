/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The device is a single resource: only the instrumentation test tasks may be serialized on it, and the build tasks
 * that merely end in `AndroidTest` must not be, or every module's test APK would be assembled one after another.
 */
class DeviceTestSerializerTest {
    @Test
    fun `connected and managed device test tasks run on the device`() {
        val tasks =
            listOf(
                "com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask",
                "com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask_Decorated",
                "com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestTask_Decorated",
            )
        tasks.forEach { runsOnADevice(it) shouldBe true }
    }

    @Test
    fun `build tasks of the android test variant do not`() {
        val tasks =
            listOf(
                "com.android.build.gradle.tasks.PackageAndroidArtifact_Decorated",
                "com.android.build.gradle.internal.tasks.AndroidTestResourcesTask_Decorated",
                "org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated",
                "org.gradle.api.DefaultTask_Decorated",
            )
        tasks.forEach { runsOnADevice(it) shouldBe false }
    }

    @Test
    fun `an unrelated class with a similar name does not`() {
        runsOnADevice("com.example.DeviceProviderInstrumentTestTask") shouldBe false
    }
}
