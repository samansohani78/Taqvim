plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // kotlinx.datetime types (LocalDate, TimeZone, Instant) appear in the public bridge API (T-107).
    api(libs.kotlinx.datetime)
    // ICU4J is a validation oracle only (docs/PLAN.md §6); never a runtime dependency of :core:calendar.
    testImplementation(libs.icu4j)
    // Recomputes the generated Persian leap table from equinoxes (A-02); test scope only, never at runtime.
    testImplementation(libs.astronomy)
}
