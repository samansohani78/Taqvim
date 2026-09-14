plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1302/T-1303: Qibla bearing and Sun/Moon azimuths (A-11, A-13); localized digits via :core:ui's i18n API.
    implementation(projects.core.astronomy)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
    systemProperty("taqvim.layout.audit", "true")
}
