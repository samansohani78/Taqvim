plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.calendar)
    implementation(projects.core.i18n)
}
