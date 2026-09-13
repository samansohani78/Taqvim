plugins {
    alias(libs.plugins.taqvim.android.application)
}

android {
    namespace = "ir.taqvim.wear"
    defaultConfig {
        applicationId = "ir.taqvim.app"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.model)
}
