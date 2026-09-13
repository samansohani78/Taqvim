pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provisions the JDK 21 toolchain used to run detekt (ADR-0004 §6).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // cosinekitty/astronomy (MIT) is published only on JitPack; restrict JitPack to that group.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("io.github.cosinekitty") }
        }
    }
}

rootProject.name = "Taqvim"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":wear")
include(":lint")
include(":konsist")
include(":benchmark")
include(":tools:dataset")

listOf(
    "model",
    "calendar",
    "events",
    "praytimes",
    "astronomy",
    "i18n",
    "nlp",
    "ics",
    "workdays",
    "testing",
    "ui",
    "ui-testing",
).forEach { include(":core:$it") }

listOf(
    "preferences",
    "database",
    "events",
    "device-calendar",
    "location",
    "scheduler",
).forEach { include(":data:$it") }

listOf(
    "calendar",
    "timeline",
    "month",
    "year",
    "agenda",
    "events",
    "times",
    "astronomy",
    "map",
    "compass",
    "tools",
    "settings",
    "about",
    "widgets",
    "notification",
    "wallpaper",
    "search",
    "backup",
).forEach { include(":feature:$it") }
