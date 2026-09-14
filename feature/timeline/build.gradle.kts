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

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
