/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import org.junit.jupiter.api.Test

/** DT-021: the bundled `iran-divisions.tsv` table format and rejection of malformed input. */
class IranDivisionTableParserTest {
    private val header = "# columns: code,level,parentCode,en,fa,latitude,longitude"

    private fun parse(vararg lines: String): List<IranDivision> = IranDivisionTableParser.parse(lines.asSequence())

    @Test
    fun `a province row has no parentCode`() {
        val province =
            parse(header, "26\tPROVINCE\t\tTehran Province\tاستان تهران\t35.66667\t51.41667").single()

        province.code shouldBe "26"
        province.level shouldBe IranDivisionLevel.PROVINCE
        province.parentCode shouldBe null
        province.englishName shouldBe "Tehran Province"
        province.persianName shouldBe "استان تهران"
        province.coordinates shouldBe Coordinates(35.66667, 51.41667)
    }

    @Test
    fun `a county row carries its province code and an empty Persian name is null`() {
        val county =
            parse(header, "66736\tCOUNTY\t26\tBakhsh-e Shahriar\t\t35.66667\t51.05000").single()

        county.level shouldBe IranDivisionLevel.COUNTY
        county.parentCode shouldBe "26"
        county.persianName shouldBe null
        county.name("fa") shouldBe "Bakhsh-e Shahriar"
        county.name("en") shouldBe "Bakhsh-e Shahriar"
    }

    @Test
    fun `comment lines before the columns header are skipped`() {
        val divisions =
            parse(
                "# source: GeoNames",
                header,
                "26\tPROVINCE\t\tTehran Province\tاستان تهران\t35.66667\t51.41667",
            )

        divisions.size shouldBe 1
    }

    @Test
    fun `a row before the columns header is rejected`() {
        shouldThrow<IllegalArgumentException> {
            parse("26\tPROVINCE\t\tTehran Province\tاستان تهران\t35.66667\t51.41667", header)
        }
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
            parse(header, "26\tCITY\t\tTehran Province\tاستان تهران\t35.66667\t51.41667")
        }
    }

    @Test
    fun `a blank English name is rejected`() {
        shouldThrow<IllegalStateException> {
            parse(header, "26\tPROVINCE\t\t \tاستان تهران\t35.66667\t51.41667")
        }
    }

    @Test
    fun `an invalid coordinate is rejected`() {
        shouldThrow<IllegalArgumentException> {
            parse(header, "26\tPROVINCE\t\tTehran Province\tاستان تهران\tnot-a-number\t51.41667")
        }
    }
}
