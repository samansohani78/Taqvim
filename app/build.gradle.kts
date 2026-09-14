plugins {
    alias(libs.plugins.taqvim.android.application)
    alias(libs.plugins.taqvim.android.compose)
    // Serializable navigation destinations, saved with the back stack (ADR-0015).
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ir.taqvim.app"
    defaultConfig {
        applicationId = "ir.taqvim.app"
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        // Version and build type shown on the About screen and in problem reports (T-1504).
        buildConfig = true
    }
    signingConfigs {
        // Release signing comes from CI secrets (release.yml). Without them release builds stay unsigned.
        val keystore = providers.environmentVariable("TAQVIM_KEYSTORE_FILE").orNull
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("TAQVIM_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("TAQVIM_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("TAQVIM_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("debug") {
            // en-XA (accented, expanded) and ar-XB (mirrored RTL) pseudo-locales catch untranslated or clipped text.
            isPseudoLocalesEnabled = true
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendar)
    implementation(projects.core.events)
    implementation(projects.core.praytimes)
    implementation(projects.core.astronomy)
    implementation(projects.core.i18n)
    implementation(projects.core.nlp)
    implementation(projects.core.ics)
    implementation(projects.core.workdays)
    implementation(projects.core.ui)

    implementation(projects.data.preferences)
    implementation(projects.data.database)
    implementation(projects.data.events)
    implementation(projects.data.deviceCalendar)
    implementation(projects.data.location)
    implementation(projects.data.scheduler)

    implementation(projects.feature.calendar)
    implementation(projects.feature.timeline)
    implementation(projects.feature.month)
    implementation(projects.feature.year)
    implementation(projects.feature.agenda)
    implementation(projects.feature.events)
    implementation(projects.feature.times)
    implementation(projects.feature.astronomy)
    implementation(projects.feature.map)
    implementation(projects.feature.compass)
    implementation(projects.feature.tools)
    implementation(projects.feature.settings)
    implementation(projects.feature.about)
    implementation(projects.feature.widgets)
    implementation(projects.feature.notification)
    implementation(projects.feature.wallpaper)
    implementation(projects.feature.search)
    implementation(projects.feature.backup)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.browser)
    // LevelCalibration (T-1303) exposes an ImmutableMap in the compass port; stored by the T-1500 adapters.
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(libs.koin.test)
    // Screenshot environments of the navigation frame (ADR-0015).
    testImplementation(projects.core.uiTesting)
    testImplementation(libs.kotlinx.coroutines.test)
}
