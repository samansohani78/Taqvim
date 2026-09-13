plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    implementation(projects.core.model)
    // Generated OfficialEvents exposes EventDefinition (D-08).
    api(projects.core.events)
    // Nowruz-week golden test of the workday engine (T-504) on the generated official events.
    testImplementation(projects.core.workdays)
}

// Official daily rows extracted from the Calendar Center's calendars (golden fixtures of :core:calendar), for T-303.
val officialDaysDirectory = rootProject.layout.projectDirectory.dir("core/calendar/src/test/resources/golden/persian")

tasks.withType<Test>().configureEach {
    systemProperty("taqvim.official.days.directory", officialDaysDirectory.asFile.path)
    inputs.dir(officialDaysDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
}
