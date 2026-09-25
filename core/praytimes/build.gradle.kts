plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // The Umm al-Qura month: that method's Isha interval is longer in Ramadan (A-10). Core may depend on core.
    implementation(projects.core.calendar)
    // cosinekitty/astronomy (MIT): the apparent Sun behind prayer times (ADR-0029). Its types stay inside
    // SolarEphemeris and never appear in the public API.
    implementation(libs.astronomy)
    // Converts the official Solar Hijri timetable dates in golden tests.
    testImplementation(projects.core.calendar)
}
