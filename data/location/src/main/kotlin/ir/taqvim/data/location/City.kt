/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * A populated place from the bundled city list (T-603), compiled from Natural Earth "Populated Places" (public
 * domain) plus, for 8 languages Natural Earth does not publish (DT-020), alternate names cross-matched from the
 * GeoNames gazetteer (CC BY 4.0; see `about_source_geonames_attribution`).
 *
 * [localizedNames] holds the published names that differ from [englishName]. For a language in
 * [PUBLISHED_LANGUAGES], a missing entry means the source has no distinct name for that place (either the source's
 * name equals the English one, for a Natural Earth language, or GeoNames simply has none, for a DT-020 language);
 * other languages have no published names at all and [name] falls back.
 */
data class City(
    val id: Long,
    val englishName: String,
    val localizedNames: Map<String, String>,
    /** ISO 3166-1 alpha-2 code, or `null` where the source has none (e.g. disputed territories). */
    val countryCode: String?,
    /** First-level administrative region, in English. */
    val region: String?,
    val coordinates: Coordinates,
    /** IANA time zone id, or `null` where the source has none. */
    val timeZoneId: String?,
    /** Largest published population figure, or `null` when unknown. */
    val population: Long?,
) {
    /**
     * The name to show in [languageCode]: the published name; for languages without published names, the name in a
     * closely related language that shares its script and spelling ([NAME_FALLBACKS]); otherwise the English name.
     */
    fun name(languageCode: String): String {
        val source = if (languageCode in PUBLISHED_LANGUAGES) languageCode else NAME_FALLBACKS[languageCode]
        return source?.let(localizedNames::get) ?: englishName
    }

    /** Whether the source publishes a name in [languageCode] (otherwise [name] shows a fallback). */
    fun hasPublishedName(languageCode: String): Boolean = languageCode in PUBLISHED_LANGUAGES

    companion object {
        const val ENGLISH: String = "en"

        /** Languages of the 24 launch languages for which Natural Earth publishes place names. */
        private val NATURAL_EARTH_LANGUAGES: Set<String> =
            setOf(ENGLISH, "ar", "bn", "de", "es", "fa", "fr", "hi", "id", "ja", "ru", "tr", "ur", "zh")

        /**
         * Languages DT-020 cross-matched from GeoNames' `alternateNamesV2` export against this file's places (by
         * name and nearest coordinate in GeoNames' `cities500` gazetteer), for the languages Natural Earth has no
         * name field for at all. Coverage is sparse and partial by design (GeoNames' own coverage of each language
         * varies), which is why a missing entry still falls back to [englishName] rather than signalling an error.
         * Dari (`prs`) and Kurmanji (`kmr`) stay out of this set: GeoNames' own `prs` and `kmr` tags exist but matched
         * only 1 and 0 of this file's places respectively (docs/DATA_TODO.md DT-020), and adding either to this set
         * would silently turn off [NAME_FALLBACKS] for every other place in that language.
         */
        private val GEONAMES_LANGUAGES: Set<String> = setOf("ps", "ckb", "az", "ta", "tg", "uz", "ms", "ne")

        /** Languages of the 24 launch languages for which this file publishes place names. */
        val PUBLISHED_LANGUAGES: Set<String> = NATURAL_EARTH_LANGUAGES + GEONAMES_LANGUAGES

        /** Dari (`prs`) is written in the Persian script with Persian place-name spellings. */
        private val NAME_FALLBACKS: Map<String, String> = mapOf("prs" to "fa")
    }
}
