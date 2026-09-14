/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.HolidayCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import org.junit.jupiter.api.Test

/**
 * ADR-0007 §3 addendum golden: with the generated dataset (D-02, D-03) and each language's first-run preferences, a
 * national official holiday marks a day only for the users whose default sources include that country.
 */
class DefaultEventSourcesGoldenTest {
    private val lookup = EventLookup(OfficialEvents.ALL)

    /** 28 Asad 1405, Afghanistan's Independence Day holiday announced for 2026-08-19 (D-03). */
    private val afghanIndependence = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 5, 28))

    /** 1 Farvardin 1405, Nowruz, an official holiday of Iran (D-02). */
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))

    private fun holidaySources(
        languageCode: String,
        jdn: Jdn,
    ): List<EventSource> {
        val sources =
            UserPreferences
                .defaultsFor(languageCode)
                .toEventsSettings()
                .preferences.enabledSources
        return HolidayCalendar(lookup, sources, weekend = emptySet()).holidayReasons(jdn).map { it.definition.source }
    }

    @Test
    fun `Afghan holidays are holidays for Dari and Pashto users only`() {
        afghanIndependence shouldBe GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2026, 8, 19))

        listOf("prs", "ps").forEach { code ->
            withClue(code) { holidaySources(code, afghanIndependence) shouldContain EventSource.AFGHANISTAN_OFFICIAL }
        }
        listOf("fa", "en", "ne", "ar", "tg").forEach { code ->
            withClue(code) {
                holidaySources(code, afghanIndependence) shouldNotContain EventSource.AFGHANISTAN_OFFICIAL
            }
        }
    }

    @Test
    fun `Iranian holidays are holidays for Persian users only`() {
        holidaySources("fa", nowruz) shouldContain EventSource.IRAN_OFFICIAL
        listOf("prs", "en", "ne").forEach { code ->
            withClue(code) { holidaySources(code, nowruz) shouldNotContain EventSource.IRAN_OFFICIAL }
        }
    }
}
