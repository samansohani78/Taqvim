/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * CycloneDX SBOM of the shipped release runtime classpath, attached to every GitHub release
 * (T-003 `release.yml`; plan §12 DoD 5). Run `./gradlew :app:cyclonedxDirectBom`; the result is written to
 * `build/reports/cyclonedx-direct/bom.json` and `bom.xml`.
 */
internal fun Project.configureSbom() {
    pluginManager.apply("org.cyclonedx.bom")
    val moduleName = name
    val versionName = provider { extensions.getByType<ApplicationExtension>().defaultConfig.versionName ?: "0.0.0" }
    tasks.withType<CyclonedxDirectTask>().configureEach {
        notCompatibleWithConfigurationCache(
            "CycloneDX Gradle plugin 3.4.1 does not declare configuration-cache support",
        )
        includeConfigs.set(listOf("releaseRuntimeClasspath"))
        projectType.set(Component.Type.APPLICATION)
        componentName.set(moduleName)
        componentVersion.set(versionName)
        includeBuildEnvironment.set(false)
    }
}
