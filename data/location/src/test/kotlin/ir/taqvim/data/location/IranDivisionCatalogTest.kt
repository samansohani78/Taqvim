/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * DT-021: the bundled GeoNames (CC BY 4.0) province/county list — counts checked against the source file (see the
 * provenance header of `iran-divisions.tsv`), parent/child links and lookup.
 */
class IranDivisionCatalogTest {
    private val bundled by lazy { IranDivisionCatalog.loadBundled() }

    @Test
    fun `the bundled list loads all 31 provinces and 435 counties`() {
        bundled.divisions shouldHaveSize PROVINCE_COUNT + COUNTY_COUNT
        bundled.provinces shouldHaveSize PROVINCE_COUNT
        bundled.divisions.count { it.level == IranDivisionLevel.COUNTY } shouldBe COUNTY_COUNT
        bundled.divisions.map(IranDivision::code).toSet() shouldHaveSize PROVINCE_COUNT + COUNTY_COUNT
    }

    @Test
    fun `every county resolves to a bundled province`() {
        val provinceCodes = bundled.provinces.map(IranDivision::code).toSet()
        bundled.divisions
            .filter { it.level == IranDivisionLevel.COUNTY }
            .forEach { county -> (county.parentCode in provinceCodes) shouldBe true }
    }

    @Test
    fun `Tehran province holds the Tehran and Shahriar counties`() {
        val tehran = bundled.division(TEHRAN_PROVINCE_CODE).shouldNotBeNull()
        tehran.level shouldBe IranDivisionLevel.PROVINCE
        tehran.englishName shouldBe "Tehran Province"
        tehran.name("fa") shouldBe "استان تهران"

        val counties = bundled.counties(TEHRAN_PROVINCE_CODE).map(IranDivision::englishName)
        counties shouldContain "Shahrestān-e Tehrān"
        counties shouldContain "Shahrestān-e Shahrīār"
    }

    @Test
    fun `an unknown code has no division and no counties`() {
        bundled.division("not-a-code").shouldBeNull()
        bundled.counties("not-a-code") shouldHaveSize 0
    }

    private companion object {
        const val PROVINCE_COUNT = 31
        const val COUNTY_COUNT = 435
        const val TEHRAN_PROVINCE_CODE = "26"
    }
}
