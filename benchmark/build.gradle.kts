plugins {
    alias(libs.plugins.taqvim.android.test)
    // T-1800 (ADR-0018): produces the app's baseline and startup profiles from BaselineProfileGenerator.
    id("androidx.baselineprofile")
}

/** Gradle Managed Device that generates the profiles: `./gradlew :app:generateBaselineProfile` (docs/RELEASE.md). */
val profileDevice = "pixel6Api34"

/**
 * Generates the profiles on an attached device instead of [profileDevice]: `-Ptaqvim.profileOnConnectedDevice=true`.
 *
 * The managed device's `aosp-atd` image is stripped of the storage the generator writes through, so
 * `:app:generateBaselineProfile` dies there with `ENOTCONN` on `/storage/emulated/0`. A full emulator (or a phone)
 * has it. Without this property the only way past that was to edit this file and put it back afterwards, which is
 * how the profile went stale unnoticed once already (T-1800; the guard is `BaselineProfileFreshnessKonsistTest`).
 */
val profileOnConnectedDevice =
    providers.gradleProperty("taqvim.profileOnConnectedDevice").map { it.toBoolean() }.orElse(false)

android {
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
    testOptions {
        managedDevices {
            localDevices {
                create(profileDevice) {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

baselineProfile {
    if (profileOnConnectedDevice.get()) {
        useConnectedDevices = true
    } else {
        managedDevices += profileDevice
        useConnectedDevices = false
    }
}

androidComponents {
    // The nightly benchmarks use the "benchmark" variant; the profile plugin adds its own nonMinified/benchmark variants.
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark" || it.buildType.orEmpty().startsWith("nonMinified") ||
            it.buildType.orEmpty().startsWith("benchmark")
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
