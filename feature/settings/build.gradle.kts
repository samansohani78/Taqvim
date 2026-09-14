plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1502: coroutines for search debounce and lookups; the location permission request.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    // T-1500: event sources and the high-latitude rule are chosen as their core types.
    implementation(projects.core.events)
    implementation(projects.core.praytimes)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
