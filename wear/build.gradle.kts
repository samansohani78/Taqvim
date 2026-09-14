plugins {
    alias(libs.plugins.taqvim.android.application)
    alias(libs.plugins.taqvim.android.compose)
}

android {
    namespace = "ir.taqvim.wear"
    defaultConfig {
        applicationId = "ir.taqvim.app"
        // Wear OS 3+ (API 30) is required by Compose for Wear OS and Tiles (T-1600, ADR-0019).
        minSdk = 30
        versionCode = 1
        versionName = "0.1.0"
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

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
