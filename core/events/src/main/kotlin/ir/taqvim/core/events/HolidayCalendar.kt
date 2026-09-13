/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday

/**
 * Holidays and weekends of days (T-303), from the holiday occurrences of [enabledSources] in an [EventLookup].
 *
 * The [weekend] is always a parameter: a language's CLDR weekend ([forLanguage]) or a set the user configured. Taqvim
 * ships no official weekend data of its own. Events of disabled sources never count as holiday reasons, even when
 * flagged [EventFlag.ALWAYS_DISPLAYED].
 */
public class HolidayCalendar(
    private val lookup: EventLookup,
    private val enabledSources: Set<EventSource>,
    public val weekend: Set<Weekday>,
) {
    /** The holiday occurrences of enabled sources on [jdn], in [EventLookup.DAY_ORDER]; empty on other days. */
    public fun holidayReasons(jdn: Jdn): List<Occurrence> =
        lookup.eventsOn(jdn, enabledSources).filter { it.isHoliday && it.definition.source in enabledSources }

    /** Whether [jdn] is a holiday of an enabled source. */
    public fun isHoliday(jdn: Jdn): Boolean = holidayReasons(jdn).isNotEmpty()

    /** Whether [jdn] falls on a [weekend] day. */
    public fun isWeekend(jdn: Jdn): Boolean = jdn.weekday() in weekend

    /** Whether [jdn] is neither a weekend day nor a holiday. */
    public fun isWorkday(jdn: Jdn): Boolean = !isWeekend(jdn) && !isHoliday(jdn)

    public companion object {
        /** A holiday calendar using [language]'s weekend from its CLDR region data (T-200). */
        public fun forLanguage(
            lookup: EventLookup,
            enabledSources: Set<EventSource>,
            language: LanguageSpec,
        ): HolidayCalendar = HolidayCalendar(lookup, enabledSources, language.weekend)
    }
}
