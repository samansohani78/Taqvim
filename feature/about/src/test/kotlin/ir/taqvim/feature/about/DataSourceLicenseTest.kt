/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

/**
 * ADR-0039: every license shown in About > Data sources is one the repository allows for bundled data, and every
 * source under a license that obliges us to credit its creators carries an attribution line.
 */
class DataSourceLicenseTest {
    private val allowList = File(requireNotNull(System.getProperty("taqvim.license.allowlist")) { "path not set" })

    private data class DataLicenseRule(
        val id: String,
        val attributionRequired: Boolean,
    )

    private fun rules(): List<DataLicenseRule> =
        Json
            .parseToJsonElement(allowList.readText())
            .jsonObject
            .getValue("dataLicenses")
            .jsonArray
            .map { entry ->
                DataLicenseRule(
                    id =
                        entry.jsonObject
                            .getValue("id")
                            .jsonPrimitive.content,
                    attributionRequired =
                        entry.jsonObject
                            .getValue("attribution")
                            .jsonPrimitive.content == "required",
                )
            }

    @Test
    fun `every data license is allowed for bundled data`() {
        val allowed = rules().map { it.id }

        allowed.shouldNotBeEmpty()
        DataLicense.entries.forEach { license ->
            withClue(license) { allowed.contains(license.spdxId) shouldBe true }
        }
    }

    @Test
    fun `sources under an attribution license carry a credit line`() {
        val attributionRequired = rules().filter { it.attributionRequired }.map { it.id }.toSet()

        DataSource.entries
            .filter { it.license.spdxId in attributionRequired }
            .shouldNotBeEmpty()
            .forEach { source -> withClue(source) { source.attribution.shouldNotBeNull() } }
    }

    @Test
    fun `share-alike and non-commercial data licenses stay forbidden`() {
        // ADR-0039 admits attribution-only licenses; anything with an SA or NC term would infect the app's own data.
        rules().forEach { rule ->
            withClue(rule.id) {
                (rule.id.contains("-SA") || rule.id.contains("-NC")) shouldBe false
            }
        }
    }

    @Test
    fun `the plate data carries the full CC BY attribution`() {
        val license = DataSource.MATTHEWS_PLATES.license

        license.spdxId shouldBe "CC-BY-4.0"
        license.deedUrl shouldBe "https://creativecommons.org/licenses/by/4.0/"
        DataSource.MATTHEWS_PLATES.attribution.shouldNotBeNull()
    }
}
