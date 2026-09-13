plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
}

// Konsist's embedded Kotlin compiler pulls org.jetbrains.intellij.deps:trove4j (LGPL-2.1), which the license
// policy forbids even for tests. Konsist only parses sources and does not need it (ADR-0005).
configurations.matching { it.name.startsWith("test") }.configureEach {
    exclude(group = "org.jetbrains.intellij.deps", module = "trove4j")
}
