plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1502: coroutines for search debounce and lookups; the location permission request.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
}
