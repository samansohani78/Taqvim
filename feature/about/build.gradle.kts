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
    // ADR-0039: the bundled-data licenses of `DataSource` are checked against the repository allow-list.
    systemProperty(
        "taqvim.license.allowlist",
        rootProject.layout.projectDirectory
            .file("config/license/allowed-licenses.json")
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

// T-1700: every captured screenshot state is also audited for accessibility (AccessibilityAudit).
tasks.withType<Test>().configureEach {
    systemProperty("taqvim.a11y.audit", "true")
    systemProperty("taqvim.layout.audit", "true")
}
