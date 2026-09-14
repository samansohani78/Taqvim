plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1503: coroutines for the backup operations, the document pickers, and the backup date in the language's calendar.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    implementation(projects.core.calendar)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
