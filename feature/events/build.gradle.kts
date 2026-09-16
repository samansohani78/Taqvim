plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1000: calendar arithmetic, recurrence rules (ADR-0011) and typed date phrases (T-500).
    implementation(projects.core.calendar)
    implementation(projects.core.ics)
    implementation(projects.core.nlp)
    implementation(libs.kotlinx.coroutines.core)
    // B11: the unsaved draft survives process death as JSON in SavedStateHandle.
    implementation(libs.kotlinx.serialization.json)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
    systemProperty("taqvim.layout.audit", "true")
}
