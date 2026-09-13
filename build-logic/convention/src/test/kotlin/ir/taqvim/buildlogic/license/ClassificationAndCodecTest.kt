/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ClassificationAndCodecTest {
    private val table =
        listOf(
            Triple(":app", "releaseRuntimeClasspath", LicenseScope.RUNTIME),
            Triple(":app", "debugRuntimeClasspath", LicenseScope.RUNTIME),
            Triple(":app", "benchmarkRuntimeClasspath", LicenseScope.RUNTIME),
            Triple(":core:model", "runtimeClasspath", LicenseScope.RUNTIME),
            Triple(":core:model", "testRuntimeClasspath", LicenseScope.TEST),
            Triple(":core:ui", "debugUnitTestRuntimeClasspath", LicenseScope.TEST),
            Triple(":core:ui", "debugAndroidTestRuntimeClasspath", LicenseScope.TEST),
            Triple(":app", "debugScreenshotTestRuntimeClasspath", LicenseScope.TEST),
            Triple(":benchmark", "benchmarkRuntimeClasspath", LicenseScope.TEST),
            Triple(":konsist", "runtimeClasspath", LicenseScope.TEST),
            Triple(":lint", "runtimeClasspath", LicenseScope.BUILD),
            Triple(":tools:dataset", "runtimeClasspath", LicenseScope.BUILD),
            Triple(":data:database", "kspDebugProcessorClasspath", LicenseScope.BUILD),
            Triple(":tools:dataset", "annotationProcessor", LicenseScope.BUILD),
            Triple(":", ConfigurationClassifier.CANARY_CONFIGURATION, LicenseScope.RUNTIME),
            Triple(":app", "debugCompileClasspath", null),
            Triple(":app", "detektCliClasspath", null),
            Triple(":app", "kotlinCompilerPluginClasspathDebug", null),
        )

    @TestFactory
    fun `classifies configurations`() =
        table.map { (project, configuration, expected) ->
            DynamicTest.dynamicTest("$project $configuration") {
                ConfigurationClassifier.classify(project, configuration) shouldBe expected
            }
        }

    @Test
    fun `coordinates round-trip`() {
        val entries =
            mapOf("b:b:2" to setOf(LicenseScope.TEST, LicenseScope.RUNTIME), "a:a:1" to setOf(LicenseScope.BUILD))

        LicenseJsonCodec.decodeCoordinates(LicenseJsonCodec.encodeCoordinates(entries)) shouldBe entries
    }

    @Test
    fun `report round-trips including null fields`() {
        val report =
            listOf(
                DependencyLicenseInfo(
                    ModuleCoordinate("g", "a", "1"),
                    setOf(LicenseScope.RUNTIME),
                    listOf(DeclaredLicense("MIT", null, "MIT"), DeclaredLicense(null, "https://x", null)),
                ),
                DependencyLicenseInfo(ModuleCoordinate("g", "b", "2"), setOf(LicenseScope.TEST), emptyList()),
            )

        LicenseJsonCodec.decodeReport(LicenseJsonCodec.encodeReport(report)) shouldBe report
    }

    @Test
    fun `codec rejects malformed input`() {
        shouldThrow<IllegalArgumentException> { LicenseJsonCodec.decodeReport("{}") }
        shouldThrow<IllegalArgumentException> { LicenseJsonCodec.decodeReport("[1]") }
        shouldThrow<IllegalArgumentException> {
            LicenseJsonCodec.decodeCoordinates("""[{ "coordinate": "only:two", "scopes": [] }]""")
        }
    }

    @Test
    fun `module coordinates parse strictly`() {
        ModuleCoordinate.parse("g:a:1").toString() shouldBe "g:a:1"
        shouldThrow<IllegalArgumentException> { ModuleCoordinate.parse("g::1") }
        shouldThrow<IllegalArgumentException> { ModuleCoordinate.parse("g:a:1:x") }
    }
}
