/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/** Stable event identifier, e.g. `ir.holiday.nowruz-1` (docs/PLAN.md §4.2). */
@JvmInline
public value class EventId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "event id must not be blank" }
    }
}

/** Where an event comes from; drives enablement and the Islamic variant used. */
public enum class EventSource {
    IRAN_OFFICIAL,
    AFGHANISTAN_OFFICIAL,
    NEPAL_OFFICIAL,
    INTERNATIONAL,
    ANCIENT_IRAN,
    USER,
}

/** Presentation group of an event. */
public enum class EventCategory {
    NATIONAL,
    RELIGIOUS,
    INTERNATIONAL,
    CULTURAL,
    ASTRONOMICAL,
    PERSONAL,
}

/** Optional behaviour markers of an event. */
public enum class EventFlag {
    /** Shown even when its source is disabled. */
    ALWAYS_DISPLAYED,

    /** Only half of the day is off. */
    HALF_DAY,
}

/**
 * Text in several languages, keyed by BCP 47 tag. Persian (`fa`) or, for Nepal's official records that carry only
 * their official Nepali title (ADR-0038), Nepali (`ne`) is mandatory.
 */
public data class LocalizedText(
    public val texts: Map<String, String>,
) {
    init {
        require(texts[PERSIAN].orEmpty().isNotBlank() || texts[NEPALI].orEmpty().isNotBlank()) {
            "a Persian (fa) or Nepali (ne) text is mandatory"
        }
        require(texts.values.all { it.isNotBlank() }) { "localized texts must not be blank" }
    }

    /** The text for [languageTag], falling back to Persian, then Nepali. */
    public fun forLanguage(languageTag: String): String = texts[languageTag] ?: texts[PERSIAN] ?: texts.getValue(NEPALI)

    public companion object {
        /** The mandatory language. */
        public const val PERSIAN: String = "fa"

        /** The mandatory language of records without a Persian text. */
        public const val NEPALI: String = "ne"
    }
}

/** A primary-source reference: [url], document [title] and optional [page]. */
public data class Citation(
    public val url: String,
    public val title: String,
    public val page: String? = null,
)

/** Years (in [calendar]) during which an event exists, from [fromYear] to [toYear] inclusive, with its [citation]. */
public data class Validity(
    public val calendar: CalendarSystem,
    public val fromYear: Int?,
    public val toYear: Int?,
    public val citation: Citation,
) {
    init {
        require(fromYear != null || toYear != null) { "validity needs fromYear or toYear" }
        require(
            fromYear == null || toYear == null || fromYear <= toYear,
        ) { "fromYear $fromYear is after toYear $toYear" }
    }

    /** Whether [year] lies within the validity. */
    public fun contains(year: Int): Boolean {
        val started = fromYear == null || year >= fromYear
        val notEnded = toYear == null || year <= toYear
        return started && notEnded
    }
}

/** An event and how to find its days (docs/PLAN.md §4.2). */
public data class EventDefinition(
    public val id: EventId,
    public val calendar: CalendarSystem,
    public val source: EventSource,
    public val category: EventCategory,
    public val isHoliday: Boolean,
    public val title: LocalizedText,
    public val rule: EventRule,
    public val validity: Validity? = null,
    public val flags: Set<EventFlag> = emptySet(),
    public val aliases: List<String> = emptyList(),
    public val citations: List<Citation> = emptyList(),
    public val links: Map<String, String> = emptyMap(),
)

/** One day on which [definition] occurs: [jdn], its [date] in the definition's calendar, and the rule [year]. */
public data class Occurrence(
    public val definition: EventDefinition,
    public val jdn: Jdn,
    public val date: CalendarDate,
    public val isHoliday: Boolean,
    public val year: Int,
)
