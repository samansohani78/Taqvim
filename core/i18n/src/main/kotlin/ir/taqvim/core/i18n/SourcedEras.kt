/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarSystem

/**
 * Era abbreviations for language/calendar pairs Unicode CLDR has none for (T-202, docs/DATA_TODO.md DT-008). They
 * come from the hand-curated `sourced-eras.properties`, whose header cites each one: today the Pashto solar and lunar
 * Hijri markers as printed on the covers of Afghanistan's Official Gazette. `formats.properties` stays generated CLDR
 * data; [FormatTable] folds these over it.
 */
internal object SourcedEras {
    private const val RESOURCE = "sourced-eras.properties"

    /** Every sourced era abbreviation, by language code and calendar. */
    val all: Map<Pair<String, CalendarSystem>, String> by lazy {
        loadPropertiesResource(RESOURCE).entries.associate { (key, era) ->
            val code = key.substringBefore('.')
            val name = key.substringAfter('.', "")
            val calendar =
                requireNotNull(CalendarSystem.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }) {
                    "$RESOURCE names an unknown calendar in '$key'"
                }
            (code to calendar) to era
        }
    }

    /** The era abbreviations of language [code], or an empty map when no official one is known for it. */
    fun of(code: String): Map<CalendarSystem, String> =
        all.filterKeys { (language, _) -> language == code }.mapKeys { it.key.second }
}
