plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1000: calendar arithmetic, recurrence rules (ADR-0011) and typed date phrases (T-500).
    implementation(projects.core.calendar)
    implementation(projects.core.ics)
    implementation(projects.core.nlp)
    implementation(libs.kotlinx.coroutines.core)
}
