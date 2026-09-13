/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Build-wide constants shared by every convention plugin. */
internal object TaqvimBuild {
    const val MIN_SDK = 26
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 2
    const val TARGET_SDK = 37
    const val JAVA_RELEASE = 21
    val javaVersion: JavaVersion = JavaVersion.VERSION_21
    val jvmTarget: JvmTarget = JvmTarget.JVM_21
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Library '$alias' missing from libs.versions.toml") }

internal fun VersionCatalog.versionOf(alias: String): String =
    findVersion(alias)
        .orElseThrow { IllegalStateException("Version '$alias' missing from libs.versions.toml") }
        .requiredVersion

/** Root project directory, resolved without cross-project model access (Isolated Projects friendly). */
internal val Project.rootDirectory: Directory
    get() = isolated.rootProject.projectDirectory

/** Derives the Android namespace from the Gradle path, e.g. `:core:ui-testing` → `ir.taqvim.core.uitesting`. */
internal val Project.taqvimNamespace: String
    get() = "ir.taqvim" + path.replace(':', '.').replace("-", "")

/** True for modules under `:core:` (stricter coverage, explicit API). */
internal val Project.isCoreModule: Boolean
    get() = path.startsWith(":core:")
