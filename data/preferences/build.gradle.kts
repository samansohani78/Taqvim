plugins {
    alias(libs.plugins.taqvim.android.library)
    id("com.google.protobuf")
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.i18n)
    api(libs.androidx.datastore)
    api(libs.protobuf.kotlin.lite)

    testImplementation(libs.kotlinx.coroutines.test)
}

// UserPrefs is generated as Java and Kotlin lite messages (T-600, docs/PLAN.md §4.4).
protobuf {
    protoc {
        artifact =
            libs.protobuf.protoc
                .get()
                .toString()
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}
