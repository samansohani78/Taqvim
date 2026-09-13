plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // ICU4J is a validation oracle for the CLDR-derived language table (T-200); never a runtime dependency.
    testImplementation(libs.icu4j)
}
