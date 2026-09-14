plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1102: athan times (A-10), days and time zones, the athan service and notifications.
    implementation(projects.core.praytimes)
    implementation(projects.core.calendar)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
}
