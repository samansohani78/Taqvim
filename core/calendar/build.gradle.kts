plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // kotlinx.datetime types (LocalDate, TimeZone, Instant) appear in the public bridge API (T-107).
    api(libs.kotlinx.datetime)
    // cosinekitty/astronomy (MIT): the equinox, conjunctions, rise/set and crescent geometry behind computed calendars
    // (ADR-0026, ADR-0027, ADR-0028). Its types stay inside CalendarAstronomy and never appear in the public API.
    implementation(libs.astronomy)
    // Optional official Islamic month overrides are read as JSON (ADR-0037); kotlinx-serialization is Apache-2.0.
    implementation(libs.kotlinx.serialization.json)
    // ICU4J is a validation oracle only (docs/PLAN.md §6); never a runtime dependency of :core:calendar.
    testImplementation(libs.icu4j)
}
