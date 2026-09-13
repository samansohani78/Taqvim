plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1400: calendars and date parsing (converter, distance), workdays (F-07), QR encoding and image sharing.
    implementation(projects.core.calendar)
    implementation(projects.core.nlp)
    implementation(projects.core.workdays)
    implementation(libs.zxing.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(projects.core.events)
}
