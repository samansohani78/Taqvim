/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/** The two administrative levels published for Iran's country divisions (DT-021); see [IranDivision]. */
enum class IranDivisionLevel {
    /** An ostan (استان). */
    PROVINCE,

    /** A shahrestan (شهرستان) — a county within a [PROVINCE]. */
    COUNTY,
}

/**
 * A province or county of Iran's country divisions (T-603, T-1502), compiled from the GeoNames gazetteer (CC BY
 * 4.0; DT-021). A district level (bakhsh) is not published: GeoNames' Iran coverage of it is 7 records, far short
 * of the real count, and no other openly licensed source with full coverage was found (docs/DATA_TODO.md DT-021).
 */
data class IranDivision(
    /** GeoNames' admin1 code for a [IranDivisionLevel.PROVINCE], or its geonameid for a [IranDivisionLevel.COUNTY]. */
    val code: String,
    val level: IranDivisionLevel,
    /** The containing province's [code], or `null` for a [IranDivisionLevel.PROVINCE]. */
    val parentCode: String?,
    val englishName: String,
    /** The published Persian name, or `null` for the 3 of 435 counties GeoNames does not name in Persian. */
    val persianName: String?,
    val coordinates: Coordinates,
) {
    init {
        require((level == IranDivisionLevel.PROVINCE) == (parentCode == null)) {
            "a province has no parentCode and a county has one (level=$level, parentCode=$parentCode)"
        }
    }

    /** [persianName] for `fa` and Dari (`prs`, written in the Persian script); [englishName] otherwise. */
    fun name(languageCode: String): String =
        if (languageCode == "fa" || languageCode == "prs") persianName ?: englishName else englishName
}
