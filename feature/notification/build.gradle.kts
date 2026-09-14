plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1102: athan times (A-10), days and time zones, the athan service and notifications.
    implementation(projects.core.praytimes)
    implementation(projects.core.calendar)
    // T-1001/T-1002: personal event recurrences (T-503) and official event occurrences (T-300).
    implementation(projects.core.ics)
    implementation(projects.core.events)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
}
