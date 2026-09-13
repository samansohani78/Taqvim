/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import ir.taqvim.buildlogic.license.ConfigurationClassifier
import ir.taqvim.buildlogic.license.LICENSE_COORDINATES_PATH
import ir.taqvim.buildlogic.license.LicenseCheckTask
import ir.taqvim.buildlogic.license.LicenseReportTask
import ir.taqvim.buildlogic.license.registerLicenseCoordinatesTask
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

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
            configureLicenseGate()
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

/**
 * License gate (T-001, ADR-0003): each module's `licenseDependencies` records external modules per scope,
 * `licenseReport` resolves their POM licenses, and `licenseCheck` applies the allow-list.
 * `-Ptaqvim.licenseGate.canary=true` injects a known LGPL artifact so CI can prove the gate fails.
 */
private fun Project.configureLicenseGate() {
    if (providers.gradleProperty("taqvim.licenseGate.canary").orNull == "true") addLicenseCanary()
    registerLicenseCoordinatesTask()
    val modules = allprojects.filter { it.buildFile.exists() }
    val report =
        tasks.register<LicenseReportTask>("licenseReport") {
            group = "verification"
            description = "Resolves the licenses of every external dependency of every module."
            dependsOn(
                modules.map {
                    if (it ==
                        it.rootProject
                    ) {
                        ":licenseDependencies"
                    } else {
                        "${it.path}:licenseDependencies"
                    }
                },
            )
            coordinateFiles.from(modules.map { it.layout.buildDirectory.file(LICENSE_COORDINATES_PATH) })
            reportFile.set(layout.buildDirectory.file("reports/licenses/license-report.json"))
        }
    tasks.register<LicenseCheckTask>("licenseCheck") {
        group = "verification"
        description = "Fails when a dependency license is not on config/license/allowed-licenses.json."
        reportFile.set(report.flatMap { it.reportFile })
        allowListFile.set(layout.projectDirectory.file("config/license/allowed-licenses.json"))
        resultFile.set(layout.buildDirectory.file("reports/licenses/license-check.txt"))
    }
}

private fun Project.addLicenseCanary() {
    val canaryScope = configurations.dependencyScope("licenseGateCanary")
    configurations.resolvable(ConfigurationClassifier.CANARY_CONFIGURATION) {
        extendsFrom(canaryScope.get())
        requestJvmRuntime(objects)
    }
    dependencies.add("licenseGateCanary", LICENSE_CANARY_DEPENDENCY)
}

/** LGPL-2.1-or-later on Maven Central; never added to a real module. */
private const val LICENSE_CANARY_DEPENDENCY = "org.mariadb.jdbc:mariadb-java-client:3.5.10"

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
