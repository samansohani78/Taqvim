plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // IslamicMonthTable in the public API of the observational Islamic month starts (A-06).
    api(projects.core.calendar)
    // cosinekitty/astronomy (MIT) stays behind the façade: its types never appear in the public API (A-13).
    implementation(libs.astronomy)
}
