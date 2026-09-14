plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-901: month arithmetic of the list, "today" and the Islamic variant selection.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(libs.kotlinx.coroutines.core)
}
