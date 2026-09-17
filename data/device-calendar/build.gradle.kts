plugins {
    alias(libs.plugins.taqvim.android.library)
}

dependencies {
    api(projects.core.model)
    // Civil days (Jdn, TimeZone) appear in the adapter API; instances are cached in `device_events_cache` (T-601).
    api(projects.core.calendar)
    api(projects.data.database)
    implementation(libs.kotlinx.coroutines.core)
    // Review I06: the device zone stream registers an ACTION_TIMEZONE_CHANGED receiver.
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
