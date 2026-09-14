plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1301: Sun and Moon positions, crescent visibility and Qibla (A-06, A-11, A-13); calendars for dates.
    implementation(projects.core.astronomy)
    // Declination and equation of time of the subsolar point (A-09).
    implementation(projects.core.praytimes)
    implementation(libs.kotlinx.coroutines.core)
}

// T-1701: every captured screenshot state is also audited for text that does not fit.
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.layout.audit", "true")
}
