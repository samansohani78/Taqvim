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
 * 4.0; DT-021, DT-020). A district level (bakhsh) is not published: GeoNames' Iran coverage of it is 7 records, far
 * short of the real count, and no other openly licensed source with full coverage was found (docs/DATA_TODO.md
 * DT-021).
 */
data class IranDivision(
    /** GeoNames' admin1 code for a [IranDivisionLevel.PROVINCE], or its geonameid for a [IranDivisionLevel.COUNTY]. */
    val code: String,
    val level: IranDivisionLevel,
    /** The containing province's [code], or `null` for a [IranDivisionLevel.PROVINCE]. */
    val parentCode: String?,
    val englishName: String,
    /**
     * The published names that differ from [englishName], keyed by language code. A language in [PUBLISHED_LANGUAGES]
     * with no entry here means GeoNames has no name in that language for this division (3 of 435 counties have no
     * Persian name, and DT-020's 8 added languages only name the provinces and a handful of counties); other
     * languages have no published names at all and [name] falls back.
     */
    val localizedNames: Map<String, String>,
    val coordinates: Coordinates,
) {
    init {
        require((level == IranDivisionLevel.PROVINCE) == (parentCode == null)) {
            "a province has no parentCode and a county has one (level=$level, parentCode=$parentCode)"
        }
    }

    /**
     * The name to show in [languageCode]: the published name; for Dari (`prs`, written in the Persian script and
     * sharing Persian place-name spellings) the Persian name; otherwise [englishName].
     */
    fun name(languageCode: String): String {
        val source = if (languageCode in PUBLISHED_LANGUAGES) languageCode else NAME_FALLBACKS[languageCode]
        return source?.let(localizedNames::get) ?: englishName
    }

    /** Whether the source publishes a name in [languageCode] (otherwise [name] shows a fallback). */
    fun hasPublishedName(languageCode: String): Boolean = languageCode in PUBLISHED_LANGUAGES

    companion object {
        const val ENGLISH: String = "en"
        const val PERSIAN: String = "fa"

        /**
         * Persian, from GeoNames' Iran alternate-names dump directly, plus the 9 languages DT-020 cross-matched from
         * the same dump for at least this file's 31 provinces (coverage of the 435 counties is far sparser, and
         * `ne` names only 1 of them; see this table's generator, `tools/geodata/geonames_iran_divisions.py`).
         * Kurmanji (`kmr`) comes from the Latin-script half of GeoNames' generic `ku` tag, Central Kurdish (`ckb`)
         * from its own tag; Dari (`prs`) is not published here and falls back to the Persian name via
         * [NAME_FALLBACKS], which is the right answer rather than a gap — Dari and Iranian Persian write these
         * names identically, and GeoNames has no Dari-tagged name for any Iran division (docs/DATA_TODO.md DT-020).
         */
        val PUBLISHED_LANGUAGES: Set<String> =
            setOf(PERSIAN, "ps", "ckb", "kmr", "az", "ta", "tg", "uz", "ms", "ne")

        /** Dari (`prs`) is written in the Persian script with Persian place-name spellings. */
        private val NAME_FALLBACKS: Map<String, String> = mapOf("prs" to PERSIAN)
    }
}
