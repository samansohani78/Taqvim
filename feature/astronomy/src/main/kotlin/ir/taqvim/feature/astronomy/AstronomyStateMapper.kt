/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.RiseSetTransit
import ir.taqvim.core.astronomy.Sky as SkyEngine
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.component.DateSelection
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toLocalDateTime

/** Turns settings and the selected instant into [AstronomyContent] (pure apart from the header cache; T-1300). */
internal object AstronomyStateMapper {
    private const val MINUTES_PER_HOUR = 60
    private const val PERCENT = 100.0
    private const val PHASES = 8
    private const val PHASE_WIDTH = 45.0
    private const val PICKER_YEARS = 150

    fun content(
        settings: AstronomySettings?,
        instant: Instant,
        isNow: Boolean,
        cache: AstronomyHeaderCache,
    ): AstronomyContent {
        if (settings == null) return AstronomyContent.NoLocation
        val text = AstronomyText(settings)
        val local = instant.toLocalDateTime(settings.timeZone)
        val day = instant.toJdn(settings.timeZone)
        val header = cache.header(instant, settings.place)
        val sunDay = SkyEngine.riseSetTransit(CelestialBody.SUN, settings.place, settings.startOf(day))
        return AstronomyContent.Sky(
            placeName = settings.placeName,
            dateTitle = text.date(instant),
            timeText = text.time(instant),
            minuteOfDay = local.hour * MINUTES_PER_HOUR + local.minute,
            isNow = isNow,
            header = header(header, text),
            earth = earth(header, sunDay, instant, text),
            moon = moon(header, settings, instant, day, text),
            sun = sun(settings, instant, sunDay, text),
            picker = picker(settings, day),
        )
    }

    /** The named phase of a Moon [elongation] east of the Sun, each name centered on its 45° sector. */
    fun phaseName(elongation: Double): MoonPhaseName {
        val shifted = (elongation + PHASE_WIDTH / 2).toLong()
        val sector = Math.floorMod(Math.floorDiv(shifted, PHASE_WIDTH.toLong()), PHASES.toLong())
        return MoonPhaseName.entries[sector.toInt()]
    }

    private fun header(
        header: AstronomyHeader,
        text: AstronomyText,
    ) = HeaderText(
        sunSign = header.sunSign,
        moonSign = header.moonSign,
        moonConstellation = header.moonConstellation,
        phase = phaseName(header.moonPhaseDegrees),
        illumination = text.decimal(header.moonIlluminatedFraction * PERCENT, 0),
        moonDistance = text.integer(Math.round(header.moonDistanceKm)),
        nextSeason = header.nextSeason,
        nextSeasonAt = text.dateTime(header.nextSeasonAt),
        solarEclipse = header.nextSolarEclipse?.let { EclipseText(it.kind, text.dateTime(it.peak)) },
        lunarEclipse = header.nextLunarEclipse?.let { EclipseText(it.kind, text.dateTime(it.peak)) },
    )

    private fun earth(
        header: AstronomyHeader,
        sunDay: RiseSetTransit,
        instant: Instant,
        text: AstronomyText,
    ): EarthText {
        val rise = sunDay.rise
        val set = sunDay.set
        val daylight = if (rise != null && set != null && set > rise) set - rise else null
        return EarthText(
            dayLength = daylight?.let(text::duration),
            subsolarLatitude =
                text.degrees(SkyEngine.skyPosition(CelestialBody.SUN, instant, ORIGIN).declinationDegrees),
            nextSeason = header.nextSeason,
            nextSeasonAt = text.dateTime(header.nextSeasonAt),
        )
    }

    private fun moon(
        header: AstronomyHeader,
        settings: AstronomySettings,
        instant: Instant,
        day: Jdn,
        text: AstronomyText,
    ): MoonText {
        val position = SkyEngine.skyPosition(CelestialBody.MOON, instant, settings.place)
        val events = SkyEngine.riseSetTransit(CelestialBody.MOON, settings.place, settings.startOf(day))
        return MoonText(
            illuminatedFraction = header.moonIlluminatedFraction.toFloat(),
            waxing = header.moonWaxing,
            phase = phaseName(header.moonPhaseDegrees),
            illumination = text.decimal(header.moonIlluminatedFraction * PERCENT, 0),
            azimuth = text.degrees(position.azimuthDegrees),
            altitude = text.degrees(position.altitudeDegrees),
            rise = events.rise?.let(text::time),
            set = events.set?.let(text::time),
            distance = text.integer(Math.round(header.moonDistanceKm)),
        )
    }

    private fun sun(
        settings: AstronomySettings,
        instant: Instant,
        sunDay: RiseSetTransit,
        text: AstronomyText,
    ): SunText {
        val position = SkyEngine.skyPosition(CelestialBody.SUN, instant, settings.place)
        val rise = sunDay.rise
        val set = sunDay.set
        val progress = if (rise != null && set != null) dayProgress(instant, rise, set) else null
        return SunText(
            azimuth = text.degrees(position.azimuthDegrees),
            altitude = text.degrees(position.altitudeDegrees),
            rise = rise?.let(text::time),
            transit = sunDay.transit?.let(text::time),
            set = set?.let(text::time),
            progress = progress,
        )
    }

    /** Elapsed part of the daylight from [rise] to [set] at [instant], or `null` outside it. */
    private fun dayProgress(
        instant: Instant,
        rise: Instant,
        set: Instant,
    ): Float? = if (set > rise && instant in rise..set) ((instant - rise) / (set - rise)).toFloat() else null

    private fun picker(
        settings: AstronomySettings,
        day: Jdn,
    ): PickerData {
        val calendar = settings.calendar
        val date = calendar.fromJdn(day)
        val names =
            settings.language.monthNames.forSystem(calendar.system)
                ?: requireNotNull(LanguageTable.forCode(FALLBACK_LANGUAGE))
                    .monthNames
                    .forSystem(calendar.system)
                    .orEmpty()
        val text = AstronomyText(settings)
        return PickerData(
            initial = DateSelection(date.year, date.month, date.day),
            years = (date.year - PICKER_YEARS)..(date.year + PICKER_YEARS),
            monthNames = names.toImmutableList(),
            daysInMonth = calendar::monthLength,
            digits = text::plain,
        )
    }

    private const val FALLBACK_LANGUAGE = "en"
    private val ORIGIN = Coordinates(0.0, 0.0)
}
