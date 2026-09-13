/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Pure Kotlin/JVM module (all `:core:*` domain modules, `:tools:*`, `:konsist`).
 * `:core:*` modules use strict explicit-API mode so every public declaration is intentional and documented.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("com.android.lint")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = TaqvimBuild.javaVersion
                targetCompatibility = TaqvimBuild.javaVersion
            }
            tasks.withType<JavaCompile>().configureEach { options.release.set(TaqvimBuild.JAVA_RELEASE) }
            if (isCoreModule) {
                extensions.configure<KotlinJvmProjectExtension> { explicitApi() }
            }
            configureKotlinCompiler()
            val root = rootDirectory
            extensions.configure<Lint> { configureTaqvimLint(root) }
            configureTestTasks()
            addBaseTestDependencies()
            pluginManager.apply(QualityConventionPlugin::class.java)
        }
    }
}
