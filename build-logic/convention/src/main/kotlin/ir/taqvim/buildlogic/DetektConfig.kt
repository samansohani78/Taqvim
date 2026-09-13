/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Registers `detekt` as a cacheable [JavaExec] running the stable detekt CLI on a JDK 21 toolchain.
 *
 * detekt 1.23.x embeds the Kotlin 2.0 compiler, which cannot run on JDK 25 (it fails parsing the
 * runtime version), and its Gradle plugin always executes inside the Gradle daemon. Running the CLI in
 * a forked JDK 21 process keeps us on the latest *stable* detekt. See ADR-0004 §6.
 */
internal fun Project.configureDetekt() {
    pluginManager.apply("jvm-toolchains")
    val catalog = libs
    val detektKotlin = catalog.versionOf("detekt-kotlin")
    val cliScope = configurations.dependencyScope("detekt")
    val pluginScope = configurations.dependencyScope("detektPlugins")
    val cliClasspath =
        configurations.resolvable("detektCliClasspath") {
            extendsFrom(cliScope.get())
            configureDetektResolution(detektKotlin, objects)
        }
    val pluginClasspath =
        configurations.resolvable("detektPluginsClasspath") {
            extendsFrom(pluginScope.get())
            configureDetektResolution(detektKotlin, objects)
        }
    dependencies.add("detekt", catalog.library("detekt-cli"))

    val root = rootDirectory
    val configFile = root.file("config/detekt/detekt.yml")
    val reports = layout.buildDirectory.dir("reports/detekt")
    val sarifReport =
        "sarif:" +
            reports
                .get()
                .file("detekt.sarif")
                .asFile.absolutePath
    val htmlReport =
        "html:" +
            reports
                .get()
                .file("detekt.html")
                .asFile.absolutePath
    val sourceDir = layout.projectDirectory.dir("src")
    val launcher =
        extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(TaqvimBuild.JAVA_RELEASE))
        }
    val limiter = forkedJvmLimiter()
    val detekt =
        tasks.register<JavaExec>("detekt") {
            group = "verification"
            description = "Runs detekt over this module's Kotlin sources."
            javaLauncher.set(launcher)
            maxHeapSize = DETEKT_JVM_HEAP
            usesService(limiter)
            mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
            classpath = cliClasspath.get()
            inputs
                .files(fileTree(sourceDir) { include("**/*.kt", "**/*.kts") })
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .skipWhenEmpty()
            inputs.file(configFile).withPathSensitivity(PathSensitivity.RELATIVE)
            inputs.files(pluginClasspath).withNormalizer(ClasspathNormalizer::class.java)
            outputs.dir(reports)
            outputs.cacheIf { true }
            args("--input", sourceDir.asFile.absolutePath)
            args("--excludes", "**/build/**")
            args("--config", configFile.asFile.absolutePath, "--build-upon-default-config")
            args("--jvm-target", TaqvimBuild.JAVA_RELEASE.toString())
            args("--base-path", root.asFile.absolutePath)
            args("--report", sarifReport, "--report", htmlReport)
            // Wrap in a plain file collection: Configuration objects cannot be stored in the configuration cache.
            val plugins = objects.fileCollection().from(pluginClasspath)
            argumentProviders.add(
                CommandLineArgumentProvider {
                    if (plugins.isEmpty) emptyList() else listOf("--plugins", plugins.asPath)
                },
            )
        }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(detekt) }
}

private fun Configuration.configureDetektResolution(
    kotlinVersion: String,
    objects: ObjectFactory,
) {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM),
        )
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
    // Keep detekt on the Kotlin compiler it was built with; KGP alignment would otherwise upgrade it.
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion(kotlinVersion)
    }
}
