plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1503: coroutines for the backup operations, the document pickers, and the backup date in the language's calendar.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.activity.compose)
    implementation(projects.core.calendar)
}
