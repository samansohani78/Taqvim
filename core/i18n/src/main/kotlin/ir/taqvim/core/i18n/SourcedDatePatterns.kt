/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarSystem

/**
 * Full date patterns for language/calendar pairs CLDR stores only a root fallback for (T-202, DT-025, ADR-0050).
 *
 * These are pinned strings, not a rule that reuses the language's current CLDR Gregorian pattern, because that rule
 * depended on a value CLDR is free to change — and CLDR 49 changes `ps` from `EEEE د y د MMMM d` to
 * `EEEE d, MMMM, y`, reintroducing the Latin-comma, day-first shape ADR-0014 and ADR-0050 exist to remove.
 * `sourced-date-patterns.properties` carries the evidence for each entry; `formats.properties` stays generated CLDR
 * data, and [FormatTable] folds these over it.
 */
internal object SourcedDatePatterns {
    private const val RESOURCE = "sourced-date-patterns.properties"

    /** Every pinned full date pattern, by language code and calendar. */
    val all: Map<Pair<String, CalendarSystem>, String> by lazy {
        loadPropertiesResource(RESOURCE).entries.associate { (key, pattern) ->
            val code = key.substringBefore('.')
            val name = key.substringAfter('.', "")
            val calendar =
                requireNotNull(CalendarSystem.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }) {
                    "$RESOURCE names an unknown calendar in '$key'"
                }
            (code to calendar) to pattern
        }
    }

    /** The pinned patterns of language [code], by calendar; empty when it has none. */
    fun of(code: String): Map<CalendarSystem, String> =
        all.filterKeys { it.first == code }.mapKeys { (key, _) -> key.second }
}
