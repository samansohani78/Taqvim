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
    public fun holidayReasons(jdn: Jdn): List<Occurrence> = holidayReasonsIn(occurrencesOn(jdn))

    /**
     * The holiday occurrences among [occurrences], which must be the occurrences of one day from [occurrencesOn].
     *
     * A caller that already has a day's occurrences passes them rather than asking for them again: the assembler
     * reads the same list to decide what to show and whether the day is off, and looking it up twice per day was
     * the second most expensive thing it did.
     */
    public fun holidayReasonsIn(occurrences: List<Occurrence>): List<Occurrence> = occurrences.filter(::isHolidayReason)

    /** The occurrences of [jdn] from enabled sources, in [EventLookup.DAY_ORDER]. */
    public fun occurrencesOn(jdn: Jdn): List<Occurrence> = lookup.eventsOn(jdn, enabledSources)

    /**
     * Whether [occurrence] falls inside its definition's validity, read in the calendar the validity is written in.
     *
     * A holiday a law created or ended is a holiday only in the years the law covers: 8 Rabi al-Awwal became one in
     * AH 1440 and 2 Shawwal in AH 1433, so the official calendars of the years before print those days without
     * (تعطیل). Without this the day was reported as a holiday in every year — wrong on screen and in the workday and
     * distance arithmetic that builds on it. [EventVisibilityPolicy] applies the same rule to what is displayed;
     * holiday determination has to apply it too, because it does not go through that policy.
     */
    private fun withinValidity(occurrence: Occurrence): Boolean {
        val validity = occurrence.definition.validity ?: return true
        val calendar = lookup.calendarFor(occurrence.definition.source, validity.calendar) ?: return false
        return validity.contains(calendar.fromJdn(occurrence.jdn).year)
    }

    /** Whether [jdn] is a holiday of an enabled source. */
    public fun isHoliday(jdn: Jdn): Boolean = occurrencesOn(jdn).any(::isHolidayReason)

    /** Whether a day with these [occurrences] (from [occurrencesOn]) is a holiday. */
    public fun isHolidayIn(occurrences: List<Occurrence>): Boolean = occurrences.any(::isHolidayReason)

    /** Whether [occurrence] makes its day a holiday: a holiday of an enabled source, inside its validity. */
    private fun isHolidayReason(occurrence: Occurrence): Boolean =
        occurrence.isHoliday && occurrence.definition.source in enabledSources && withinValidity(occurrence)

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
