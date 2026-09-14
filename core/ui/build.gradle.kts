plugins {
    alias(libs.plugins.taqvim.android.library)
    alias(libs.plugins.taqvim.android.compose)
}

dependencies {
    api(projects.core.model)
    api(projects.core.i18n)
    // Runtime permission requests shared by features (notification permission, T-1001/T-1101).
    implementation(libs.androidx.activity.compose)

    testImplementation(projects.core.uiTesting)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
}
