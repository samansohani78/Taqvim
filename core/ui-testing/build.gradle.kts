plugins {
    alias(libs.plugins.taqvim.android.library)
    alias(libs.plugins.taqvim.android.compose)
}

// Test-support library: consumed only through testImplementation, so its tooling is exposed as api.
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.ext.junit)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
}
