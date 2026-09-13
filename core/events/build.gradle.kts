plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // CalendarArithmetic appears in the public rule-engine API (CalendarProvider).
    api(projects.core.calendar)
    // PersianText search keys and FuzzyMatcher for the event search index (T-304).
    implementation(projects.core.i18n)
    // ICU4J is a validation oracle only; never a runtime dependency.
    testImplementation(libs.icu4j)
}
