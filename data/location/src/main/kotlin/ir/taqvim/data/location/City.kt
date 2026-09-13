/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * A populated place from the bundled city list (T-603), compiled from Natural Earth "Populated Places" (public domain).
 *
 * [localizedNames] holds the published names that differ from [englishName]. For a language in
 * [PUBLISHED_LANGUAGES], a missing entry means the source's name equals the English one; other languages have no
 * published names and [name] falls back.
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
        val PUBLISHED_LANGUAGES: Set<String> =
            setOf(ENGLISH, "ar", "bn", "de", "es", "fa", "fr", "hi", "id", "ja", "ru", "tr", "ur", "zh")

        /** Dari (`prs`) is written in the Persian script with Persian place-name spellings. */
        private val NAME_FALLBACKS: Map<String, String> = mapOf("prs" to "fa")
    }
}
