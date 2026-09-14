plugins {
    alias(libs.plugins.taqvim.android.library)
    alias(libs.plugins.taqvim.android.compose)
}

dependencies {
    api(projects.core.model)
    api(projects.core.i18n)
    // Runtime permission requests shared by features (notification permission, T-1001/T-1101).
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.core.uiTesting)
}
