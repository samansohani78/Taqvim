/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.android.build.api.dsl.Lint
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Static analysis and coverage for a single module: detekt, Kover (with the `:core:*` 95 % line /
 * 90 % branch gate), and the custom `:lint` checks. Must be applied after the Kotlin/Android plugin.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            configureDetekt()
            configureModuleCoverage()
            if (path != ":lint" && configurations.findByName("lintChecks") != null) {
                dependencies { "lintChecks"(project(":lint")) }
            }
        }
    }
}

private fun Project.configureModuleCoverage() {
    val core = isCoreModule
    extensions.configure<KoverProjectExtension> {
        reports {
            filters { excludes { classes(GENERATED_CLASS_PATTERNS) } }
            if (core) {
                verify {
                    rule("Core line coverage") { minBound(CORE_LINE_COVERAGE) }
                    rule("Core branch coverage") { minBound(CORE_BRANCH_COVERAGE, CoverageUnit.BRANCH) }
                }
            }
        }
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn("koverVerify") }
}

/** Shared Android/JVM lint configuration: warnings are errors (reports, incl. SARIF, are always generated). */
internal fun Lint.configureTaqvimLint(root: Directory) {
    abortOnError = true
    warningsAsErrors = true
    checkDependencies = false
    checkTestSources = false
    lintConfig = root.file("config/lint/lint.xml").asFile
}

internal val GENERATED_CLASS_PATTERNS =
    listOf(
        "*.BuildConfig",
        "*.R",
        "*.R$*",
        "*ComposableSingletons*",
        "*_Impl",
        "*_Impl$*",
        "*.generated.*",
    )

private const val CORE_LINE_COVERAGE = 95
private const val CORE_BRANCH_COVERAGE = 90
