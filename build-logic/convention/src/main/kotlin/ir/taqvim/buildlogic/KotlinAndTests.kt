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
import org.gradle.kotlin.dsl.register
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

/**
 * Every test task runs on the JUnit Platform (Jupiter + Kotest; Vintage for Robolectric/lint tests). Wall-clock
 * timing tests are excluded from all of them and run by [TIMING_TEST_TASK], which copies [unitTestTask]'s classpath.
 */
internal fun Project.configureTestTasks(unitTestTask: String = "test") {
    pluginManager.apply("jvm-toolchains")
    val launcher =
        extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(TaqvimBuild.JAVA_RELEASE))
        }
    val limiter = forkedJvmLimiter()
    val updateSnapshots = providers.gradleProperty(UPDATE_SNAPSHOTS_PROPERTY).orElse("false")
    val propertyIterations = providers.gradleProperty(PROPERTY_ITERATIONS_PROPERTY).orElse("1000")
    val timingScale = providers.gradleProperty(TIMING_SCALE_PROPERTY).orElse("1")
    tasks.withType<Test>().configureEach {
        val timing = name == TIMING_TEST_TASK
        useJUnitPlatform {
            if (timing) includeTags(*TIMING_TAGS) else excludeTags(*TIMING_TAGS)
        }
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
        // Fail instead of silently skipping mis-declared tests (e.g. a @Test function that returns a value).
        systemProperty("junit.platform.discovery.issue.severity.critical", "WARNING")
        systemProperty(UPDATE_SNAPSHOTS_PROPERTY, updateSnapshots.get())
        systemProperty(TIMING_SCALE_PROPERTY, timingScale.get())
        systemProperty(PROPERTY_ITERATIONS_PROPERTY, propertyIterations.get())
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    registerTimingTestTask(unitTestTask)
}

/**
 * Registers [TIMING_TEST_TASK]: [unitTestTask]'s test classes and classpath, only the timing tags, and a build-wide
 * one-at-a-time lock so no two modules' timing tests share the CPU (ADR-0018 addendum).
 */
private fun Project.registerTimingTestTask(unitTestTask: String) {
    val serializer = timingTestSerializer()
    tasks.register<Test>(TIMING_TEST_TASK) {
        group = "verification"
        description = "Runs this module's wall-clock timing tests alone; the default test tasks exclude them."
        // Modules without host tests (`:benchmark:micro`) have no unit test task, so nothing runs there.
        val source = tasks.withType<Test>().findByName(unitTestTask)
        testClassesDirs = source?.testClassesDirs ?: files()
        classpath = source?.classpath ?: files()
        // Most modules have no timing tests.
        failOnNoDiscoveredTests.set(false)
        usesService(serializer)
    }
}

/** Per-module task running only the timing tests; `timingTests` on the root runs all of them. */
internal const val TIMING_TEST_TASK = "timingTest"

/** Tags of timing tests: `TimingTest.TAG` (JUnit 5) and `TimingTest.CATEGORY_TAG` (JUnit 4 via Vintage), `:core:testing`. */
private val TIMING_TAGS = arrayOf("timing", "ir.taqvim.core.testing.TimingTest")

/** Gradle and system property that lets `SnapshotVerifier` (core:testing) rewrite snapshots. */
private const val UPDATE_SNAPSHOTS_PROPERTY = "taqvim.updateSnapshots"

/** Factor applied to the wall-clock budgets of timing tests (`TimingTest.budget`); CI passes a larger value. */
private const val TIMING_SCALE_PROPERTY = "taqvim.timingScale"

/** Property-test iteration budget: 1 000 by default, 10 000 nightly (plan §8.1; core:testing PropertyTesting). */
private const val PROPERTY_ITERATIONS_PROPERTY = "taqvim.propertyIterations"

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
