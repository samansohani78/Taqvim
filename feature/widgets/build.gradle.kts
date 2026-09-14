plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1200: Glance app widgets, day-boundary arithmetic in the widget's time zone, the configuration activity.
    implementation(projects.core.calendar)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.androidx.glance.appwidget.testing)
}
