/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSorted
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

/**
 * T-1504: the bundled license catalog is consistent, every license text is bundled verbatim, and the catalog equals the
 * runtime dependency set of the license gate report (T-001) whenever that report has been built.
 */
class LicenseCatalogTest {
    private val assets = File(requireNotNull(System.getProperty("taqvim.about.assets")) { "assets path not set" })
    private val report = File(requireNotNull(System.getProperty("taqvim.license.report")) { "report path not set" })

    private fun bundled(): LicenseCatalog =
        LicenseCatalogParser.parse(File(assets, LicenseCatalogParser.CATALOG_ASSET).readText())

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.strings(key: String): List<String> = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `bundled catalog lists runtime components with bundled license texts`() {
        val catalog = bundled()

        catalog.components.shouldNotBeEmpty()
        catalog.components.map { it.coordinate }.shouldBeSorted()
        catalog.components
            .map { it.coordinate }
            .toSet()
            .size shouldBe catalog.components.size
        catalog.licenses.forEach { license ->
            val asset = license.textAsset.shouldNotBeNull()
            File(assets, asset).readText().isNotBlank() shouldBe true
        }
        File(assets, "licenses/texts/Apache-2.0.txt").readText() shouldContain "Apache License"
        File(assets, requireNotNull(DataSource.UNICODE_LICENSE.textAsset)).readText() shouldContain "UNICODE LICENSE V3"
    }

    @Test
    fun `bundled catalog matches the license gate report when it exists`() {
        val expected =
            if (report.isFile) {
                Json
                    .parseToJsonElement(report.readText())
                    .jsonArray
                    .map { it.jsonObject }
                    .filter { dependency -> "RUNTIME" in dependency.strings("scopes") }
                    .associate { dependency ->
                        dependency.string("coordinate") to
                            dependency
                                .getValue("licenses")
                                .jsonArray
                                .map { it.jsonObject.string("spdx") }
                                .toSet()
                    }
            } else {
                // No report in this build: the asset must name its origin; CI checks staleness with the script.
                File(assets, LicenseCatalogParser.CATALOG_ASSET).readText() shouldContain "license-report.json"
                bundled().components.associate { it.coordinate to it.licenseIds.toSet() }
            }

        bundled().components.associate { it.coordinate to it.licenseIds.toSet() } shouldBe expected
    }

    @Test
    fun `parser rejects inconsistent catalogs`() {
        shouldThrow<IllegalArgumentException> {
            LicenseCatalogParser.parse("""{"licenses":[],"components":[{"coordinate":"a:b:1","licenses":["MIT"]}]}""")
        }
        shouldThrow<IllegalArgumentException> {
            LicenseCatalogParser.parse(
                """{"licenses":[{"id":"MIT","name":"MIT","url":null,"text":null}],""" +
                    """"components":[{"coordinate":"a:1","licenses":["MIT"]}]}""",
            )
        }
        shouldThrow<IllegalArgumentException> { LicenseCatalogParser.parse("""{"licenses":[]}""") }
    }

    @Test
    fun `groups join versions per module and leave out unused licenses`() {
        val unused = LicenseInfo("BSD-3-Clause", "BSD", null, null)
        val groups = AboutFixtures.catalog.copy(licenses = AboutFixtures.catalog.licenses + unused).groups()

        groups.map { it.license.id } shouldBe listOf("Apache-2.0", "MIT")
        groups.first().modules shouldBe listOf(ModuleRow("androidx.core:core", "1.13.1, 1.16.0"))
        groups.last().modules shouldBe listOf(ModuleRow("io.github.cosinekitty:astronomy", "2.1.19"))
    }
}
