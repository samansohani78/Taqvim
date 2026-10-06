/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.EventVisibilityPolicy
import ir.taqvim.core.events.OccurrenceCalculator
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * T-108/T-109: the Jewish observances and Christian movable feasts reach the calendar through the ordinary pipeline,
 * and are off until a user asks for them.
 */
class ReligiousObservancesTest {
    private val events = OfficialEvents.ALL
    private val calculator = OccurrenceCalculator(events, astronomy = SkyAstronomicalEventSource)
    private val tehran = TimeZone.of("Asia/Tehran")

    @Test
    fun `both sources are off for every language's defaults`() {
        // ADR-0007 §3: a source nobody asked for shows nothing. Neither is in any language's defaults, so the only
        // way to see them is the settings switch.
        val shown =
            LanguageTable.languages.flatMap { spec ->
                val preferences = UserPreferences.defaultsFor(spec).toEventsSettings(homeTimeZone = tehran)
                val policy = EventVisibilityPolicy(preferences.preferences)
                occurrencesOf(EventSource.JEWISH, 2026).filter { policy.isVisible(it, tehran) } +
                    occurrencesOf(EventSource.CHRISTIAN, 2026).filter { policy.isVisible(it, tehran) }
            }

        shown.shouldBeEmpty()
    }

    @Test
    fun `Easter 2026 is 5 April, the day USNO publishes`() {
        // The record is the engine's answer, not a typed date: the engine is checked against all 67 336 USNO
        // Christian observances, so this pins the wiring rather than the computus.
        val easter = occurrencesOf(EventSource.CHRISTIAN, 2026).single { it.definition.id.value == "christian.easter" }

        easter.date shouldBe CalendarDate(CalendarSystem.GREGORIAN, 2026, 4, 5)
    }

    @Test
    fun `Pesach keeps its Hebrew date in a leap year, where a fixed month number would not`() {
        // 5787 is a leap year: Adar I pushes Nisan from month 7 to month 8. A Fixed(7, 15) rule would land in Adar.
        val pesach = occurrencesOf(EventSource.JEWISH, 5787).single { it.definition.id.value == "jewish.pesach" }

        pesach.date.day shouldBe 15
        pesach.date.month shouldBe 8
    }

    @Test
    fun `every observance of both sources produces exactly one day a year`() {
        occurrencesOf(EventSource.JEWISH, 5787) shouldHaveSize 6
        occurrencesOf(EventSource.CHRISTIAN, 2026) shouldHaveSize 8
        occurrencesOf(EventSource.JEWISH, 5787).map { it.definition.id.value }.distinct() shouldHaveSize 6
    }

    @Test
    fun `none of them is a day off, because none is a public holiday anywhere the app claims to know`() {
        (occurrencesOf(EventSource.JEWISH, 5787) + occurrencesOf(EventSource.CHRISTIAN, 2026))
            .map { it.isHoliday }
            .distinct()
            .shouldContainExactly(listOf(false))
    }

    private fun occurrencesOf(
        source: EventSource,
        year: Int,
    ) = events.filter { it.source == source }.flatMap { calculator.occurrences(it, year) }
}
