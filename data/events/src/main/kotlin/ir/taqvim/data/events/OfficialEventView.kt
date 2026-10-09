/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.events.AstronomicalEventSource
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventVisibilityPolicy
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.events.Occurrence
import ir.taqvim.data.events.generated.OfficialEvents
import kotlinx.datetime.TimeZone

/**
 * The calendars and the visibility rule of one [EventsSettings] value — what every surface showing dataset events
 * must agree on (T-302).
 *
 * Built once here rather than per screen, because the two things are easy to forget and were: the calendar screen
 * assembled its days through [IslamicCalendarSelection] and [EventVisibilityPolicy], while search and the workday
 * calculator each built a bare `EventLookup` with the default calendars and no policy at all. That made Afghan
 * official events fall back to the computed Iranian calendar instead of the tabular one they are announced in
 * (ADR-0010), ignored a user's official Iranian month overrides (ADR-0037), and let search answer with observances
 * the user had switched off.
 */
class OfficialEventView(
    val settings: EventsSettings,
    definitions: List<EventDefinition> = OfficialEvents.ALL,
    astronomy: AstronomicalEventSource? = SkyAstronomicalEventSource,
) {
    /** The Islamic calendar each source uses, with the user's variant and overrides. */
    val calendars: IslamicCalendarSelection =
        IslamicCalendarSelection(
            settings.preferences.islamicVariant,
            overrides = settings.preferences.islamicOverrides,
        )

    /** Occurrences computed with [calendars]. */
    val lookup: EventLookup = EventLookup(definitions, calendars, astronomy)

    /** The one gate: sources, categories, holidays-only and validity. */
    val policy: EventVisibilityPolicy = EventVisibilityPolicy(settings.preferences, calendars)

    /** Whether [occurrence] is shown to this user, in [zone]. */
    fun isVisible(
        occurrence: Occurrence,
        zone: TimeZone,
    ): Boolean = policy.isVisible(occurrence, zone)

    /**
     * Whether any occurrence of [definition] in the years around [year] is shown.
     *
     * Search answers with definitions rather than days, so it asks the policy about the occurrence the user would
     * actually reach. A definition whose every occurrence is hidden — by its source, its category, holidays-only or
     * its validity — is not a result.
     */
    fun isVisibleSomewhen(
        definition: EventDefinition,
        year: Int,
        zone: TimeZone,
    ): Boolean =
        (year..year + 1).any { candidate ->
            lookup
                .occurrencesIn(definition.calendar, candidate, settings.preferences.enabledSources)
                .any { it.definition.id == definition.id && isVisible(it, zone) }
        }
}
