plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    implementation(projects.core.model)
    // The `scheduled_alarms` table (T-601) and the preferences that decide alarm times (T-600) are part of the API.
    api(projects.data.database)
    api(projects.data.preferences)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
}
