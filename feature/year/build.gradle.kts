plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-805: calendar arithmetic, the Islamic variant selection and the language table of the year view.
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.i18n)
    implementation(libs.kotlinx.coroutines.core)
}
