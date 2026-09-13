plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    implementation(projects.core.model)
    // Generated OfficialEvents exposes EventDefinition (D-08).
    api(projects.core.events)
}

// Official daily rows extracted from the Calendar Center's calendars (golden fixtures of :core:calendar), for T-303.
val officialDaysDirectory = rootProject.layout.projectDirectory.dir("core/calendar/src/test/resources/golden/persian")

tasks.withType<Test>().configureEach {
    systemProperty("taqvim.official.days.directory", officialDaysDirectory.asFile.path)
    inputs.dir(officialDaysDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
}
