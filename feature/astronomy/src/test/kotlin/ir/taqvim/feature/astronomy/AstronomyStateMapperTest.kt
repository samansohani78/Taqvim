/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldMatch
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.IauConstellationNames
import org.junit.jupiter.api.Test

/** T-1300: the Earth, Moon and Sun views and the header, mapped and localized. */
class AstronomyStateMapperTest {
    private val tehran = AstronomyFixtures.tehran()

    @Test
    fun `no place gives the missing-place state`() {
        val noon = AstronomyFixtures.at("2026-06-21T12:00", tehran)
        AstronomyStateMapper.content(null, noon, true, AstronomyHeaderCache()) shouldBe AstronomyContent.NoLocation
    }

    @Test
    fun `a summer noon in Tehran`() {
        val noon = AstronomyFixtures.at("2026-06-21T13:05", tehran)
        val sky = AstronomyFixtures.sky(tehran, noon, isNow = false)

        sky.placeName shouldBe "Tehran"
        sky.timeText shouldBe "13:05"
        sky.minuteOfDay shouldBe 13 * 60 + 5
        sky.isNow shouldBe false
        sky.earth.dayLength.shouldNotBeNull() shouldContain "14 hours"
        sky.earth.subsolarLatitude shouldBe "23.4°"
        // The 2026 June solstice (08:24 UTC) precedes 13:05 Tehran time (08:35 UTC) on the same day.
        sky.earth.nextSeason shouldBe Season.SEPTEMBER_EQUINOX
        AstronomyFixtures.sky(tehran, AstronomyFixtures.at("2026-06-21T11:00", tehran)).earth.nextSeason shouldBe
            Season.JUNE_SOLSTICE
        sky.sun.rise.shouldNotBeNull()
        sky.sun.set.shouldNotBeNull()
        (sky.sun.progress.shouldNotBeNull() in 0.45f..0.6f) shouldBe true
        sky.sun.altitude.shouldEndWith("°")
        sky.header.sunSign.name shouldBe "CANCER"
        sky.picker.initial.year shouldBe 1405
        sky.picker.initial.month shouldBe 3
        sky.picker.initial.day shouldBe 31
        sky.picker.monthNames.size shouldBe 12
        sky.picker.daysInMonth(1405, 12) shouldBe PersianCalendarSystem.monthLength(1405, 12)
    }

    @Test
    fun `the Moon's constellation is named, not abbreviated`() {
        // The astronomy library reports only the IAU abbreviation ("Sco"); the screen showed that verbatim until the
        // IAU's own names were bundled (DT-026). No per-language name is sourced yet, so every language sees the
        // Latin one.
        val sky = AstronomyFixtures.sky(tehran, AstronomyFixtures.at("2026-08-28T22:00", tehran))

        sky.header.moonConstellation shouldBe IauConstellationNames.name("Aqr")
        sky.header.moonConstellation shouldBe "Aquarius"
    }

    @Test
    fun `the full moon of 2026-08-28 and the Sun at night`() {
        val night = AstronomyFixtures.at("2026-08-28T08:00", tehran)
        val sky = AstronomyFixtures.sky(tehran, night)

        sky.moon.phase shouldBe MoonPhaseName.FULL_MOON
        (sky.moon.illuminatedFraction > 0.98f) shouldBe true
        sky.moon.illumination shouldBe "100"
        sky.header.phase shouldBe MoonPhaseName.FULL_MOON
        val midnight = AstronomyFixtures.sky(tehran, AstronomyFixtures.at("2026-08-28T01:00", tehran))
        midnight.sun.progress.shouldBeNull()
    }

    @Test
    fun `Persian digits and a polar day`() {
        val persian = AstronomyFixtures.tehran("fa")
        val sky = AstronomyFixtures.sky(persian, AstronomyFixtures.at("2026-06-21T13:05", persian))
        sky.timeText shouldBe "۱۳:۰۵"

        val tromso = AstronomyFixtures.tromso()
        val polar = AstronomyFixtures.sky(tromso, AstronomyFixtures.at("2026-06-21T12:00", tromso))
        polar.earth.dayLength.shouldBeNull()
        polar.sun.progress.shouldBeNull()
    }

