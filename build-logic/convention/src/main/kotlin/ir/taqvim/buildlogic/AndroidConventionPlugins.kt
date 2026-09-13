/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Shared Android configuration for application, library and test modules. */
internal fun Project.configureAndroidCommon(
    extension: CommonExtension,
    hostTests: Boolean = true,
) {
    val root = rootDirectory
    extension.apply {
        compileSdk {
            version = release(TaqvimBuild.COMPILE_SDK) { minorApiLevel = TaqvimBuild.COMPILE_SDK_MINOR }
        }
        defaultConfig.minSdk = TaqvimBuild.MIN_SDK
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.sourceCompatibility = TaqvimBuild.javaVersion
        compileOptions.targetCompatibility = TaqvimBuild.javaVersion
        lint.configureTaqvimLint(root)
        testOptions.unitTests.isIncludeAndroidResources = true
        packaging.resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*.md")
    }
    configureKotlinCompiler()
    if (hostTests) {
        configureTestTasks()
        addBaseTestDependencies()
        addRobolectricTestDependencies()
    }
}

private fun Project.addRobolectricTestDependencies() {
    val catalog = libs
    dependencies {
        "testImplementation"(catalog.library("junit4"))
        "testImplementation"(catalog.library("androidx-test-ext-junit"))
        "testImplementation"(catalog.library("robolectric"))
        "testRuntimeOnly"(catalog.library("junit-vintage-engine"))
    }
}

/** Android library (`:data:*`, `:core:ui`, `:core:ui-testing`, `:feature:*`). Release host tests are disabled. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            val android = extensions.getByType<LibraryExtension>()
            android.namespace = taqvimNamespace
            configureAndroidCommon(android)
            android.testOptions.targetSdk = TaqvimBuild.TARGET_SDK
            android.lint.targetSdk = TaqvimBuild.TARGET_SDK
            extensions.configure<LibraryAndroidComponentsExtension> {
                beforeVariants(selector().withBuildType("release")) { variant ->
                    variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
                }
            }
            pluginManager.apply(QualityConventionPlugin::class.java)
        }
    }
}

/** Android application (`:app`, `:wear`). Release is minified and resource-shrunk. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            val android = extensions.getByType<ApplicationExtension>()
            android.namespace = taqvimNamespace
            configureAndroidCommon(android)
            android.defaultConfig.targetSdk = TaqvimBuild.TARGET_SDK
            android.buildTypes.getByName("release").apply {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(android.getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                beforeVariants(selector().withBuildType("release")) { variant ->
                    variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
                }
            }
            pluginManager.apply(QualityConventionPlugin::class.java)
        }
    }
}

/** `com.android.test` module (`:benchmark`). */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")
            val android = extensions.getByType<TestExtension>()
            android.namespace = taqvimNamespace
            configureAndroidCommon(android, hostTests = false)
            android.defaultConfig.targetSdk = TaqvimBuild.TARGET_SDK
            pluginManager.apply(QualityConventionPlugin::class.java)
        }
    }
}
