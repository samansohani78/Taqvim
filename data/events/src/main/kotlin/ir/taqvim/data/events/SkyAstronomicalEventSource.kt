/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.astronomy.MoonQuarter
import ir.taqvim.core.astronomy.Season
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.events.AstroKind
import ir.taqvim.core.events.AstronomicalEventSource
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The default [AstronomicalEventSource] (ADR-0044): equinoxes, solstices and Moon phases from `:core:astronomy`'s
 * [Sky] façade over cosinekitty/astronomy 2.1.19, the same Meeus-based ephemeris every other astronomical calendar
 * rule in this app already uses (`UsnoMoonPhasesTest` checks it against USNO to within two minutes over 1700–2100).
 * Every instant is geocentric — a solar-longitude-versus-lunar-longitude event, not a rise/set/visibility one — so
 * [instants] never consults an observer's location; `OccurrenceCalculator` alone turns an instant into a civil day,
 * in the rule's own `timeZone`.
 */
public object SkyAstronomicalEventSource : AstronomicalEventSource {
    override fun instants(
        kind: AstroKind,
        from: Instant,
        until: Instant,
    ): List<Instant> =
        when (kind) {
            AstroKind.MARCH_EQUINOX -> seasonInstants(Season.MARCH_EQUINOX, from, until)
            AstroKind.JUNE_SOLSTICE -> seasonInstants(Season.JUNE_SOLSTICE, from, until)
            AstroKind.SEPTEMBER_EQUINOX -> seasonInstants(Season.SEPTEMBER_EQUINOX, from, until)
            AstroKind.DECEMBER_SOLSTICE -> seasonInstants(Season.DECEMBER_SOLSTICE, from, until)
            AstroKind.NEW_MOON -> moonInstants(MoonQuarter.NEW_MOON, from, until)
            AstroKind.FULL_MOON -> moonInstants(MoonQuarter.FULL_MOON, from, until)
        }

    private fun moonInstants(
        quarter: MoonQuarter,
        from: Instant,
        until: Instant,
    ): List<Instant> = Sky.moonQuarters(from, until).filter { it.quarter == quarter }.map { it.instant }

    /** [season] of every Gregorian (UTC) year touching [from, until); [Sky.seasons] takes one whole year at a time. */
    private fun seasonInstants(
        season: Season,
        from: Instant,
        until: Instant,
    ): List<Instant> {
        val firstYear = from.toLocalDateTime(TimeZone.UTC).year - 1
        val lastYear = until.toLocalDateTime(TimeZone.UTC).year + 1
        return (firstYear..lastYear)
            .map { Sky.seasons(it).of(season) }
            .filter { it >= from && it < until }
    }
}
