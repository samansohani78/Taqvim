plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // kotlinx.datetime types (LocalDate, TimeZone, Instant) appear in the public bridge API (T-107).
    api(libs.kotlinx.datetime)
    // cosinekitty/astronomy (MIT): the March equinox behind Persian year starts (A-02, ADR-0026). Its types stay
    // inside CalendarAstronomy and never appear in the public API.
    implementation(libs.astronomy)
    // ICU4J is a validation oracle only (docs/PLAN.md §6); never a runtime dependency of :core:calendar.
    testImplementation(libs.icu4j)
}
