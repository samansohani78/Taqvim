plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1100: prayer times, report-month arithmetic, and localized date and number formatting (via :core:ui).
    implementation(projects.core.astronomy)
    implementation(projects.core.praytimes)
    implementation(projects.core.calendar)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
    systemProperty("taqvim.layout.audit", "true")
}
