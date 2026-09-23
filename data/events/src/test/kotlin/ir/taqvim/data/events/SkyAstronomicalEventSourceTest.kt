/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.shouldBe
import ir.taqvim.core.astronomy.MoonQuarter
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.AstroKind
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.OccurrenceCalculator
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarSystem
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * ADR-0044: [SkyAstronomicalEventSource] over `:core:astronomy`'s [Sky] façade, and the `Astronomical`/`FULL_MOON`
 * rule this ADR unblocks, exercised end to end against an [EventDefinition] shaped exactly like the shipped
 * `un.vesak-day` record (`dataset/international/un-international-days.json`, ADR-0044 §5).
 */
class SkyAstronomicalEventSourceTest {
    private val from = Instant.parse("2020-01-01T00:00:00Z")
    private val until = Instant.parse("2031-01-01T00:00:00Z")

    @Test
    fun `equinoxes and solstices match Sky seasons`() {
        val kinds =
            mapOf(
                AstroKind.MARCH_EQUINOX to Season.MARCH_EQUINOX,
                AstroKind.JUNE_SOLSTICE to Season.JUNE_SOLSTICE,
                AstroKind.SEPTEMBER_EQUINOX to Season.SEPTEMBER_EQUINOX,
                AstroKind.DECEMBER_SOLSTICE to Season.DECEMBER_SOLSTICE,
            )
        kinds.forEach { (astroKind, season) ->
            val expected = (2020..2030).map { Sky.seasons(it).of(season) }
            SkyAstronomicalEventSource.instants(astroKind, from, until) shouldBe expected
        }
    }

    @Test
    fun `NEW_MOON and FULL_MOON match Sky moonQuarters`() {
        val quarters = Sky.moonQuarters(from, until)
        SkyAstronomicalEventSource.instants(AstroKind.NEW_MOON, from, until) shouldBe
            quarters.filter { it.quarter == MoonQuarter.NEW_MOON }.map { it.instant }
        SkyAstronomicalEventSource.instants(AstroKind.FULL_MOON, from, until) shouldBe
            quarters.filter { it.quarter == MoonQuarter.FULL_MOON }.map { it.instant }
    }

    @TestFactory
    fun `Vesak's rule produces the day of the full moon in May, 2020 to 2030`(): List<DynamicTest> {
        // Every date below is the UTC calendar date of that year's May (or, in 2026, earlier May) full moon in
        // core/astronomy/src/test/resources/golden/usno/moon-phases-1700-2100.csv (checked against USNO by
        // UsnoMoonPhasesTest); 2026 alone has two full moons in May (1 May 17:23Z and 31 May 08:45Z) and ADR-0044
        // keeps the earlier one.
        val expected =
            mapOf(
                2020 to LocalDate(2020, 5, 7),
                2021 to LocalDate(2021, 5, 26),
                2022 to LocalDate(2022, 5, 16),
                2023 to LocalDate(2023, 5, 5),
                2024 to LocalDate(2024, 5, 23),
                2025 to LocalDate(2025, 5, 12),
                2026 to LocalDate(2026, 5, 1),
                2027 to LocalDate(2027, 5, 20),
                2028 to LocalDate(2028, 5, 8),
                2029 to LocalDate(2029, 5, 27),
                2030 to LocalDate(2030, 5, 17),
            )
        val calculator = OccurrenceCalculator(listOf(vesak), astronomy = SkyAstronomicalEventSource)
        return expected.map { (year, date) ->
            DynamicTest.dynamicTest("$year") {
                calculator.occurrences(vesak, year).map { it.jdn.toLocalDate() } shouldBe listOf(date)
            }
        }
    }

    private companion object {
        /** Shaped like the shipped `un.vesak-day` record, so this test exercises the rule and its citation together. */
        val vesak =
            EventDefinition(
                id = EventId("un.vesak-day"),
                calendar = CalendarSystem.GREGORIAN,
                source = EventSource.INTERNATIONAL,
                category = EventCategory.INTERNATIONAL,
                isHoliday = false,
                title =
                    LocalizedText(
                        mapOf(
                            "en" to "Vesak, the Day of the Full Moon",
                            LocalizedText.PERSIAN to "ویساک، روز ماه کامل",
                        ),
                    ),
                rule = EventRule.Astronomical(AstroKind.FULL_MOON, offsetDays = 0, timeZone = "UTC", month = 5),
                validity =
                    Validity(
                        calendar = CalendarSystem.GREGORIAN,
                        fromYear = 1999,
                        toYear = null,
                        citation =
                            Citation(
                                url = "https://undocs.org/en/A/RES/54/115",
                                title =
                                    "General Assembly resolution 54/115: International recognition of the Day of " +
                                        "Vesak at United Nations Headquarters and other United Nations offices (1999)",
                            ),
                    ),
                citations =
                    listOf(
                        Citation(
                            url = "https://www.un.org/en/observances/vesak-day",
                            title = "United Nations — Vesak Day (observance page)",
                        ),
                    ),
                links = mapOf("un" to "https://www.un.org/en/observances/vesak-day"),
            )
    }
}
