plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-805: calendar arithmetic, the Islamic variant selection and the language table of the year view.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.i18n)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
    systemProperty("taqvim.layout.audit", "true")
}
