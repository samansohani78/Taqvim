/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.HebrewCalendarSystem
import ir.taqvim.core.calendar.IranCrescentCalendar
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import kotlinx.datetime.TimeZone

/**
 * The Islamic calendar per event source (T-302): [bySource] fixes a variant for some sources; every other source uses
 * the user's [preferredVariant]. Non-Islamic calendars come from [base]. [overrides] are the optional official Iranian
 * month starts the user switched on or imported (ADR-0037); without them every Islamic date is computed.
 */
public class IslamicCalendarSelection(
    public val preferredVariant: IslamicVariant,
    private val bySource: Map<EventSource, IslamicVariant> = DEFAULT_SOURCE_VARIANTS,
    private val base: CalendarProvider = CalendarProvider.DEFAULT,
    public val overrides: IslamicMonthTable? = null,
) : SourceCalendars {
    private val iranCalendar: CalendarArithmetic by lazy { iranCalendarFor(overrides) }

    /** The variant used for events of [source]. */
    public fun variantFor(source: EventSource): IslamicVariant = bySource[source] ?: preferredVariant

    /** Arithmetic for [variant] with this selection's [overrides]. */
    public fun calendarOf(variant: IslamicVariant): CalendarArithmetic =
        if (variant == IslamicVariant.IRAN_OFFICIAL) iranCalendar else calendarFor(variant)

    override fun providerFor(source: EventSource): CalendarProvider {
        val islamic = calendarOf(variantFor(source))
        return CalendarProvider { system ->
            val isIslamic = system == CalendarSystem.ISLAMIC
            if (isIslamic) islamic else base.calendarFor(system)
        }
    }

    public companion object {
        /** Product default: Iranian official events follow the Iranian official lunar calendar (ADR-0010). */
        public val DEFAULT_SOURCE_VARIANTS: Map<EventSource, IslamicVariant> =
            mapOf(EventSource.IRAN_OFFICIAL to IslamicVariant.IRAN_OFFICIAL)

        private val IRAN_COMPUTED_CALENDAR = IranIslamicCalendar()

        /**
         * The Iranian calendar: computed, with [overrides] applied when the user switched official dates on (ADR-0037).
         */
        public fun iranCalendarFor(overrides: IslamicMonthTable?): CalendarArithmetic =
            overrides?.let(::IranIslamicCalendar) ?: IRAN_COMPUTED_CALENDAR

        /**
         * Arithmetic for [variant], with [overrides] for the Iranian variant. The calculated-observational variant
         * (A-06) is the crescent calendar with the Iran calibration ([IranCrescentCalendar], ADR-0027); the Iranian
         * variant is the same computed calendar unless official months were switched on (ADR-0037).
         */
        public fun calendarFor(
            variant: IslamicVariant,
            overrides: IslamicMonthTable? = null,
        ): CalendarArithmetic =
            when (variant) {
                IslamicVariant.IRAN_OFFICIAL -> iranCalendarFor(overrides)
                IslamicVariant.UMM_AL_QURA -> UmmAlQuraCalendar
                IslamicVariant.TABULAR_16 -> TabularIslamicCalendar.TYPE_II
                IslamicVariant.TABULAR_15 -> TabularIslamicCalendar.TYPE_I
                IslamicVariant.CALCULATED_OBSERVATIONAL -> IranCrescentCalendar
            }

        /** Arithmetic of [system], with [variant] for the Islamic calendar; every calendar can be computed (T-105). */
        public fun arithmeticFor(
            system: CalendarSystem,
            variant: IslamicVariant,
            overrides: IslamicMonthTable? = null,
        ): CalendarArithmetic =
            when (system) {
                CalendarSystem.PERSIAN -> PersianCalendarSystem
                CalendarSystem.ISLAMIC -> calendarFor(variant, overrides)
                CalendarSystem.GREGORIAN -> GregorianCalendarSystem
                CalendarSystem.NEPALI -> NepaliCalendarSystem
                CalendarSystem.HEBREW -> HebrewCalendarSystem
            }
    }
}

/** User choices that decide which occurrences are shown (T-302). */
public data class EventPreferences(
    /** Sources whose events are shown. */
    public val enabledSources: Set<EventSource>,
    /** Home time zone for [hideReligiousOutsideHomeZone]. */
    public val homeTimeZone: TimeZone,
    /** Show only days off. */
    public val holidaysOnly: Boolean = false,
    /** Hide religious observances that are not holidays while the device is outside [homeTimeZone]. */
    public val hideReligiousOutsideHomeZone: Boolean = false,
    /** Islamic variant for sources without a fixed one. */
    public val islamicVariant: IslamicVariant = IslamicVariant.IRAN_OFFICIAL,
    /** Optional official Iranian month starts (ADR-0037); `null` means every Islamic date is computed. */
    public val islamicOverrides: IslamicMonthTable? = null,
)

/**
 * Data-driven visibility of occurrences (T-302), in this order: outside the event's validity → hidden (also when the
 * validity's calendar is unavailable); [EventFlag.ALWAYS_DISPLAYED] → shown; disabled source → hidden; holidays-only
 * and not a holiday → hidden; non-holiday religious observance while away from home (if enabled) → hidden.
 */
public class EventVisibilityPolicy(
    private val preferences: EventPreferences,
    /** Calendars per source, also to be used by the [EventLookup] feeding this policy. */
    public val calendars: SourceCalendars =
        IslamicCalendarSelection(preferences.islamicVariant, overrides = preferences.islamicOverrides),
) {
    /** Whether [occurrence] is shown while the device is in [currentTimeZone]. */
    public fun isVisible(
        occurrence: Occurrence,
        currentTimeZone: TimeZone,
    ): Boolean {
        val definition = occurrence.definition
        if (!withinValidity(occurrence)) return false
        if (EventFlag.ALWAYS_DISPLAYED in definition.flags) return true
        return definition.source in preferences.enabledSources &&
            (!preferences.holidaysOnly || occurrence.isHoliday) &&
            !hiddenAwayFromHome(occurrence, currentTimeZone)
    }

    /** The visible subset of [occurrences], order preserved. */
    public fun visible(
        occurrences: List<Occurrence>,
        currentTimeZone: TimeZone,
    ): List<Occurrence> = occurrences.filter { isVisible(it, currentTimeZone) }

    private fun hiddenAwayFromHome(
        occurrence: Occurrence,
        currentTimeZone: TimeZone,
    ): Boolean {
        val religiousObservance = occurrence.definition.category == EventCategory.RELIGIOUS && !occurrence.isHoliday
        val away = currentTimeZone.id != preferences.homeTimeZone.id
        return preferences.hideReligiousOutsideHomeZone && religiousObservance && away
    }

    private fun withinValidity(occurrence: Occurrence): Boolean {
        val validity = occurrence.definition.validity ?: return true
        val calendar =
            calendars.providerFor(occurrence.definition.source).calendarFor(validity.calendar) ?: return false
        return validity.contains(calendar.fromJdn(occurrence.jdn).year)
    }
}
