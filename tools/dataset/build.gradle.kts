plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    implementation(libs.networknt.json.schema.validator)
    implementation(libs.kotlinx.serialization.json)
}

val datasetDirectory = rootProject.layout.projectDirectory.dir("dataset")

// `./gradlew :tools:dataset:validate` — D-01 validator CLI over dataset/**/*.json.
tasks.register<JavaExec>("validate") {
    group = "verification"
    description = "Validates dataset/**/*.json against dataset/events.v1.json and the cross-record rules (D-01)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ir.taqvim.tools.dataset.DatasetCliKt")
    args(datasetDirectory.file("events.v1.json").asFile.path, datasetDirectory.asFile.path)
}

// Official daily rows extracted from the Calendar Center's calendars (golden fixtures of :core:calendar, T-102).
val officialDaysDirectory = rootProject.layout.projectDirectory.dir("core/calendar/src/test/resources/golden/persian")

tasks.withType<Test>().configureEach {
    systemProperty("taqvim.dataset.schema", datasetDirectory.file("events.v1.json").asFile.path)
    systemProperty("taqvim.dataset.directory", datasetDirectory.asFile.path)
    systemProperty("taqvim.official.days.directory", officialDaysDirectory.asFile.path)
    inputs.dir(datasetDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(officialDaysDirectory).withPathSensitivity(PathSensitivity.RELATIVE)
}
