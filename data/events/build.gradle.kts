plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    implementation(projects.core.model)
    // Generated OfficialEvents exposes EventDefinition (D-08).
    api(projects.core.events)
    // SkyAstronomicalEventSource computes Astronomical rule instants (ADR-0042, T-403).
    implementation(projects.core.astronomy)
    // T-305: personal and iCalendar tables, preferences and device instances are combined with the dataset.
    api(projects.data.database)
    api(projects.data.preferences)
    api(projects.data.deviceCalendar)
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    // T-1003: WebCal subscriptions are refreshed by periodic WorkManager work (docs/PLAN.md §3.3, F-05).
    api(libs.androidx.work.runtime.ktx)

    testImplementation(libs.androidx.work.testing)
    // Nowruz-week golden test of the workday engine (T-504) on the generated official events.
    testImplementation(projects.core.workdays)
    testImplementation(projects.core.i18n)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

// Official daily rows extracted from the Calendar Center's calendars (golden fixtures of :core:calendar), for T-303.
val officialDaysDirectory =
    rootProject.layout.projectDirectory.dir(
        "core/calendar/src/test/resources/golden/persian/official",
    )

tasks.withType<Test>().configureEach {
    systemProperty("taqvim.official.days.directory", officialDaysDirectory.asFile.path)
    inputs.dir(officialDaysDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
}
