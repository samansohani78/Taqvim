plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // cosinekitty/astronomy (MIT): the apparent Sun behind prayer times (ADR-0029). Its types stay inside
    // SolarEphemeris and never appear in the public API.
    implementation(libs.astronomy)
    // Converts the official Solar Hijri timetable dates in golden tests; not used at runtime.
    testImplementation(projects.core.calendar)
}
