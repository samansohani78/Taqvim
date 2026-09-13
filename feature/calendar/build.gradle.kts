plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-800: calendar arithmetic, "today" and the Islamic variant selection of the day details.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(libs.kotlinx.coroutines.core)
}
