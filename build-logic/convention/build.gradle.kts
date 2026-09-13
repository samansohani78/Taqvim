plugins {
    `kotlin-dsl`
}

group = "ir.taqvim.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.serialization.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("root") {
            id = "taqvim.root"
            implementationClass = "ir.taqvim.buildlogic.RootConventionPlugin"
        }
        register("quality") {
            id = "taqvim.quality"
            implementationClass = "ir.taqvim.buildlogic.QualityConventionPlugin"
        }
        register("jvmLibrary") {
            id = "taqvim.jvm.library"
            implementationClass = "ir.taqvim.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "taqvim.android.library"
            implementationClass = "ir.taqvim.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "taqvim.android.compose"
            implementationClass = "ir.taqvim.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "taqvim.android.feature"
            implementationClass = "ir.taqvim.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "taqvim.android.application"
            implementationClass = "ir.taqvim.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidTest") {
            id = "taqvim.android.test"
            implementationClass = "ir.taqvim.buildlogic.AndroidTestConventionPlugin"
        }
        register("lintChecks") {
            id = "taqvim.lint.checks"
            implementationClass = "ir.taqvim.buildlogic.LintChecksConventionPlugin"
        }
    }
}
