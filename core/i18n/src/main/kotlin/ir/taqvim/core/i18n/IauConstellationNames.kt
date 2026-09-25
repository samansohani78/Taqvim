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
 * There is no CLDR data for constellation names, and no per-language translation has been sourced from a national
 * astronomical society or academy yet (docs/DATA_TODO.md DT-026), so [name] returns the same Latin name for every
 * language today. A future per-language override, once sourced, belongs in a resource this object also reads —
 * mirroring how [HebrewMonthNames] layers a machine-translated resource over its CLDR one — without changing this
 * public API.
 */
public object IauConstellationNames {
    private const val RESOURCE = "iau-constellations.properties"

    private val entries: Map<String, String> by lazy { loadPropertiesResource(RESOURCE) }

    /** Every IAU abbreviation this table has a name for (88 entries). */
    public val abbreviations: Set<String> by lazy { entries.keys }

    /** The IAU's official Latin name of the constellation abbreviated [abbreviation] (e.g. "Sco" → "Scorpius"). */
    public fun name(abbreviation: String): String? = entries[abbreviation]
}
