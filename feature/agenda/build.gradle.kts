plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-901: month arithmetic of the list, "today" and the Islamic variant selection.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
