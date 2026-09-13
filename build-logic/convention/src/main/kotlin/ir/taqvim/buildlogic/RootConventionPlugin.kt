/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Root project: repository-wide formatting (Spotless + ktlint), merged coverage with the overall
 * 85 % line gate, and aggregate verification entry points (`konsistTest`).
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        check(target == target.rootProject) { "taqvim.root must be applied to the root project" }
        with(target) {
            pluginManager.apply("base")
            configureSpotless()
            configureMergedCoverage()
            tasks.register("konsistTest") {
                group = "verification"
                description = "Runs the Konsist architecture rule suite."
                dependsOn(":konsist:test")
            }
        }
    }
}

private val FORMAT_EXCLUDES = listOf("**/build/**", "**/.gradle/**", "**/.kotlin/**")

private fun Project.configureSpotless() {
    pluginManager.apply("com.diffplug.spotless")
    val ktlintVersion = libs.versionOf("ktlint")
    extensions.configure<SpotlessExtension> {
        // Explicit policy: the default git-attributes policy is not configuration-cache serializable.
        lineEndings = LineEnding.UNIX
        kotlin {
            target("**/*.kt")
            targetExclude(FORMAT_EXCLUDES)
            ktlint(ktlintVersion)
            licenseHeaderFile(file("config/spotless/copyright.txt"))
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude(FORMAT_EXCLUDES)
            ktlint(ktlintVersion)
        }
        format("misc") {
            target("**/*.md", "**/.gitignore", "**/*.yml", "**/*.yaml", "**/*.toml", "**/*.pro")
            targetExclude(FORMAT_EXCLUDES + "docs/PLAN.md")
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("xml") {
            target("**/src/**/*.xml", "config/**/*.xml")
            targetExclude(FORMAT_EXCLUDES)
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

/** Modules without production classes or unsupported by Kover. */
private val COVERAGE_EXCLUDED_PROJECTS = setOf(":benchmark", ":konsist")

private fun Project.configureMergedCoverage() {
    pluginManager.apply("org.jetbrains.kotlinx.kover")
    val covered = subprojects.filter { it.buildFile.exists() && it.path !in COVERAGE_EXCLUDED_PROJECTS }
    dependencies {
        covered.forEach { "kover"(project(it.path)) }
    }
    extensions.configure<KoverProjectExtension> {
        reports {
            filters { excludes { classes(GENERATED_CLASS_PATTERNS) } }
            verify {
                rule("Overall line coverage") { minBound(OVERALL_LINE_COVERAGE) }
            }
        }
    }
}

private const val OVERALL_LINE_COVERAGE = 85
