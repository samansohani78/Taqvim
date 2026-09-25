/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/**
 * The IAU's own official Latin name of each of the 88 constellations (DT-026), keyed by the abbreviation the app's
 * astronomy façade returns (e.g. `Zodiac.moonConstellation`). Loaded from `iau-constellations.properties`, hand-curated
 * from the IAU's own published list (see that file's header for the citation).
 *
 * There is no CLDR data for constellation names. A national astronomical society or academy's own translation, once
 * verified for a language, is layered in from `iau-constellations-i18n.properties` (keyed `<language code>.<IAU
 * abbreviation>`; see that file's header for citations) — mirroring how [HebrewMonthNames] layers a second resource
 * over its CLDR one — and [name] falls back to the universal Latin name for every other language (docs/DATA_TODO.md
 * DT-026 stays open for those).
 */
public object IauConstellationNames {
    private const val RESOURCE = "iau-constellations.properties"
    private const val I18N_RESOURCE = "iau-constellations-i18n.properties"

    private val entries: Map<String, String> by lazy { loadPropertiesResource(RESOURCE) }
    private val localized: Map<String, String> by lazy { loadPropertiesResource(I18N_RESOURCE) }

    /** Every IAU abbreviation this table has a name for (88 entries). */
    public val abbreviations: Set<String> by lazy { entries.keys }

    /** The languages with a verified, sourced translation of at least one constellation name. */
    public val localizedLanguages: Set<String> by lazy {
        localized.keys.mapNotNull { it.substringBefore('.', "").ifEmpty { null } }.toSet()
    }

    /** The IAU's official Latin name of the constellation abbreviated [abbreviation] (e.g. "Sco" → "Scorpius"). */
    public fun name(abbreviation: String): String? = entries[abbreviation]

    /**
     * The constellation abbreviated [abbreviation] in language [languageCode]: a national authority's own name if
     * one has been sourced (see `iau-constellations-i18n.properties`), otherwise the universal Latin name.
     */
    public fun name(
        abbreviation: String,
        languageCode: String,
    ): String? = localized["$languageCode.$abbreviation"] ?: name(abbreviation)

    /** Whether [languageCode] has a sourced, non-Latin name for [abbreviation] rather than the Latin fallback. */
    public fun hasLocalizedName(
        abbreviation: String,
        languageCode: String,
    ): Boolean = localized.containsKey("$languageCode.$abbreviation")
}
