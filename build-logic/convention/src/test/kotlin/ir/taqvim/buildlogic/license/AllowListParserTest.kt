/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test

class AllowListParserTest {
    private val defaultLicenses =
        """[{ "id": "Apache-2.0", "scopes": ["runtime", "build", "test"] }, { "id": "EPL-2.0", "scopes": ["TEST"] }]"""

    private fun doc(
        licenses: String = defaultLicenses,
        overrides: String = "[]",
        schemaVersion: String = "1",
        dataLicenses: String? = null,
    ) = """{ "schemaVersion": $schemaVersion, "licenses": $licenses, "overrides": $overrides""" +
        (dataLicenses?.let { """, "dataLicenses": $it""" } ?: "") + " }"

    private fun failure(json: String): String =
        shouldThrow<IllegalArgumentException> { AllowListParser.parse(json) }.message.orEmpty()

    @Test
    fun `parses the repository allow-list`() {
        val allowList = AllowListParser.parse(File("../../config/license/allowed-licenses.json").readText())

        allowList.permits("Apache-2.0", setOf(LicenseScope.RUNTIME, LicenseScope.TEST)) shouldBe true
        allowList.permits("EPL-2.0", setOf(LicenseScope.TEST)) shouldBe true
        allowList.permits("EPL-2.0", setOf(LicenseScope.RUNTIME)) shouldBe false
        allowList.licenses.keys.none { it.contains("GPL") || it.startsWith("MPL") } shouldBe true
    }

    @Test
    fun `the repository allow-list admits CC BY for data only`() {
        val allowList = AllowListParser.parse(File("../../config/license/allowed-licenses.json").readText())

        // ADR-0039: attribution-only data licenses are allowed for bundled data, never for a code dependency.
        allowList.dataLicense("CC-BY-4.0")?.attributionRequired shouldBe true
        allowList.dataLicense("LicenseRef-Public-Domain")?.attributionRequired shouldBe false
        allowList.licenses.keys.none { it.startsWith("CC-BY") } shouldBe true
        allowList.dataLicenses.map { it.id }.none { it.contains("SA") || it.contains("NC") } shouldBe true
    }

    @Test
    fun `parses data licenses and defaults them to empty`() {
        val json = """[{ "id": "CC-BY-4.0", "attribution": "required", "note": " plates " }]"""

        AllowListParser.parse(doc(dataLicenses = json)).dataLicenses shouldContainExactly
            listOf(DataLicenseEntry("CC-BY-4.0", attributionRequired = true, note = "plates"))
        AllowListParser.parse(doc()).dataLicenses shouldBe emptyList()
        AllowListParser.parse(doc(dataLicenses = json)).dataLicense("MIT") shouldBe null
    }

    @Test
    fun `rejects invalid data licenses`() {
        failure(doc(dataLicenses = "{}")) shouldContain "'dataLicenses' must be an array"
        failure(doc(dataLicenses = "[1]")) shouldContain "dataLicenses[0] must be an object"
        failure(doc(dataLicenses = """[{ "attribution": "none" }]""")) shouldContain "dataLicenses[0].id is required"
        failure(
            doc(dataLicenses = """[{ "id": "CC-BY-4.0", "attribution": "maybe" }]"""),
        ) shouldContain "dataLicenses[0].attribution must be one of required, none"
        failure(
            doc(
                dataLicenses =
                    """[{ "id": "CC0-1.0", "attribution": "none" }, { "id": "CC0-1.0", "attribution": "none" }]""",
            ),
        ) shouldContain "Duplicate data license id 'CC0-1.0'"
    }

    @Test
    fun `parses scopes case-insensitively and overrides with defaults`() {
        val overrides =
            """[{ "module": "com.example:lib", "license": "Apache-2.0", "evidence": "https://example.com/LICENSE" }]"""

        val allowList = AllowListParser.parse(doc(overrides = overrides))

        allowList.licenses["EPL-2.0"] shouldBe setOf(LicenseScope.TEST)
        allowList.overrides shouldContainExactly
            listOf(LicenseOverride("com.example:lib", "Apache-2.0", "https://example.com/LICENSE"))
    }

    @Test
    fun `keeps an explicit override version`() {
        val overrides =
            """[{ "module": "a:b", "license": "Apache-2.0", "evidence": "https://x", "versions": " 1.2 " }]"""

        AllowListParser
            .parse(doc(overrides = overrides))
            .overrides
            .single()
            .versions shouldBe "1.2"
    }

    @Test
    fun `missing overrides array means no overrides`() {
        val json = """{ "schemaVersion": 1, "licenses": [{ "id": "MIT", "scopes": ["runtime"] }] }"""

        AllowListParser.parse(json).overrides shouldBe emptyList()
    }

    @Test
    fun `rejects malformed documents`() {
        failure("not json") shouldContain "not valid JSON"
        failure("[]") shouldContain "root must be a JSON object"
        failure(doc(schemaVersion = "2")) shouldContain "schemaVersion"
        failure(doc(licenses = "{}")) shouldContain "'licenses' must be an array"
        failure(doc(licenses = "[]")) shouldContain "must not be empty"
        failure(doc(licenses = "[1]")) shouldContain "licenses[0] must be an object"
        failure(doc(licenses = """[{ "scopes": ["test"] }]""")) shouldContain "licenses[0].id is required"
        failure(
            doc(licenses = """[{ "id": "MIT", "scopes": ["test"] }, { "id": "MIT", "scopes": ["test"] }]"""),
        ) shouldContain
            "Duplicate"
        failure(doc(licenses = """[{ "id": "MIT", "scopes": "test" }]""")) shouldContain "scopes must be an array"
        failure(doc(licenses = """[{ "id": "MIT", "scopes": [] }]""")) shouldContain "scopes must not be empty"
        failure(doc(licenses = """[{ "id": "MIT", "scopes": ["shipping"] }]""")) shouldContain
            "unknown scope 'shipping'"
    }

    @Test
    fun `rejects invalid overrides`() {
        fun override(body: String) = failure(doc(overrides = "[$body]"))

        failure(doc(overrides = "{}")) shouldContain "'overrides' must be an array"
        override("1") shouldContain "overrides[0] must be an object"
        override("""{ "module": "no-colon", "license": "Apache-2.0", "evidence": "https://x" }""") shouldContain
            "group:name"
        override("""{ "module": "a:b", "license": "GPL-3.0-only", "evidence": "https://x" }""") shouldContain
            "not an allowed license id"
        override("""{ "module": "a:b", "license": "Apache-2.0", "evidence": "http://x" }""") shouldContain "https://"
    }
}
