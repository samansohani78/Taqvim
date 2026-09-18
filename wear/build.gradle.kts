plugins {
    alias(libs.plugins.taqvim.android.application)
    alias(libs.plugins.taqvim.android.compose)
}

/**
 * Gradle Managed Device for the watch smoke tests: `./gradlew :wear:wearApi34DebugAndroidTest`
 * (§0 of docs/MANUAL_TEST_CHECKLIST.md).
 */
val smokeDevice = "wearApi34"

android {
    namespace = "ir.taqvim.wear"
    defaultConfig {
        applicationId = "ir.taqvim.app"
        // Wear OS 3+ (API 30) is required by Compose for Wear OS and Tiles (T-1600, ADR-0019).
        minSdk = 30
        versionCode = 1
        versionName = "0.1.0"
        // A watch test that hangs (e.g. a screen that never becomes idle) fails after 10 minutes instead of stalling.
        testInstrumentationRunnerArguments["timeout_msec"] = "600000"
    }
    testOptions {
        managedDevices {
            localDevices {
                create(smokeDevice) {
                    // Wear OS 5 (`system-images;android-34;android-wear;x86_64`) on the smallest round watch.
                    device = "Wear OS Small Round"
                    apiLevel = 34
                    systemImageSource = "android-wear"
                    // The Wear image has no NDK translation; AGP 10 would otherwise default to arm64-v8a.
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.i18n)
    implementation(projects.core.praytimes)
    // The watch is standalone (ADR-0019): its own preferences store, the bundled dataset and the city catalog.
    implementation(projects.data.preferences)
    implementation(projects.data.events)
    implementation(projects.data.location)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.expression)
    implementation(libs.androidx.wear.complications.data.source.ktx)
    implementation(libs.androidx.concurrent.futures.ktx)

    // Renders the tile layouts into real views for the tile screenshots and the watch smoke tests (T-1600). Debug
    // only: the system renders the tiles on a watch, so the release build never needs the renderer. Its resources
    // must be merged into the variant, which `testImplementation` would not do (the renderer themes itself).
    debugImplementation(libs.androidx.wear.tiles.renderer)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Watch smoke tests on the managed device (T-1600): real Wear OS, not Robolectric.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
