plugins {
    alias(libs.plugins.taqvim.android.library)
    id("com.google.devtools.ksp")
}

// Exported Room schemas (committed) feed MigrationTestHelper in host tests (T-601). The new DSL type is used because
// the legacy `android` accessor's source sets cannot be cast under AGP 9.
extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    sourceSets.named("test") { assets.directories.add("$projectDir/schemas") }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    // Recurrence and workday types are stored as columns and mapped back (docs/PLAN.md §4.3).
    api(projects.core.ics)
    api(projects.core.events)
    api(projects.core.workdays)
    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
