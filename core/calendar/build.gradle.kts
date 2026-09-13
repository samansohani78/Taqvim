plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // ICU4J is a validation oracle only (docs/PLAN.md §6); never a runtime dependency of :core:calendar.
    testImplementation(libs.icu4j)
}
