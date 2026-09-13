plugins {
    alias(libs.plugins.taqvim.android.library)
    alias(libs.plugins.taqvim.android.compose)
}

dependencies {
    api(projects.core.model)
    api(projects.core.i18n)

    testImplementation(projects.core.uiTesting)
}
