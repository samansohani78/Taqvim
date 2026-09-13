plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.i18n)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
