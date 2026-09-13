plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // cosinekitty/astronomy (MIT) stays behind the façade: its types never appear in the public API (A-13).
    implementation(libs.astronomy)
}
