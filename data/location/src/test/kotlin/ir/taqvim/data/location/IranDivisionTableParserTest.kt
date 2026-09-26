/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import org.junit.jupiter.api.Test

/** DT-021/DT-020: the bundled `iran-divisions.tsv` table format and rejection of malformed input. */
class IranDivisionTableParserTest {
    private val columns =
        listOf("code", "level", "parentCode", "en", "latitude", "longitude") +
            IranDivision.PUBLISHED_LANGUAGES.sorted()
    private val header = "# columns: " + columns.joinToString(",")

    private fun parse(vararg lines: String): List<IranDivision> = IranDivisionTableParser.parse(lines.asSequence())

    private fun row(values: Map<String, String>): String = columns.joinToString("\t") { values[it].orEmpty() }

    private val tehranProvince =
        mapOf(
            "code" to "26",
            "level" to "PROVINCE",
            "en" to "Tehran Province",
            "fa" to "استان تهران",
            "latitude" to "35.66667",
            "longitude" to "51.41667",
        )

    @Test
    fun `a province row has no parentCode`() {
        val province = parse(header, row(tehranProvince)).single()

        province.code shouldBe "26"
        province.level shouldBe IranDivisionLevel.PROVINCE
        province.parentCode shouldBe null
        province.englishName shouldBe "Tehran Province"
        province.name("fa") shouldBe "استان تهران"
        province.coordinates shouldBe Coordinates(35.66667, 51.41667)
    }

    @Test
    fun `a county row carries its province code and an empty Persian name falls back to English`() {
        val county =
            parse(
                header,
                row(
                    mapOf(
                        "code" to "66736",
                        "level" to "COUNTY",
                        "parentCode" to "26",
                        "en" to "Bakhsh-e Shahriar",
                        "latitude" to "35.66667",
                        "longitude" to "51.05000",
                    ),
                ),
            ).single()

        county.level shouldBe IranDivisionLevel.COUNTY
        county.parentCode shouldBe "26"
        county.name("fa") shouldBe "Bakhsh-e Shahriar"
        county.name("en") shouldBe "Bakhsh-e Shahriar"
    }

    @Test
    fun `a county names in a DT-020 language`() {
        val county =
            parse(
                header,
                row(
                    mapOf(
                        "code" to "142549",
                        "level" to "COUNTY",
                        "parentCode" to "33",
                        "en" to "Tabriz",
                        "fa" to "تبریز",
                        "az" to "Təbriz",
                        "kmr" to "Tewrêz",
                        "latitude" to "38.08000",
                        "longitude" to "46.29194",
                    ),
                ),
            ).single()

        county.name("az") shouldBe "Təbriz"
        county.hasPublishedName("az") shouldBe true
        county.hasPublishedName("kmr") shouldBe true
        county.name("kmr") shouldBe "Tewrêz"
    }

    @Test
    fun `comment lines before the columns header are skipped`() {
        val divisions = parse("# source: GeoNames", header, row(tehranProvince))

        divisions.size shouldBe 1
    }

    @Test
    fun `a row before the columns header is rejected`() {
        shouldThrow<IllegalArgumentException> { parse(row(tehranProvince), header) }
    }

    @Test
    fun `a missing columns header is rejected`() {
        shouldThrow<IllegalArgumentException> { parse() }
    }

    @Test
    fun `an unexpected columns header is rejected`() {
        shouldThrow<IllegalArgumentException> { parse("# columns: code,level,en,fa,latitude,longitude") }
    }

    @Test
    fun `a row with the wrong number of fields is rejected`() {
        shouldThrow<IllegalArgumentException> { parse(header, "26\tPROVINCE\t\tTehran Province") }
    }

    @Test
    fun `an invalid level is rejected`() {
        shouldThrow<IllegalArgumentException> {
            parse(header, row(tehranProvince + ("level" to "CITY")))
        }
    }

    @Test
    fun `a blank English name is rejected`() {
        shouldThrow<IllegalStateException> {
            parse(header, row(tehranProvince + ("en" to " ")))
        }
    }

    @Test
    fun `an invalid coordinate is rejected`() {
        shouldThrow<IllegalArgumentException> {
            parse(header, row(tehranProvince + ("latitude" to "not-a-number")))
        }
    }
}
