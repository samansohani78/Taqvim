plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // CalendarArithmetic appears in the public rule-engine API (CalendarProvider).
    api(projects.core.calendar)
    // ICU4J is a validation oracle only; never a runtime dependency.
    testImplementation(libs.icu4j)
}
