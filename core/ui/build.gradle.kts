plugins {
    alias(libs.plugins.taqvim.android.library)
    alias(libs.plugins.taqvim.android.compose)
}

dependencies {
    api(projects.core.model)
}
