/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/**
 * Bikram Sambat month and era names (T-105). CLDR has no Bikram Sambat calendar, so they come from the hand-curated
 * `bikram-sambat.properties`, whose header cites them: Nepali names as printed by Nepal's official National Panchang,
 * and for every other language the Government of Nepal's own English spellings (owner decision 2026-09-16).
 */
internal object BikramSambatNames {
    private const val RESOURCE = "bikram-sambat.properties"
    private const val NEPALI = "ne"
    private const val MONTHS = 12

    private val entries: Map<String, String> by lazy { loadPropertiesResource(RESOURCE) }

    /** The 12 month names shown in language [code]: Nepali script for `ne`, the official Latin spellings otherwise. */
    fun months(code: String): List<String> {
        val key = if (code == NEPALI) "months.$NEPALI" else "months.latin"
        return requireNotNull(entries[key]) { "$RESOURCE has no '$key'" }.split('|').also { names ->
            require(names.size == MONTHS) { "$RESOURCE needs 12 names in '$key'" }
        }
    }

    /** The era abbreviation of language [code], or `null` when no official one is known. */
    fun era(code: String): String? = entries["era.$code"]

    /** The full date pattern of language [code], or `null` to use its Gregorian pattern. */
    fun pattern(code: String): String? = entries["pattern.$code"]
}
