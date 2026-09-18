plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // IslamicMonthTable in the public API of the observational Islamic month starts (A-06).
    api(projects.core.calendar)
    // cosinekitty/astronomy (MIT) stays behind the façade: its types never appear in the public API (A-13).
    implementation(libs.astronomy)
    // ICU4J's Chinese and Umm al-Qura calendars are validation oracles (T-406, ADR-0040); never runtime dependencies.
    testImplementation(libs.icu4j)
}

// The calibration report of ADR-0040, rewritten by `IslamicCalibrationReportTest` with -Ptaqvim.updateSnapshots=true.
val calibrationReport = rootProject.layout.projectDirectory.file("docs/data-todo/islamic-calibration-report.md")

// The official Iranian month starts 1381–1405 SH (ADR-0041), imported into `:core:calendar`'s golden fixtures.
val iranMonthStarts =
    rootProject.layout.projectDirectory.file(
        "core/calendar/src/test/resources/golden/islamic-iran/official-month-starts-1381-1405.csv",
    )

tasks.withType<Test>().configureEach {
    systemProperty("taqvim.calibration.report", calibrationReport.asFile.path)
    systemProperty("taqvim.iran.monthStarts", iranMonthStarts.asFile.path)
    inputs.files(calibrationReport, iranMonthStarts).withPathSensitivity(PathSensitivity.RELATIVE)
}
