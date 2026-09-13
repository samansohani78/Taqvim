/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** Kotlin compiler settings shared by JVM and Android modules. Warnings are errors (plan §8.1). */
internal fun Project.configureKotlinCompiler() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(TaqvimBuild.jvmTarget)
            allWarningsAsErrors.set(true)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

/** Every test task runs on the JUnit Platform (Jupiter + Kotest; Vintage for Robolectric/lint tests). */
internal fun Project.configureTestTasks() {
    pluginManager.apply("jvm-toolchains")
    val launcher =
        extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(TaqvimBuild.JAVA_RELEASE))
        }
    val limiter = forkedJvmLimiter()
    val updateSnapshots = providers.gradleProperty(UPDATE_SNAPSHOTS_PROPERTY).orElse("false")
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // One fork per test task, capped heap, and a build-wide cap on concurrent forks (ADR-0004 §9).
        maxParallelForks = 1
        maxHeapSize = TEST_JVM_HEAP
        usesService(limiter)
        // Robolectric supports JDKs up to 21; tests run on the same JDK 21 toolchain as detekt (ADR-0004 §8).
        javaLauncher.set(launcher)
        jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
        // Robolectric's SDK 36 FileDescriptor interceptor (ApplicationSharedMemory) needs these on any modern JDK.
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
        systemProperty("kotest.framework.dump.config", "false")
        systemProperty(UPDATE_SNAPSHOTS_PROPERTY, updateSnapshots.get())
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

/** Gradle and system property that lets `SnapshotVerifier` (core:testing) rewrite snapshots. */
private const val UPDATE_SNAPSHOTS_PROPERTY = "taqvim.updateSnapshots"

/** Base test dependencies for every module with Kotlin code. */
internal fun Project.addBaseTestDependencies() {
    val catalog = libs
    val selfIsTesting = path == ":core:testing"
    dependencies {
        "testImplementation"(platform(catalog.library("junit-bom")))
        "testImplementation"(catalog.library("junit-jupiter"))
        "testImplementation"(catalog.library("junit-jupiter-params"))
        "testImplementation"(catalog.library("kotest-assertions-core"))
        "testImplementation"(catalog.library("kotest-property"))
        "testRuntimeOnly"(catalog.library("junit-platform-launcher"))
        if (!selfIsTesting) {
            "testImplementation"(project(":core:testing"))
        }
    }
}
