plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1215: live wallpaper and daydream drawn with the T-702 painters; content is bound in :app.
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(projects.core.testing)
}
