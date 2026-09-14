plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1200: Glance app widgets, day-boundary arithmetic in the widget's time zone, the configuration activity.
    implementation(projects.core.calendar)
    // T-1201…T-1204: localized dates and the prayer strip.
    implementation(projects.core.i18n)
    implementation(projects.core.praytimes)
    // T-1210: the Moon's phase and the next full and new moon.
    implementation(projects.core.astronomy)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.androidx.glance.appwidget.testing)
}
