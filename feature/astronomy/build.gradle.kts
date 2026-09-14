plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1300: the astronomy façade (A-13, A-14, T-404…T-406), calendars for dates and the date picker, i18n formatting.
    implementation(projects.core.astronomy)
    implementation(projects.core.calendar)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
