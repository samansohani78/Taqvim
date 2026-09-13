/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/** The calendars used for the events of each [EventSource] (e.g. the Islamic variant, T-302). */
public fun interface SourceCalendars {
    /** Calendar arithmetic for events of [source]. */
    public fun providerFor(source: EventSource): CalendarProvider

    public companion object {
        /** Every source uses [CalendarProvider.DEFAULT]. */
        public val DEFAULT: SourceCalendars = SourceCalendars { CalendarProvider.DEFAULT }
    }
}

/** Effectiveness of the year cache: lookups served from the cache ([hits]), computed ([misses]) and entries held. */
public data class CacheStats(
    public val hits: Long,
    public val misses: Long,
    public val size: Int,
)

/**
 * Day lookup over event occurrences (T-301). Occurrences are computed per (calendar system, year, enabled sources)
 * and kept in a thread-safe LRU cache of [capacity] entries. Events flagged [EventFlag.ALWAYS_DISPLAYED] are included
 * even when their source is disabled; visibility rules are applied by [EventVisibilityPolicy].
 */
public class EventLookup(
    definitions: Collection<EventDefinition>,
    sourceCalendars: SourceCalendars = SourceCalendars.DEFAULT,
    astronomy: AstronomicalEventSource? = null,
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val definitions: List<EventDefinition> = definitions.toList()
    private val calculators: Map<EventSource, OccurrenceCalculator> =
        EventSource.entries.associateWith {
            OccurrenceCalculator(this.definitions, sourceCalendars.providerFor(it), astronomy)
        }
    private val yearCalendars: Map<CalendarSystem, List<CalendarArithmetic>> =
        CalendarSystem.entries
            .associateWith { system ->
                EventSource.entries.mapNotNull { sourceCalendars.providerFor(it).calendarFor(system) }.distinct()
            }.filterValues { it.isNotEmpty() }
    private val lock = Any()
    private val cache =
        object : LinkedHashMap<YearKey, Map<Long, List<Occurrence>>>(capacity, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<YearKey, Map<Long, List<Occurrence>>>?) =
                size > capacity
        }
    private var hits = 0L
    private var misses = 0L

    init {
        require(capacity >= 1) { "cache capacity must be ≥ 1 (was $capacity)" }
    }

    /** Current cache statistics. */
    public val stats: CacheStats
        get() = synchronized(lock) { CacheStats(hits, misses, cache.size) }

    /** Occurrences of [system]-calendar events in [year] for [enabledSources], ordered by day then [DAY_ORDER]. */
    public fun occurrencesIn(
        system: CalendarSystem,
        year: Int,
        enabledSources: Set<EventSource>,
    ): List<Occurrence> =
        indexed(YearKey(system, year, enabledSources))
            .values
            .flatten()
            .sortedWith(compareBy<Occurrence> { it.jdn }.then(DAY_ORDER))

    /**
     * Occurrences on [jdn] from all available calendars for [enabledSources], holidays first, then by source and id.
     * Neighbouring years are consulted so rules whose offsets cross a year boundary are found.
     */
    public fun eventsOn(
        jdn: Jdn,
        enabledSources: Set<EventSource>,
    ): List<Occurrence> =
        yearCalendars
            .flatMap { (system, calendars) ->
                candidateYears(jdn, calendars).flatMap { year ->
                    indexed(YearKey(system, year, enabledSources))[jdn.value].orEmpty()
                }
            }.distinctBy { it.definition.id to it.jdn }
            .sortedWith(DAY_ORDER)

    private fun candidateYears(
        jdn: Jdn,
        calendars: List<CalendarArithmetic>,
    ): Set<Int> = calendars.flatMapTo(LinkedHashSet()) { calendar -> calendar.fromJdn(jdn).year.let { it - 1..it + 1 } }

    private fun indexed(key: YearKey): Map<Long, List<Occurrence>> =
        synchronized(lock) {
            val cached = cache[key]
            if (cached != null) {
                hits++
                cached
            } else {
                misses++
                compute(key).also { cache[key] = it }
            }
        }

    private fun compute(key: YearKey): Map<Long, List<Occurrence>> =
        definitions
            .filter { it.calendar == key.system && (it.source in key.enabledSources || it.isAlwaysDisplayed) }
            .flatMap { calculators.getValue(it.source).occurrences(it, key.year) }
            .groupBy { it.jdn.value }

    private val EventDefinition.isAlwaysDisplayed: Boolean
        get() = EventFlag.ALWAYS_DISPLAYED in flags

    private data class YearKey(
        val system: CalendarSystem,
        val year: Int,
        val enabledSources: Set<EventSource>,
    )

    public companion object {
        /** Cache entries kept by default (docs/PLAN.md T-301). */
        public const val DEFAULT_CAPACITY: Int = 64

        /** Order of events on one day: holidays first, then source, then id. */
        public val DAY_ORDER: Comparator<Occurrence> =
            compareByDescending<Occurrence> { it.isHoliday }
                .thenBy { it.definition.source }
                .thenBy { it.definition.id.value }

        private const val LOAD_FACTOR = 0.75f
    }
}
