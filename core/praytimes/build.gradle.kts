plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // Converts the official Solar Hijri timetable dates in golden tests; not used at runtime.
    testImplementation(projects.core.calendar)
}
