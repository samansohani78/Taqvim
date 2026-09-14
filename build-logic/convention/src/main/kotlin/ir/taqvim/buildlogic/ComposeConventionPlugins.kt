/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.android.build.api.dsl.CommonExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Enables Jetpack Compose plus Roborazzi screenshot testing. Reference screenshots live in
 * `src/test/screenshots` so `verifyRoborazziDebug` can compare against committed images.
 * Compose compiler metrics are emitted when `-Ptaqvim.composeMetrics=true` (T-1802).
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("io.github.takahirom.roborazzi")
            extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true
            val root = rootDirectory
            val metrics = providers.gradleProperty("taqvim.composeMetrics").map { it.toBoolean() }.orElse(false)
            val reports = layout.buildDirectory.dir("compose/reports")
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(root.file("config/compose/stability.conf"))
                if (metrics.get()) {
                    metricsDestination.set(layout.buildDirectory.dir("compose/metrics"))
                    reportsDestination.set(reports)
                }
            }
            registerComposeStabilityCheck(metrics.get())
            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            }
            addComposeDependencies()
        }
    }
}

/**
 * `composeStabilityCheck` (T-1802, ADR-0021): reads this module's debug Compose compiler reports and fails on unstable
 * composable parameters or UI models. Report destinations are not compile-task inputs, so the reports flag is added
 * as one; otherwise an up-to-date compile would leave no reports behind.
 */
private fun Project.registerComposeStabilityCheck(reportsEnabled: Boolean) {
    val reports = layout.buildDirectory.dir("compose/reports")
    tasks.withType<KotlinJvmCompile>().configureEach {
        inputs.property("taqvimComposeReports", reportsEnabled)
        if (reportsEnabled && name == "compileDebugKotlin") outputs.dir(reports)
    }
    tasks.register<ComposeStabilityCheckTask>("composeStabilityCheck") {
        group = "verification"
        description = "Fails on unstable Compose parameters and UI models (needs -Ptaqvim.composeMetrics=true)."
        reportsDirectory.set(reports)
        exceptionsFile.set(rootDirectory.file("config/compose/stability-exceptions.txt"))
        this.reportsEnabled.set(reportsEnabled)
        dependsOn("compileDebugKotlin")
    }
}

private fun Project.addComposeDependencies() {
    val catalog = libs
    dependencies {
        val bom = platform(catalog.library("androidx-compose-bom"))
        "implementation"(bom)
        "testImplementation"(bom)
        "androidTestImplementation"(bom)
        "implementation"(catalog.library("androidx-compose-ui"))
        "implementation"(catalog.library("androidx-compose-ui-tooling-preview"))
        "implementation"(catalog.library("androidx-compose-material3"))
        "debugImplementation"(catalog.library("androidx-compose-ui-tooling"))
        "debugImplementation"(catalog.library("androidx-compose-ui-test-manifest"))
        "testImplementation"(catalog.library("androidx-compose-ui-test-junit4"))
        "testImplementation"(catalog.library("roborazzi"))
        "testImplementation"(catalog.library("roborazzi-compose"))
        "testImplementation"(catalog.library("roborazzi-junit-rule"))
    }
}

/**
 * Feature module: Compose UI + ViewModels. Depends only on `:core:*` (never on other features or
 * `:data:*`; enforced by Konsist) — DI in `:app` binds implementations.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
            pluginManager.apply(AndroidComposeConventionPlugin::class.java)
            val catalog = libs
            dependencies {
                "implementation"(project(":core:model"))
                "implementation"(project(":core:ui"))
                "implementation"(platform(catalog.library("koin-bom")))
                "implementation"(catalog.library("koin-androidx-compose"))
                "implementation"(catalog.library("androidx-lifecycle-runtime-compose"))
                "implementation"(catalog.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(catalog.library("androidx-navigation3-runtime"))
                "implementation"(catalog.library("kotlinx-collections-immutable"))
                "testImplementation"(project(":core:ui-testing"))
                "testImplementation"(catalog.library("turbine"))
                "testImplementation"(catalog.library("kotlinx-coroutines-test"))
            }
        }
    }
}
