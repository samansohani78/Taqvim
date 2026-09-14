plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-804: Persian-insensitive matching (T-203), "go to date" through the date parser (T-500) and today (T-107).
    implementation(projects.core.calendar)
    implementation(projects.core.i18n)
    implementation(projects.core.nlp)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
