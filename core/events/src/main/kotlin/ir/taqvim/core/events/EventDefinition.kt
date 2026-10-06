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

    /** The six Jewish observances of T-108, computed from the Hebrew calendar; off unless the user asks for them. */
    JEWISH,

    /** The Christian movable feasts of T-109, computed from the computus; off unless the user asks for them. */
    CHRISTIAN,
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
    /**
     * Who or where the observance applies to, or `null` for the whole country (DT-038).
     *
     * `null` is what every record meant before this existed, so nothing changes for one that does not carry it. The
     * app shows [EventScope.label] beside the event and never filters on it: it does not know which district a user
     * is in, and quietly hiding a holiday from someone who did not ask would be worse than naming its scope.
     */
    public val scope: EventScope? = null,
)

/** How wide an observance is (DT-038). */
public enum class EventScopeLevel {
    /** The whole country, which is also what no scope at all means. */
    NATIONWIDE,
    PROVINCE,
    DISTRICT,

    /** A geographic group that is not an administrative division, such as Nepal's hill and Terai districts. */
    REGION,

    /** A community or audience rather than a place: a people, a profession, an institution. */
    COMMUNITY,
}

/**
 * The scope of an observance (DT-038): how wide it is, which areas or audiences it names, and the text shown for it.
 *
 * [areas] are namespaced ids (`np.region.hill`, `np.community.newar`). They are not resolved to coordinates; they
 * exist so two records can say they mean the same group. [label] is what a reader sees.
 */
public data class EventScope(
    public val level: EventScopeLevel,
    public val label: LocalizedText,
    public val areas: List<String> = emptyList(),
)

/** One day on which [definition] occurs: [jdn], its [date] in the definition's calendar, and the rule [year]. */
public data class Occurrence(
    public val definition: EventDefinition,
    public val jdn: Jdn,
    public val date: CalendarDate,
    public val isHoliday: Boolean,
    public val year: Int,
)
