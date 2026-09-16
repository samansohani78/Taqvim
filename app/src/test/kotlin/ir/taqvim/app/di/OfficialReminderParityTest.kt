/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.EventVisibilityPolicy
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * B04: every official reminder day equals the day the calendar display shows for that event, for every selectable
 * Islamic variant, across Gregorian and Persian year boundaries and the events' validity ranges.
 */
class OfficialReminderParityTest {
    private val from = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2025, 11, 1))
    private val until = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2027, 4, 30))

    @Test
    fun `official reminder days equal the displayed event days for every variant`() {
        IslamicVariant.entries.forEach { variant ->
            val arithmetic = UserPreferences.defaultsFor("fa").copy(islamicVariant = variant).availableArithmetic()
            val calendars = CalendarProvider { arithmetic[it] }
            val schedule = officialSchedule(OfficialEvents.ALL, "fa", variant, calendars)
            val displayed = displayedDays(variant, calendars)

            displayed.values.flatten().shouldNotBeEmpty()
            OfficialEvents.ALL.forEach { definition ->
                (variant to schedule.days(definition.id, from, until)) shouldBe
                    (variant to displayed[definition.id].orEmpty())
            }
        }
    }

    private fun displayedDays(
        variant: IslamicVariant,
        calendars: CalendarProvider,
    ): Map<EventId, List<Jdn>> {
        val selection = IslamicCalendarSelection(variant, base = calendars)
        val sources = EventSource.entries.toSet()
        val lookup = EventLookup(OfficialEvents.ALL, selection)
        val policy = EventVisibilityPolicy(EventPreferences(sources, TimeZone.UTC, islamicVariant = variant), selection)
        return (from.value..until.value)
            .flatMap { day -> policy.visible(lookup.eventsOn(Jdn(day), sources), TimeZone.UTC) }
            .groupBy({ it.definition.id }, { it.jdn })
            .mapValues { (_, days) -> days.distinct().sorted() }
    }
}
