plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    implementation(projects.core.model)
    // Generated OfficialEvents exposes EventDefinition (D-08).
    api(projects.core.events)
}
