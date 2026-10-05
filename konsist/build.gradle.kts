plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
    // Plural categories and text direction per launch language for the translation checks (T-1702).
    testImplementation(projects.core.i18n)
}

// These tests read the whole repository at runtime (Konsist's project root), not their own classpath, so Gradle sees
// no inputs that change when the code they check changes. Without this the task is served FROM-CACHE and reports
// success on a tree it never read - a broken `config/i18n/same-as-source.txt` passed CI that way on 2026-10-05.
tasks.named<Test>("test") {
    inputs
        .files(
            fileTree(rootDir) {
                include("**/src/main/kotlin/**/*.kt", "**/src/main/res/values*/strings.xml")
                include("config/i18n/**", ".weblate", "app/build.gradle.kts")
                exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/.kotlin/**")
            },
        ).withPropertyName("repositorySources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// T-1702: `./gradlew :konsist:translationReport` writes per-language, per-module translation completeness to
// konsist/build/reports/translations (docs/i18n/TRANSLATING.md). The quality checks run in the regular test task.
tasks.register<Test>("translationReport") {
    group = "verification"
    description = "Writes the translation completeness report to build/reports/translations."
    val test = sourceSets["test"]
    testClassesDirs = test.output.classesDirs
    classpath = test.runtimeClasspath
    filter { includeTestsMatching("ir.taqvim.konsist.TranslationReportKonsistTest") }
    outputs.upToDateWhen { false }
}

// Konsist's embedded Kotlin compiler pulls org.jetbrains.intellij.deps:trove4j (LGPL-2.1), which the license
// policy forbids even for tests. Konsist only parses sources and does not need it (ADR-0005).
configurations.matching { it.name.startsWith("test") }.configureEach {
    exclude(group = "org.jetbrains.intellij.deps", module = "trove4j")
}
