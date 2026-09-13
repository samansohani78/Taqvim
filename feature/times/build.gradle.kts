plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1100: prayer times, report-month arithmetic, and localized date and number formatting (via :core:ui).
    implementation(projects.core.praytimes)
    implementation(projects.core.calendar)
    implementation(libs.kotlinx.coroutines.core)
}
