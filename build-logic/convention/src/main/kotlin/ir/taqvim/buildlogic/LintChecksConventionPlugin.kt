/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** `:lint` — custom Android Lint detectors packaged with a `Lint-Registry-v2` manifest entry. */
class LintChecksConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = TaqvimBuild.javaVersion
                targetCompatibility = TaqvimBuild.javaVersion
            }
            tasks.withType<JavaCompile>().configureEach { options.release.set(TaqvimBuild.JAVA_RELEASE) }
            configureKotlinCompiler()
            configureTestTasks()
            tasks.withType<Jar>().configureEach {
                manifest { attributes(mapOf("Lint-Registry-v2" to "ir.taqvim.lint.TaqvimIssueRegistry")) }
            }
            val catalog = libs
            dependencies {
                "compileOnly"(catalog.library("lint-api"))
                "compileOnly"(catalog.library("lint-checks"))
                "testImplementation"(catalog.library("lint-api"))
                "testImplementation"(catalog.library("lint-checks"))
                "testImplementation"(catalog.library("lint-tests"))
                "testImplementation"(catalog.library("junit4"))
                "testRuntimeOnly"(platform(catalog.library("junit-bom")))
                "testRuntimeOnly"(catalog.library("junit-vintage-engine"))
                "testRuntimeOnly"(catalog.library("junit-platform-launcher"))
            }
            pluginManager.apply(QualityConventionPlugin::class.java)
        }
    }
}
