plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-800: calendar arithmetic, "today" and the Islamic variant selection of the day details.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    // T-802: prayer times of the Times tab; Sun and Moon of the Calendars tab.
    implementation(projects.core.praytimes)
    implementation(projects.core.astronomy)
    implementation(libs.kotlinx.coroutines.core)
    // T-806: foldable posture (tabletop) of the adaptive layout.
    implementation(libs.androidx.compose.material3.adaptive)
}
