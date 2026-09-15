plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
    // Plural categories and text direction per launch language for the translation checks (T-1702).
    testImplementation(projects.core.i18n)
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
