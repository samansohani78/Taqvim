plugins {
    alias(libs.plugins.taqvim.android.feature)
}

dependencies {
    // T-1504: loading the bundled license catalog and diagnostics; the catalog is read as a JSON tree (no plugin).
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
}

// The license catalog test compares the bundled asset with the license gate report when that report exists.
tasks.withType<Test>().configureEach {
    systemProperty(
        "taqvim.about.assets",
        layout.projectDirectory
            .dir("src/main/assets")
            .asFile.path,
    )
    systemProperty(
        "taqvim.license.report",
        rootProject.layout.buildDirectory
            .file("reports/licenses/license-report.json")
            .get()
            .asFile.path,
    )
}