    @Test
    fun `the Sun panel shows the day's golden and blue hours (F-10)`() {
        val sky = AstronomyFixtures.sky(tehran, AstronomyFixtures.at("2026-06-21T13:05", tehran))

        // Tehran in midsummer: the Sun crosses the -4..6 band once on the way up and once on the way down.
        sky.sun.goldenHours.map { it.part } shouldBe listOf(DayPart.MORNING, DayPart.EVENING)
        sky.sun.goldenHours.forEach { it.range shouldMatch Regex("""\d{2}:\d{2}–\d{2}:\d{2}""") }
        // Zero-padded 24-hour text sorts the way the times do, so this states that the two bands bracket solar noon
        // — which is what the mapper's morning/evening split is for.
        val transit = sky.sun.transit.shouldNotBeNull()
        (
            sky.sun.goldenHours
                .first()
                .range < transit
        ) shouldBe true
        (
            sky.sun.goldenHours
                .last()
                .range > transit
        ) shouldBe true

        // Blue hour is the band below golden hour, so the morning blue window ends where the morning golden one
        // begins and the evening pair meet the other way round. That ordering is the thing worth pinning: it is what
        // would break if the two bands were ever mapped from the wrong edges.
        sky.sun.blueHours.map { it.part } shouldBe listOf(DayPart.MORNING, DayPart.EVENING)
        val morningBlue =
            sky.sun.blueHours
                .first()
                .range
        val morningGold =
            sky.sun.goldenHours
                .first()
                .range
        (morningBlue < morningGold) shouldBe true
        morningBlue.substringAfter(EN_DASH) shouldBe morningGold.substringBefore(EN_DASH)
        val eveningBlue =
            sky.sun.blueHours
                .last()
                .range
        val eveningGold =
            sky.sun.goldenHours
                .last()
                .range
        (eveningBlue > eveningGold) shouldBe true
        eveningGold.substringAfter(EN_DASH) shouldBe eveningBlue.substringBefore(EN_DASH)

        // The Persian panel writes the same window in Persian digits.
        val persian = AstronomyFixtures.tehran("fa")
        AstronomyFixtures
            .sky(persian, AstronomyFixtures.at("2026-06-21T13:05", persian))
            .sun.goldenHours
            .first()
            .range shouldMatch Regex("""[۰-۹]{2}:[۰-۹]{2}–[۰-۹]{2}:[۰-۹]{2}""")
    }

    private companion object {
        /** The dash `AstronomyText.range` joins a window's two times with. */
        const val EN_DASH = "–"
    }

    @Test
    fun `a Sun that never sets still has golden light around midnight`() {
        // Tromso on the June solstice is a polar day, but the Sun still dips to about 3 degrees, inside the band.
        // The row is therefore not empty here; what empties it is a Sun that stays above 6 degrees or below -4 all
        // day, which the screen renders as "none" rather than omitting the row.
        val tromso = AstronomyFixtures.tromso()
        val polar = AstronomyFixtures.sky(tromso, AstronomyFixtures.at("2026-06-21T12:00", tromso))

        polar.sun.progress.shouldBeNull()
        polar.sun.goldenHours.shouldNotBeEmpty()
        polar.sun.goldenHours.forEach { it.range shouldMatch Regex("""\d{2}:\d{2}–\d{2}:\d{2}""") }
        // The Sun stays well above civil twilight all day, so there is no blue hour at all — the case the screen
        // renders as a single "none" row rather than dropping silently.
        polar.sun.blueHours.shouldBeEmpty()
    }

    @Test
    fun `phase names are centered on their sectors`() {
        mapOf(
            0.0 to MoonPhaseName.NEW_MOON,
            22.4 to MoonPhaseName.NEW_MOON,
            22.6 to MoonPhaseName.WAXING_CRESCENT,
            90.0 to MoonPhaseName.FIRST_QUARTER,
            135.0 to MoonPhaseName.WAXING_GIBBOUS,
            180.0 to MoonPhaseName.FULL_MOON,
            225.0 to MoonPhaseName.WANING_GIBBOUS,
            270.0 to MoonPhaseName.THIRD_QUARTER,
            315.0 to MoonPhaseName.WANING_CRESCENT,
            359.0 to MoonPhaseName.NEW_MOON,
        ).forEach { (elongation, name) -> AstronomyStateMapper.phaseName(elongation) shouldBe name }
    }

    @Test
    fun `sign positions and localized numbers`() {
        val text = AstronomyText(tehran)
        text.signPosition(215.5) shouldBe SignPosition(ZodiacSign.SCORPIO, "5°30′")
        text.signPosition(-0.25) shouldBe SignPosition(ZodiacSign.PISCES, "29°45′")
        text.integer(384_400) shouldBe "384,400"
        text.decimal(12.345, 1) shouldBe "12.3"
        AstronomyText(AstronomyFixtures.tehran("fa")).plain(12) shouldBe "۱۲"
    }
}
