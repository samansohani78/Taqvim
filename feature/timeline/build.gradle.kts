plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-900: calendar arithmetic and the Islamic variant, day titles and digits, prayer lines of the timeline.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.i18n)
    implementation(projects.core.praytimes)
    implementation(libs.kotlinx.coroutines.core)
}
